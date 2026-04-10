package com.example.magictime

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import kotlin.math.abs

enum class SnapArea { TOP, CENTER }

class LockDragController(
    private val canvas: ViewGroup,
    private val snapThresholdPx: Float = 15f,
    private val onSelected: (View) -> Unit,
    private val onMoved: ((View) -> Unit)? = null,
    private val onSnapGuide: (showVertical: Boolean, showHorizontal: Boolean) -> Unit
) {
    fun attach(target: View, area: SnapArea) {
        target.setOnTouchListener(object : View.OnTouchListener {
            var lastRawX = 0f
            var lastRawY = 0f

            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        onSelected(v)
                        lastRawX = e.rawX
                        lastRawY = e.rawY
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = e.rawX - lastRawX
                        val dy = e.rawY - lastRawY
                        lastRawX = e.rawX
                        lastRawY = e.rawY

                        v.x = (v.x + dx).coerceIn(0f, (canvas.width - v.width).toFloat())
                        v.y = (v.y + dy).coerceIn(0f, (canvas.height - v.height).toFloat())

                        applySnap(v, area)
                        onMoved?.invoke(v)
                        return true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        onSnapGuide(false, false)
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun applySnap(v: View, area: SnapArea) {
        val centerX = canvas.width / 2f - v.width / 2f
        val centerY = canvas.height / 2f - v.height / 2f

        var snapV = false
        var snapH = false

        if (abs(v.x - centerX) < snapThresholdPx) {
            v.x = centerX
            snapV = true
        }

        val horizontalTarget = when (area) {
            SnapArea.TOP -> (canvas.height * 0.08f).coerceAtLeast(0f)
            SnapArea.CENTER -> centerY
        }

        if (abs(v.y - horizontalTarget) < snapThresholdPx) {
            v.y = horizontalTarget
            snapH = true
        }

        onSnapGuide(snapV, snapH)
    }
}