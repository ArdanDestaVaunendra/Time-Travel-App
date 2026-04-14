package com.example.magictime

import android.graphics.Color
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
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

        applySavedStyle(c, binding)
    }

    private fun applySavedStyle(c: LayoutConfig, binding: ActivityFakeLockBinding) {
        val ctx = binding.root.context

        val cameraRes = if (c.cameraIconRes != 0) c.cameraIconRes else R.drawable.ic_camera
        val phoneRes = if (c.phoneIconRes != 0) c.phoneIconRes else R.drawable.ic_phone
        val lockRes = if (c.lockIconRes != 0) c.lockIconRes else R.drawable.ic_lock

        binding.ivCamera.setImageResource(cameraRes)
        binding.ivPhone.setImageResource(phoneRes)
        binding.ivLock.setImageResource(lockRes)

        val clockFont = if (c.clockFontRes != 0) c.clockFontRes else R.font.samsung_one_700
        val dateFont = when {
            c.dateFontRes != 0 -> c.dateFontRes
            c.textFontRes != 0 -> c.textFontRes
            else -> R.font.samsung_one_700
        }
        val operatorFont = when {
            c.operatorFontRes != 0 -> c.operatorFontRes
            c.textFontRes != 0 -> c.textFontRes
            else -> R.font.samsung_one_400
        }
        val marqueeFont = when {
            c.marqueeFontRes != 0 -> c.marqueeFontRes
            c.textFontRes != 0 -> c.textFontRes
            else -> R.font.samsung_one_400
        }

        ResourcesCompat.getFont(ctx, clockFont)?.let { binding.tvBigClock.typeface = it }
        ResourcesCompat.getFont(ctx, dateFont)?.let { binding.tvDate.typeface = it }
        ResourcesCompat.getFont(ctx, operatorFont)?.let { binding.tvTicker.typeface = it }
        ResourcesCompat.getFont(ctx, marqueeFont)?.let { binding.tvMarqueeBottom.typeface = it }

        applyStatusPack(c.statusBarPackRes, binding)

        val batteryRes = if (c.batteryStyleRes != 0) c.batteryStyleRes else R.drawable.bg_battery_dynamic
        applyBatteryStyle(batteryRes, binding)
    }

    private fun applyStatusPack(statusPackRes: Int, binding: ActivityFakeLockBinding) {
        val name = runCatching {
            if (statusPackRes != 0) binding.root.resources.getResourceEntryName(statusPackRes) else ""
        }.getOrDefault("")

        when (name) {
            "pack_status_ios" -> {
                binding.ivWifi.setImageResource(resolveDrawable(binding, "ic_wifi_ios", R.drawable.ic_wifi))
                binding.iv4g1.setImageResource(resolveDrawable(binding, "ic_4g_ios", R.drawable.ic_4g))
                binding.ivSignal1.setImageResource(resolveDrawable(binding, "ic_signal_ios", R.drawable.ic_signal_plain))
                binding.iv4g2.setImageResource(resolveDrawable(binding, "ic_4g_ios", R.drawable.ic_4g))
                binding.ivSignal2.setImageResource(resolveDrawable(binding, "ic_signal_ios", R.drawable.ic_signal_plain))
                binding.tvBatteryLevel.setBackgroundResource(resolveDrawable(binding, "bg_battery_ios", R.drawable.bg_battery_dynamic))
            }
            "pack_status_pixel" -> {
                binding.ivWifi.setImageResource(resolveDrawable(binding, "ic_wifi_pixel", R.drawable.ic_wifi))
                binding.iv4g1.setImageResource(resolveDrawable(binding, "ic_4g_pixel", R.drawable.ic_4g))
                binding.ivSignal1.setImageResource(resolveDrawable(binding, "ic_signal_pixel", R.drawable.ic_signal_plain))
                binding.iv4g2.setImageResource(resolveDrawable(binding, "ic_4g_pixel", R.drawable.ic_4g))
                binding.ivSignal2.setImageResource(resolveDrawable(binding, "ic_signal_pixel", R.drawable.ic_signal_plain))
                binding.tvBatteryLevel.setBackgroundResource(resolveDrawable(binding, "bg_battery_pixel", R.drawable.bg_battery_dynamic))
            }
            else -> {
                binding.ivWifi.setImageResource(R.drawable.ic_wifi)
                binding.iv4g1.setImageResource(R.drawable.ic_4g)
                binding.ivSignal1.setImageResource(R.drawable.ic_signal_plain)
                binding.iv4g2.setImageResource(R.drawable.ic_4g)
                binding.ivSignal2.setImageResource(R.drawable.ic_signal_plain)
                binding.tvBatteryLevel.setBackgroundResource(R.drawable.bg_battery_dynamic)
            }
        }
    }

    private fun applyBatteryStyle(resId: Int, binding: ActivityFakeLockBinding) {
        binding.ivWifi.setImageResource(R.drawable.ic_wifi)
        binding.iv4g1.setImageResource(R.drawable.ic_4g)
        binding.ivSignal1.setImageResource(R.drawable.ic_signal_plain)
        binding.iv4g2.setImageResource(R.drawable.ic_4g)
        binding.ivSignal2.setImageResource(R.drawable.ic_signal_plain)

        binding.tvBatteryLevel.setBackgroundResource(resId)
        binding.tvBatteryLevel.tag = resId

        val textColor = if (resId == R.drawable.bg_battery_dynamic) Color.BLACK else Color.WHITE
        binding.tvBatteryLevel.setTextColor(textColor)
    }

    private fun resolveDrawable(binding: ActivityFakeLockBinding, name: String, fallback: Int): Int {
        val res = binding.root.resources
        val pkg = binding.root.context.packageName
        val id = res.getIdentifier(name, "drawable", pkg)
        return if (id != 0) id else fallback
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