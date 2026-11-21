package com.doublefmediafab

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Size
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.security.MessageDigest
import kotlin.math.min

/**
 * On-device duplicate search: exact digest by matching byte size, visual dHash
 * across 1 photo or 3 video frames, optional ML Kit labels for ambiguous pairs.
 * Memory: one small feature per media, no full-size decoded frames retained.
 */
class SimilarityEngine(private val context: Context) {
    private val cache = FeatureCache(context)

    fun analyze(files: List<MediaFile>, progress: (String) -> Unit): List<MatchGroup> {
        val began = System.currentTimeMillis()
        val count = files.size
        val parent = IntArray(count) { it }
        val digests = HashMap<Int, String>()
        val visualFeatures = arrayOfNulls<Features>(count)
        var errors = 0
        var mlRuns = 0
        fun root(x: Int): Int {
            var p = x
            while (p != parent[p]) { parent[p] = parent[parent[p]]; p = parent[p] }
            return p
        }
        fun join(a: Int, b: Int) {
            val ra = root(a); val rb = root(b)
            if (ra != rb) parent[rb] = ra
        }

        progress("Étape 1/3 : copies identiques")
        AppLog.write(context, "ANALYSE_START", "count=$count")
        // Only hash files with the same byte length (necessary for byte-identical files).
        for (ids in files.indices.groupBy { files[it].size }.values.filter { it.size > 1 }) {
            val same = HashMap<String, Int>()
            for (i in ids) {
                try {
                    val digest = exactDigest(files[i])
                    digests[i] = digest
                    same[digest]?.let { join(i, it) } ?: run { same[digest] = i }
                } catch (e: Exception) {
                    errors++
                    AppLog.write(context, "HASH_ERROR", "${files[i].uri}: ${e.javaClass.simpleName}: ${e.message}")
                }
            }
        }
        progress("Étape 2/3 : signatures visuelles")
        var cacheHits = 0
        for (i in files.indices) {
            try {
                val stored = cache.get(files[i])
                if (stored != null) cacheHits++
                val feature = stored ?: Features(extractFrames(files[i]))
                visualFeatures[i] = feature
                if (stored == null) cache.put(files[i], feature)
            } catch (e: Exception) {
                errors++
                AppLog.write(context, "FEATURE_ERROR", "${files[i].uri}: ${e.javaClass.simpleName}: ${e.message}")
            }
            if (i % 30 == 0 || i == count - 1) progress("Signatures : ${i + 1}/$count (cache $cacheHits)")
        }

        progress("Étape 3/3 : comparaison et IA locale")
        val labelsModel = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        val bucketIndex = HashMap<Long, MutableList<Int>>()
        val maxVisualPairs = 150000
        var compared = 0
        var candidates = 0
        var logLargeBuckets = 0
        fun labelled(index: Int): Set<String> {
            val old = visualFeatures[index] ?: return emptySet()
            if (old.labels.isNotEmpty()) return old.labels
            if (mlRuns >= 200) return emptySet()
            mlRuns++
            return try {
                val bmp = representativeBitmap(files[index]) ?: return emptySet()
                val result = Tasks.await(labelsModel.process(InputImage.fromBitmap(bmp, 0)))
                bmp.recycle()
                val labels = result.filter { it.confidence >= 0.55f }
                    .map { it.text.lowercase() }.toSet()
                visualFeatures[index] = old.copy(labels = labels)
                cache.put(files[index], visualFeatures[index]!!)
                labels
            } catch (e: Exception) {
                errors++
                AppLog.write(context, "ML_ERROR", "${files[index].uri}: ${e.javaClass.simpleName}: ${e.message}")
                emptySet()
            }
        }

        try {
            for (i in files.indices) {
                val f = visualFeatures[i] ?: continue
                if (!f.usable) continue
                val keyHash = f.frames[f.frames.size / 2]
                val seen = HashSet<Int>()
                // 8 locality-sensitive buckets; final distance comparison is mandatory.
                for (band in 0 until 8) {
                    val part = (keyHash ushr (band * 8)) and 255L
                    val key = (files[i].kind.toLong() shl 40) or
                        (band.toLong() shl 32) or part
                    val bucket = bucketIndex.getOrPut(key) { ArrayList() }
                    if (bucket.size <= 1500) {
                        for (j in bucket) {
                            if (!seen.add(j)) continue
                            val other = visualFeatures[j] ?: continue
                            if (!other.usable || other.frames.size != f.frames.size) continue
                            // Different durations are okay for re-encoded videos; frame order matters.
                            compared++
                            if (compared > maxVisualPairs) break
                            val dist = f.frames.zip(other.frames).sumOf { (a, b) ->
                                java.lang.Long.bitCount(a xor b)
                            }.toDouble() / f.frames.size
                            val threshold = if (files[i].isVideo) 12.0 else 11.0
                            if (dist <= threshold) {
                                candidates++
                                var accepted = true
                                if (dist >= 5.0 && mlRuns < 200) {
                                    val left = labelled(j)
                                    val right = labelled(i)
                                    // AI semantic disagreement can mark uncertain matches,
                                    // but cannot override a very close structural signature.
                                    if (left.isNotEmpty() && right.isNotEmpty()) {
                                        val intersection = left.intersect(right).size
                                        val union = left.union(right).size
                                        val jaccard = if (union == 0) 0.0 else intersection.toDouble() / union
                                        if (dist > 8.0 && jaccard < 0.16) accepted = false
                                    }
                                }
                                if (accepted) join(i, j)
                            }
                        }
                    } else if (logLargeBuckets < 4) {
                        logLargeBuckets++
                        AppLog.write(context, "LARGE_BUCKET", "band=$band size=${bucket.size}; bounded comparison")
                    }
                    bucket.add(i)
                    if (compared > maxVisualPairs) break
                }
                if (i % 75 == 0 || i == count - 1) progress("Comparaison : ${i + 1}/$count · IA $mlRuns")
                if (compared > maxVisualPairs) {
                    AppLog.write(context, "PAIR_LIMIT", "max=$maxVisualPairs")
                    break
                }
            }
        } finally {
            labelsModel.close()
            cache.close()
        }

        val groups = files.indices.groupBy { root(it) }.values
            .filter { it.size > 1 }.map { ids ->
                val exact = ids.all { digests[it] != null } &&
                    ids.map { digests[it] }.distinct().size == 1
                val distances = ids.drop(1).mapNotNull { i ->
                    val a = visualFeatures[ids.first()]?.frames ?: return@mapNotNull null
                    val b = visualFeatures[i]?.frames ?: return@mapNotNull null
                    if (a.size != b.size || a.isEmpty()) return@mapNotNull null
                    a.zip(b).sumOf { (x, y) -> java.lang.Long.bitCount(x xor y) }
                        .toDouble() / a.size
                }
                val score = if (exact) 100 else
                    (100.0 - (distances.maxOrNull() ?: 12.0) * 100.0 / 64.0)
                        .toInt().coerceIn(0, 99)
                MatchGroup(ids.map { files[it] }, if (exact) "Copies exactes" else "Ressemblance visuelle", score)
            }.sortedWith(compareBy<MatchGroup> { it.label != "Copies exactes" }
                .thenByDescending { it.savedBytes })

        AppLog.write(context, "ANALYSE_DONE",
            "media=$count groups=${groups.size} candidates=$candidates pairs=$compared ml=$mlRuns cache=$cacheHits errors=$errors elapsedMs=${System.currentTimeMillis() - began}")
        progress("${groups.size} groupes trouvés · $errors anomalies journalisées")
        return groups
    }

    private fun exactDigest(item: MediaFile): String {
        val digest = MessageDigest.getInstance("SHA-256")
        context.contentResolver.openInputStream(item.uri).use { input ->
            requireNotNull(input) { "Flux inaccessible" }
            val buffer = ByteArray(65536)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                if (n > 0) digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun representativeBitmap(item: MediaFile): Bitmap? =
        if (!item.isVideo) {
            context.contentResolver.loadThumbnail(item.uri, Size(96, 96), null)
        } else {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, item.uri)
                val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                retriever.getScaledFrameAtTime((dur * 500L), MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 96, 96)
                    ?: retriever.getFrameAtTime()
            }
        }

    private fun extractFrames(item: MediaFile): List<Long> {
        if (!item.isVideo) {
            val bmp = representativeBitmap(item) ?: return emptyList()
            return try { listOf(dHash(bmp)) } finally { bmp.recycle() }
        }
        return MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, item.uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val result = ArrayList<Long>()
            for (fraction in listOf(0.15, 0.5, 0.85)) {
                val frame = retriever.getScaledFrameAtTime(
                    (durationMs * fraction * 1000).toLong(),
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 96, 96
                ) ?: continue
                try { result.add(dHash(frame)) } finally { frame.recycle() }
            }
            result
        }
    }

    private fun dHash(source: Bitmap): Long {
        val scaled = Bitmap.createScaledBitmap(source, 9, 8, true)
        var bits = 0L
        try {
            for (y in 0 until 8) for (x in 0 until 8) {
                val left = scaled.getPixel(x, y)
                val right = scaled.getPixel(x + 1, y)
                fun luma(c: Int) = ((c shr 16) and 255) * 299 +
                    ((c shr 8) and 255) * 587 + (c and 255) * 114
                bits = (bits shl 1) or if (luma(left) > luma(right)) 1L else 0L
            }
        } finally {
            if (scaled !== source) scaled.recycle()
        }
        return bits
    }
}
