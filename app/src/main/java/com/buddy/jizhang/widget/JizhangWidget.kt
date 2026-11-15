package com.buddy.jizhang.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.buddy.jizhang.R
import com.buddy.jizhang.core.Budget
import com.buddy.jizhang.core.LifeAssist
import com.buddy.jizhang.core.Period
import com.buddy.jizhang.core.Piggy
import com.buddy.jizhang.core.fmt
import com.buddy.jizhang.core.periodRange
import com.buddy.jizhang.data.Dao
import com.buddy.jizhang.data.Prefs
import com.buddy.jizhang.ui.MainActivity

abstract class BaseWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdater.refresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> WidgetUpdater.refresh(context)
        }
    }
}

class SmallWidget : BaseWidget()
class BigWidget : BaseWidget()

object WidgetUpdater {

    private val Red = 0xFFD8382F.toInt()
    private val Dark = 0xFF1E2230.toInt()
    private val PurpleDark = 0xFF2E1A5E.toInt()
    private val Orange = 0xFFE06A5F.toInt()

    fun refresh(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val b = Budget.current(ctx)
        val pct = LifeAssist.progressPct(ctx)
        val (df, dt) = periodRange(Period.DAY)
        val today = Dao.sum(ctx, df, dt, "expense")
        val aheadLine = if (b.isAhead) "本月超前 ¥${fmt(b.ahead)}" else ""
        val tint = when {
            b.isAhead -> Red
            b.shouldSpend > 0 && b.spent / b.shouldSpend >= 0.8 -> Orange
            else -> Dark
        }

        val smallIds = mgr.getAppWidgetIds(ComponentName(ctx, SmallWidget::class.java))
        if (smallIds.isNotEmpty()) {
            val v = RemoteViews(ctx.packageName, R.layout.widget_small)
            v.setTextViewText(R.id.tv_pct, "$pct%")
            v.setTextColor(R.id.tv_pct, tint)
            v.setProgressBar(R.id.widget_bar, 100, pct.coerceAtMost(100), false)
            v.setTextViewText(R.id.tv_today, "今日 ¥${fmt(today)} / 日均 ¥${fmt(b.dailyCeil)}")
            if (b.isAhead) {
                v.setViewVisibility(R.id.tv_ahead, View.VISIBLE)
                v.setTextViewText(R.id.tv_ahead, aheadLine)
            } else v.setViewVisibility(R.id.tv_ahead, View.GONE)
            v.setOnClickPendingIntent(R.id.widget_root, openIntent(ctx, 1, 0))
            mgr.updateAppWidget(smallIds, v)
        }

        val bigIds = mgr.getAppWidgetIds(ComponentName(ctx, BigWidget::class.java))
        if (bigIds.isNotEmpty()) {
            val v = RemoteViews(ctx.packageName, R.layout.widget_big)
            v.setTextViewText(R.id.tv_pct, "$pct%")
            v.setTextColor(R.id.tv_pct, if (b.isAhead) Red else PurpleDark)
            v.setProgressBar(R.id.widget_bar, 100, pct.coerceAtMost(100), false)
            v.setTextViewText(R.id.tv_remain, "¥${fmt(b.remain.coerceAtLeast(0.0))}")
            v.setTextViewText(R.id.tv_perday, "¥${fmt(b.perDayLeft.coerceAtLeast(0.0))}")
            v.setTextViewText(R.id.tv_goal, Piggy.goalLeftText(ctx))
            v.setTextViewText(
                R.id.tv_tip,
                if (b.isAhead) "先停 10 秒，真的需要吗" else "「${Prefs.goalName}」加油"
            )
            if (b.isAhead) {
                v.setViewVisibility(R.id.tv_ahead, View.VISIBLE)
                v.setTextViewText(R.id.tv_ahead, aheadLine)
            } else v.setViewVisibility(R.id.tv_ahead, View.GONE)
            v.setOnClickPendingIntent(R.id.widget_root, openIntent(ctx, 3, 0))
            v.setOnClickPendingIntent(R.id.tv_action, openIntent(ctx, 4, 1))
            mgr.updateAppWidget(bigIds, v)
        }
    }

    private fun openIntent(ctx: Context, req: Int, tab: Int): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java)
            .putExtra("tab", tab)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            ctx, req, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
