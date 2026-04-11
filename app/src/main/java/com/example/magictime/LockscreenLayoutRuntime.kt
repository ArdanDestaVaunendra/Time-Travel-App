package com.example.magictime

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.magictime.databinding.ActivityFakeLockBinding
import kotlin.text.toInt

object LockscreenLayoutRuntime {

    fun apply(store: LayoutConfigStore, binding: ActivityFakeLockBinding) {
        val slot = store.getActiveSlot()
        if (slot !in 1..3) return
        val c = store.load(slot) ?: return

        applyPosAndScaleCenteredY(binding.tvBigClock, c.clock)
        applyPosAndScaleCenteredY(binding.tvDate, c.date)
        applyPosAndScaleTopStart(binding.tvTicker, c.operator)
        applyPosAndScaleMarquee(binding.tvMarqueeBottom, c.marquee)
        applyPosAndScaleStatusBarYOnly(binding.statusBarContainer, c.statusBar)

        c.lockIcon?.let { pos ->
            applyPosAndScaleTopStart(binding.ivLock, pos)
        }

        c.phoneButton?.let { pos ->
            applyPosAndScaleTopStart(binding.bgPhone, pos)
            binding.ivPhone.scaleX = pos.scale
            binding.ivPhone.scaleY = pos.scale
        }

        c.cameraButton?.let { pos ->
            applyPosAndScaleTopStart(binding.bgCamera, pos)
            binding.ivCamera.scaleX = pos.scale
            binding.ivCamera.scaleY = pos.scale
        }
    }

    private fun applyPosAndScaleStatusBarYOnly(view: View, pos: Pos) {
        val lp = view.layoutParams as? ConstraintLayout.LayoutParams ?: return

        lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID

        lp.startToStart = ConstraintLayout.LayoutParams.UNSET
        lp.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
        lp.leftToLeft = ConstraintLayout.LayoutParams.UNSET
        lp.rightToRight = ConstraintLayout.LayoutParams.UNSET
        lp.topToBottom = ConstraintLayout.LayoutParams.UNSET
        lp.bottomToTop = ConstraintLayout.LayoutParams.UNSET
        lp.startToEnd = ConstraintLayout.LayoutParams.UNSET
        lp.endToStart = ConstraintLayout.LayoutParams.UNSET

        lp.topMargin = pos.y.toInt()

        view.layoutParams = lp
        view.translationX = 0f
        view.translationY = 0f
        view.scaleX = pos.scale
        view.scaleY = pos.scale
    }

    private fun applyPosAndScaleMarquee(view: View, pos: Pos) {
        val lp = view.layoutParams as? ConstraintLayout.LayoutParams ?: return
        lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        lp.bottomToBottom = ConstraintLayout.LayoutParams.UNSET

        lp.width = 0
        lp.marginStart = 24
        lp.marginEnd = 24
        lp.topMargin = pos.y.toInt()

        view.layoutParams = lp
        view.translationX = 0f
        view.translationY = 0f
        view.scaleX = pos.scale
        view.scaleY = pos.scale
    }

    private fun applyPosAndScaleTopStart(view: View, pos: Pos) {
        val lp = view.layoutParams as? ConstraintLayout.LayoutParams ?: return
        lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        lp.endToEnd = ConstraintLayout.LayoutParams.UNSET
        lp.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
        lp.leftToLeft = ConstraintLayout.LayoutParams.UNSET
        lp.rightToRight = ConstraintLayout.LayoutParams.UNSET
        lp.topToBottom = ConstraintLayout.LayoutParams.UNSET
        lp.bottomToTop = ConstraintLayout.LayoutParams.UNSET
        lp.startToEnd = ConstraintLayout.LayoutParams.UNSET
        lp.endToStart = ConstraintLayout.LayoutParams.UNSET

        lp.marginStart = pos.x.toInt()
        lp.topMargin = pos.y.toInt()

        view.layoutParams = lp
        view.translationX = 0f
        view.translationY = 0f
        view.scaleX = pos.scale
        view.scaleY = pos.scale
    }

    private fun applyPosAndScaleCenteredY(view: View, pos: Pos) {
        val lp = view.layoutParams as? ConstraintLayout.LayoutParams ?: return

        lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID

        lp.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
        lp.leftToLeft = ConstraintLayout.LayoutParams.UNSET
        lp.rightToRight = ConstraintLayout.LayoutParams.UNSET
        lp.topToBottom = ConstraintLayout.LayoutParams.UNSET
        lp.bottomToTop = ConstraintLayout.LayoutParams.UNSET
        lp.startToEnd = ConstraintLayout.LayoutParams.UNSET
        lp.endToStart = ConstraintLayout.LayoutParams.UNSET

        lp.marginStart = 0
        lp.marginEnd = 0
        lp.topMargin = pos.y.toInt()

        view.layoutParams = lp
        view.translationX = 0f
        view.translationY = 0f
        view.scaleX = pos.scale
        view.scaleY = pos.scale
    }
}
