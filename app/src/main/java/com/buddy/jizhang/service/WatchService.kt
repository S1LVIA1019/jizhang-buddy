package com.buddy.jizhang.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import com.buddy.jizhang.R
import com.buddy.jizhang.core.Budget
import com.buddy.jizhang.core.Period
import com.buddy.jizhang.core.Piggy
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
        val v = LayoutInflater.from(this).inflate(R.layout.overlay_view, null)

        val d = data()
        val aheadWord = if (d.isAhead) "本月已超前 ¥${fmt(d.ahead)}，还买？" else ""
        val main = Prefs.overlayText
            .replace("{已用}", fmt(d.used))
            .replace("{剩余}", fmt(d.remain))
            .replace("{目标名}", Prefs.goalName)
            .replace("{还差}", fmt(d.goalLeft))
            .replace("{超前}", aheadWord)
            .replace("{金额}", "")
            .trim()

        if (d.isAhead) {
            v.setBackgroundResource(R.drawable.overlay_bg_ahead)
            v.findViewById<TextView>(R.id.tv_title).text = "记账buddy · 本月超前 ¥${fmt(d.ahead)}"
        } else {
            v.setBackgroundResource(R.drawable.overlay_bg)
            v.findViewById<TextView>(R.id.tv_title).text = "记账buddy"
        }
        v.findViewById<TextView>(R.id.tv_main).text = main
        v.findViewById<TextView>(R.id.tv_sub).text =
            "本月预计能存 ¥${fmt(d.expected)}\n「${Prefs.goalName}」还差 ${Piggy.goalLeftText(this)}"

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

        if (pkg != null) Prefs.appendTriggerLog("${nowStr()} ${Prefs.appNames[pkg] ?: pkg}")
    }

    fun hide() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        overlay?.let { runCatching { wm.removeView(it) } }
        overlay = null
    }

    private fun data(): OverlayData {
        val (f, t) = periodRange(Period.MONTH)
        val used = Dao.sum(this, f, t, "expense")
        val b = Budget.current(this)
        return OverlayData(
            used = used,
            remain = b.remain,
            expected = Piggy.expectedBalance(this),
            goalLeft = Piggy.goalLeft(this),
            ahead = b.ahead,
            isAhead = b.isAhead
        )
    }

    private data class OverlayData(
        val used: Double,
        val remain: Double,
        val expected: Double,
        val goalLeft: Double,
        val ahead: Double,
        val isAhead: Boolean
    )

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
        hide()
        super.onDestroy()
    }
}
