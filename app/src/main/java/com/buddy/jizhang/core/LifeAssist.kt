package com.buddy.jizhang.core

import android.content.Context
import com.buddy.jizhang.data.Dao
import com.buddy.jizhang.data.Prefs
import java.time.LocalDate

data class PlanOption(
    val title: String,
    val detail: String,
    val monthEndBalance: Double,
    val enabled: Boolean,
    val recommended: Boolean = false
)

object LifeAssist {

    fun status(ctx: Context): String {
        val b = Budget.current(ctx)
        return when {
            b.isAhead -> "超前 ¥${fmt(b.ahead)} · 按 ${b.passed} 号应花 ¥${fmt(b.shouldSpend)}，接下来每天最多 ¥${fmt(b.perDayLeft.coerceAtLeast(0.0))}"
            b.remain <= 0 -> "本月可花额已用完，接下来只动缓冲"
            else -> "进度正常 · 比计划少花 ¥${fmt(-b.ahead)}，接下来每天最多 ¥${fmt(b.perDayLeft)}"
        }
    }

    fun progressPct(ctx: Context): Int {
        val b = Budget.current(ctx)
        if (b.shouldSpend <= 0.5) return 0
        return (b.spent / b.shouldSpend * 100).toInt().coerceIn(0, 999)
    }

    fun aheadText(ctx: Context): String {
        val b = Budget.current(ctx)
        return if (b.isAhead) "本月已超前 ¥${fmt(b.ahead)}，还买？" else ""
    }

    fun treatHeavy(ctx: Context): Boolean {
        val (f, t) = periodRange(Period.MONTH, LocalDate.now())
        val ma = Tags.memoAnalysis(Dao.all(ctx, f, t).filter { it.kind == "expense" })
        val tea = ma["奶茶"]?.first ?: 0
        val snack = ma["零食"]?.first ?: 0
        val outside = ma["外食"]?.first ?: 0
        return tea + snack >= 3 || outside >= 2
    }

    /** 自由支配额的三套用法；备注显示奶茶外食偏多时自动划掉「放松」 */
    fun plans(ctx: Context): List<PlanOption> {
        val b = Budget.current(ctx)
        val free = b.remain.coerceAtLeast(0.0)
        val conservative = b.L - b.spent
        val afterSpend = b.B + b.S
        val heavy = treatHeavy(ctx)

        val keep = PlanOption(
            "保守 · 默认推荐",
            "这 ¥${fmt(free)} 不再额外花，全部留进攒钱罐",
            conservative, true, true
        )
        val balanced = PlanOption(
            "均衡",
            "娱乐 ¥${fmt(free * 0.35)} / 学习 ¥${fmt(free * 0.25)} / 留 ¥${fmt(free * 0.40)} 应急",
            afterSpend, true
        )
        val relaxed = PlanOption(
            "放松",
            if (heavy) "本月奶茶/外食偏多，这套已划掉" else "娱乐 ¥${fmt(free * 0.7)} / 奶茶零食 ¥${fmt(free * 0.3)}",
            afterSpend, !heavy
        )
        return listOf(keep, balanced, relaxed)
    }

    /** 品类额度转移建议，单次最多转入 transferCapPct */
    fun transferAdvice(ctx: Context): String {
        val b = Budget.current(ctx)
        val cats = Prefs.categories
        val (f, t) = periodRange(Period.MONTH, LocalDate.now())
        val rows = mutableListOf<String>()
        for (cat in cats) {
            val lim = Prefs.catLimit(cat)
            if (lim <= 0) continue
            val used = Dao.sum(ctx, f, t, "expense", cat)
            val left = lim - used
            if (left > 0) rows.add("$cat 还剩 ¥${fmt(left)}")
        }
        if (rows.isEmpty()) return "还没设品类限额，先看下面的推荐值"
        val cap = (b.C * Budget.transferCap(ctx))
        return rows.take(3).joinToString("；") + "。单次最多转 ¥${fmt(cap)}，别全匀给娱乐"
    }

    fun note(): String = "上面的「每天最多」是上限，不是要花完的目标。"
}
