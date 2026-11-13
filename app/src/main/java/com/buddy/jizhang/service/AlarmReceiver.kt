package com.buddy.jizhang.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.buddy.jizhang.core.Summary
import com.buddy.jizhang.data.Prefs
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            Alarms.A_DAILY -> {
                val text = Summary.daily(ctx)
                Notify.show(ctx, 101, Notify.CH_SUMMARY, "今日总结", text)
                Alarms.set(ctx, Alarms.A_DAILY, Alarms.nextAt(null, Prefs.dailyTime))
            }
            Alarms.A_WEEKLY -> {
                val text = Summary.weekly(ctx)
                Notify.show(ctx, 102, Notify.CH_SUMMARY, "本周总结", text)
                Prefs.saveReport("周报·" + dateTitle(), text)
                Alarms.set(ctx, Alarms.A_WEEKLY, Alarms.nextAt(Calendar.SUNDAY, Prefs.weeklyTime))
            }
            Alarms.A_FINANCE -> {
                Notify.show(
                    ctx, 103, Notify.CH_FINANCE, "理财提醒",
                    "去银行 App 看一眼你买的理财利率有没有变化，顺手记一下。"
                )
                Prefs.appendFinanceLog(dateTitle())
                Alarms.set(ctx, Alarms.A_FINANCE, Alarms.nextAt(Calendar.SATURDAY, Prefs.financeTime))
            }
        }
    }

    private fun dateTitle(): String {
        val c = Calendar.getInstance()
        return "%04d-%02d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }
}
