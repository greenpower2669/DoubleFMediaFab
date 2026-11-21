package com.doublefmediafab

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.abs
import kotlin.math.min

/**
 * Lecteur photo : ajustement initial, zoom pincement/boutons et déplacement.
 * En taille normale, glissement horizontal pour la photo précédente/suivante.
 * Le double-tap est réservé à l'affichage de la barre d'outils.
 */
class ZoomImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {
    var onDoubleTapTools: (() -> Unit)? = null
    var onSlide: ((Int) -> Unit)? = null
    private val imageMatrixValue = Matrix()
    private var fitScale = 1f
    private var zoom = 1f
    private var startX = 0f
    private var startY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var multiTouch = false
    private var panned = false

    private val pinchDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleBy(detector.scaleFactor, detector.focusX, detector.focusY)
                return true
            }
        })

    private val tapDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true
            override fun onDoubleTap(e: MotionEvent): Boolean {
                onDoubleTapTools?.invoke()
                return true
            }
        })

    init {
        scaleType = ScaleType.MATRIX
        setBackgroundColor(android.graphics.Color.BLACK)
        isClickable = true
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        post { resetToFit() }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        post { resetToFit() }
    }

    fun zoomIn() = scaleBy(1.4f, width / 2f, height / 2f)
    fun zoomOut() = scaleBy(1f / 1.4f, width / 2f, height / 2f)

    fun resetToFit() {
        val d = drawable ?: return
        if (width <= 0 || height <= 0 || d.intrinsicWidth <= 0 || d.intrinsicHeight <= 0) return
        fitScale = min(width.toFloat() / d.intrinsicWidth, height.toFloat() / d.intrinsicHeight)
        zoom = 1f
        imageMatrixValue.reset()
        imageMatrixValue.postScale(fitScale, fitScale)
        imageMatrixValue.postTranslate(
            (width - d.intrinsicWidth * fitScale) / 2f,
            (height - d.intrinsicHeight * fitScale) / 2f
        )
        imageMatrix = imageMatrixValue
    }

    private fun scaleBy(factor: Float, pivotX: Float, pivotY: Float) {
        if (drawable == null || width == 0) return
        val next = (zoom * factor).coerceIn(1f, 5f)
        val actual = next / zoom
        if (abs(actual - 1f) < 0.0001f) return
        imageMatrixValue.postScale(actual, actual, pivotX, pivotY)
        zoom = next
        constrainImage()
        imageMatrix = imageMatrixValue
    }

    private fun constrainImage() {
        val d = drawable ?: return
        val bounds = RectF(0f, 0f, d.intrinsicWidth.toFloat(), d.intrinsicHeight.toFloat())
        imageMatrixValue.mapRect(bounds)
        val dx = if (bounds.width() <= width) {
            width / 2f - bounds.centerX()
        } else when {
            bounds.left > 0f -> -bounds.left
            bounds.right < width -> width - bounds.right
            else -> 0f
        }
        val dy = if (bounds.height() <= height) {
            height / 2f - bounds.centerY()
        } else when {
            bounds.top > 0f -> -bounds.top
            bounds.bottom < height -> height - bounds.bottom
            else -> 0f
        }
        imageMatrixValue.postTranslate(dx, dy)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        pinchDetector.onTouchEvent(event)
        tapDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x; startY = event.y
                lastX = event.x; lastY = event.y
                multiTouch = false; panned = false
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_POINTER_DOWN -> multiTouch = true
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount > 1) {
                    multiTouch = true
                } else if (!pinchDetector.isInProgress && zoom > 1.01f) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    if (abs(dx) + abs(dy) > 0.5f) {
                        imageMatrixValue.postTranslate(dx, dy)
                        constrainImage()
                        imageMatrix = imageMatrixValue
                        panned = true
                    }
                }
                lastX = event.x; lastY = event.y
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - startX
                val dy = event.y - startY
                val threshold = 65f * resources.displayMetrics.density
                if (!multiTouch && !panned && zoom <= 1.01f &&
                    abs(dx) > threshold && abs(dx) > abs(dy) * 1.3f) {
                    onSlide?.invoke(if (dx < 0) 1 else -1)
                }
                performClick()
            }
            MotionEvent.ACTION_CANCEL -> multiTouch = false
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
