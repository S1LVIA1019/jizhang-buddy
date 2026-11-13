package com.buddy.jizhang.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object Alarms {
    const val A_DAILY = "com.buddy.jizhang.DAILY"
    const val A_WEEKLY = "com.buddy.jizhang.WEEKLY"
    const val A_FINANCE = "com.buddy.jizhang.FINANCE"

    private fun parseTime(s: String): Pair<Int, Int> {
        val parts = s.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 22
        val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 30
        return h to m
    }

    /** dayOfWeek: null=每天；1=周日 … 7=周六（Calendar 常量） */
    fun nextAt(dayOfWeek: Int?, time: String): Long {
        val (h, m) = parseTime(time)
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, h)
        cal.set(Calendar.MINUTE, m)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (dayOfWeek != null) {
            var delta = (dayOfWeek - cal.get(Calendar.DAY_OF_WEEK) + 7) % 7
            if (delta == 0 && cal.timeInMillis <= System.currentTimeMillis()) delta = 7
            cal.add(Calendar.DAY_OF_YEAR, delta)
        } else if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun pending(ctx: Context, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, action.hashCode(),
            Intent(action).setClass(ctx, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun set(ctx: Context, action: String, at: Long) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(ctx, action)
        val exact = if (Build.VERSION.SDK_INT >= 31) am.canScheduleExactAlarms() else true
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    fun scheduleAll(ctx: Context) {
        set(ctx, A_DAILY, nextAt(null, com.buddy.jizhang.data.Prefs.dailyTime))
        set(ctx, A_WEEKLY, nextAt(Calendar.SUNDAY, com.buddy.jizhang.data.Prefs.weeklyTime))
        set(ctx, A_FINANCE, nextAt(Calendar.SATURDAY, com.buddy.jizhang.data.Prefs.financeTime))
    }
}
