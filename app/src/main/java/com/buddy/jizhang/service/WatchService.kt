package com.buddy.jizhang.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import com.buddy.jizhang.R
import com.buddy.jizhang.core.Period
import com.buddy.jizhang.core.Summary
import com.buddy.jizhang.core.fmt
import com.buddy.jizhang.core.periodRange
import com.buddy.jizhang.data.Dao
import com.buddy.jizhang.data.Prefs
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class WatchService : AccessibilityService() {

    companion object {
        @Volatile var instance: WatchService? = null
            private set

        fun showPreview(seconds: Int = 12): Boolean {
            val s = instance ?: return false
            s.preview(seconds)
            return true
        }
    }

    private var overlay: View? = null
    private var lastPkg: String? = null
    private var previewUntil = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (pkg == lastPkg) return
        lastPkg = pkg
        if (System.currentTimeMillis() < previewUntil) return
        if (pkg in Prefs.monitored) show(pkg) else hide()
    }

    fun preview(seconds: Int) {
        show(null)
        previewUntil = System.currentTimeMillis() + seconds * 1000L
    }

    private fun show(pkg: String?) {
        hide()
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val v = layoutInflater.inflate(R.layout.overlay_view, null)

        val (used, remain, goalLeft, months) = data()
        val main = Prefs.overlayText
            .replace("{已用}", fmt(used))
            .replace("{剩余}", fmt(remain))
            .replace("{目标名}", Prefs.goalName)
            .replace("{还差}", fmt(goalLeft))
            .replace("{N个月}", months.coerceAtLeast(1).toString())
            .replace("{金额}", "")

        v.findViewById<TextView>(R.id.tv_main).text = main
        val sub = buildString {
            append("「${Prefs.goalName}」还差 ¥${fmt(goalLeft)}")
            if (months > 0 && months <= 60) append("，照这速度还得 $months 个月")
        }
        v.findViewById<TextView>(R.id.tv_sub).text = sub

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        lp.y = 140

        makeDraggable(v, lp)

        var ok = true
        runCatching { wm.addView(v, lp) }.onFailure { ok = false }
        if (!ok) {
            lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            runCatching { wm.addView(v, lp) }
        }
        overlay = v

        if (pkg != null) {
            Prefs.appendTriggerLog("${nowStr()} ${Prefs.appNames[pkg] ?: pkg}")
        }
    }

    fun hide() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        overlay?.let { runCatching { wm.removeView(it) } }
        overlay = null
    }

    private fun data(): Data4 {
        val (f, t) = periodRange(Period.MONTH)
        val used = Dao.sum(this, f, t, "expense")
        val remain = (Prefs.monthLimit - used).coerceAtLeast(0.0)
        val saved = Dao.balance(this)
        val goalLeft = (Prefs.goalAmount - saved).coerceAtLeast(0.0)
        val months = Summary.monthsToGoal(this)
        return Data4(used, remain, goalLeft, months)
    }

    private data class Data4(val used: Double, val remain: Double, val goalLeft: Double, val months: Int)

    private fun makeDraggable(v: View, lp: WindowManager.LayoutParams) {
        var touchX = 0f
        var touchY = 0f
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchX = e.rawX; touchY = e.rawY; true
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.x += (e.rawX - touchX).toInt()
                    lp.y += (e.rawY - touchY).toInt()
                    touchX = e.rawX; touchY = e.rawY
                    runCatching {
                        (getSystemService(WINDOW_SERVICE) as WindowManager).updateViewLayout(v, lp)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun nowStr(): String =
        DateTimeFormatter.ofPattern("MM-dd HH:mm")
            .format(Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault()))

    override fun onInterrupt() { }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        hide()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
