package com.doublefmediafab

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Features(val frames: List<Long>, val labels: Set<String> = emptySet()) {
    val usable: Boolean get() = frames.isNotEmpty()
}

class FeatureCache(context: Context) : SQLiteOpenHelper(context, "features.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE features(uri TEXT PRIMARY KEY, bytes INTEGER, modified INTEGER, hashes TEXT, labels TEXT)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS features")
        onCreate(db)
    }
    fun get(item: MediaFile): Features? {
        readableDatabase.rawQuery(
            "SELECT hashes,labels FROM features WHERE uri=? AND bytes=? AND modified=?",
            arrayOf(item.uri.toString(), item.size.toString(), item.modified.toString())
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return Features(
                cursor.getString(0).split(",").filter { it.isNotBlank() }
                    .map { java.lang.Long.parseUnsignedLong(it, 16) },
                cursor.getString(1)?.split("|")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
            )
        }
    }
    fun put(item: MediaFile, feature: Features) {
        val values = ContentValues().apply {
            put("uri", item.uri.toString())
            put("bytes", item.size)
            put("modified", item.modified)
            put("hashes", feature.frames.joinToString(",") { java.lang.Long.toUnsignedString(it, 16) })
            put("labels", feature.labels.joinToString("|"))
        }
        writableDatabase.insertWithOnConflict("features", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
}
