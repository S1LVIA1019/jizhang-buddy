package com.buddy.jizhang.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.buddy.jizhang.core.Budget
import com.buddy.jizhang.core.Piggy
import com.buddy.jizhang.core.Summary
import com.buddy.jizhang.data.Prefs
import com.buddy.jizhang.widget.WidgetUpdater
import java.time.LocalDate
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(ctx: Context, intent: Intent) {
        runCatching {
            Piggy.ensureSettled(ctx)
            Budget.refreshNextRecommend(ctx)
            WidgetUpdater.refresh(ctx)
        }
        when (intent.action) {
            Alarms.A_DAILY -> {
                val b = Budget.current(ctx)
                val title = if (b.isAhead) "超前提醒 · 今日总结" else "今日总结"
                Notify.show(ctx, 101, Notify.CH_SUMMARY, title, Summary.daily(ctx))

                val today = LocalDate.now()
                if (today.plusDays(1).monthValue != today.monthValue) {
                    Budget.forceRefreshNextRecommend(ctx)
                    val report = Summary.monthly(ctx)
                    Prefs.saveReport("月报·" + dateTitle(), report)
                    Notify.show(
                        ctx, 104, Notify.CH_SUMMARY, "本月总结与下月推荐已生成",
                        "下月有效生活费 ¥${fmtNum(b.L)}，可花 ¥${fmtNum(b.C)}。" +
                            "在设置或月报里点「一键设置下月限额」即可应用。"
                    )
                }
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

    private fun fmtNum(d: Double): String = com.buddy.jizhang.core.fmt(d)

    private fun dateTitle(): String {
        val c = Calendar.getInstance()
        return "%04d-%02d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }
}
