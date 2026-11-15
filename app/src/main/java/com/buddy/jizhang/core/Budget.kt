package com.buddy.jizhang.core

import android.content.Context
import com.buddy.jizhang.data.Dao
import com.buddy.jizhang.data.Prefs
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.max

data class BudgetInfo(
    val L: Double,
    val B: Double,
    val S: Double,
    val C: Double,
    val days: Int,
    val passed: Int,
    val daysLeft: Int,
    val dailyCeil: Double,
    val shouldSpend: Double,
    val spent: Double,
    val remain: Double,
    val perDayLeft: Double,
    val ahead: Double,
    val aheadRatio: Double,
    val isAhead: Boolean,
    val lastOverspend: Double,
    val monthIncome: Double,
    val monthExpense: Double
)

object Budget {

    fun lastMonthOverspend(ctx: Context): Double {
        val (f, t) = periodRange(Period.MONTH, LocalDate.now().minusMonths(1))
        val income = Dao.sum(ctx, f, t, "income")
        val expense = Dao.sum(ctx, f, t, "expense")
        return max(0.0, expense - income)
    }

    fun current(ctx: Context): BudgetInfo {
        val now = LocalDate.now()
        val ym = YearMonth.from(now)
        val days = ym.lengthOfMonth()
        val passed = now.dayOfMonth
        val daysLeft = (days - passed).coerceAtLeast(1)

        val overspend = lastMonthOverspend(ctx)
        val L = (Prefs.allowance - overspend).coerceAtLeast(0.0)
        val B = L * Prefs.bufferPct / 100.0
        val S = Prefs.planSave
        val C = (L - B - S).coerceAtLeast(0.0)

        val (mf, mt) = periodRange(Period.MONTH, now)
        val monthIncome = Dao.sum(ctx, mf, mt, "income")
        val monthExpense = Dao.sum(ctx, mf, mt, "expense")
        val spent = monthExpense

        val dailyCeil = if (days > 0) C / days else 0.0
        val shouldSpend = if (days > 0) C * passed / days else 0.0
        val remain = C - spent
        val ahead = spent - shouldSpend
        val ratio = if (shouldSpend > 0.5) ahead / shouldSpend else 0.0
        val isAhead = shouldSpend > 0.5 && ratio >= Prefs.aheadPct / 100.0

        return BudgetInfo(
            L = L, B = B, S = S, C = C,
            days = days, passed = passed, daysLeft = daysLeft,
            dailyCeil = dailyCeil, shouldSpend = shouldSpend,
            spent = spent, remain = remain,
            perDayLeft = remain / daysLeft,
            ahead = ahead, aheadRatio = ratio, isAhead = isAhead,
            lastOverspend = overspend,
            monthIncome = monthIncome, monthExpense = monthExpense
        )
    }

    /** 下月推荐：过去 3 个月各品类加权（近月权重高）+ 惩罚规则，归一化到 C */
    fun recommend(ctx: Context): Pair<Double, Map<String, Double>> {
        val b = current(ctx)
        val C = b.C
        val cats = Prefs.categories
        val weights = linkedMapOf<String, Double>()

        for (cat in cats) {
            var w = 0.0
            var wsum = 0.0
            for (i in 1..3) {
                val weight = (4 - i).toDouble()
                val (f, t) = periodRange(Period.MONTH, LocalDate.now().minusMonths(i.toLong()))
                w += Dao.sum(ctx, f, t, "expense", cat) * weight
                wsum += weight
            }
            weights[cat] = if (wsum > 0) w / wsum else 0.0
        }

        if (weights.values.sum() <= 0.5) {
            val cur = cats.associateWith { Prefs.catLimit(it) }
            if (cur.values.sum() > 0.5) {
                for (cat in cats) weights[cat] = cur[cat] ?: 0.0
            } else {
                val tpl = mapOf(
                    "必要支出" to 0.15, "餐饮" to 0.50, "休闲娱乐" to 0.10,
                    "医疗" to 0.02, "工作学习" to 0.08, "出行" to 0.05, "其他" to 0.10
                )
                for (cat in cats) weights[cat] = C * (tpl[cat] ?: 0.05)
            }
        }

        val (lf, lt) = periodRange(Period.MONTH, LocalDate.now().minusMonths(1))
        val lastMa = Tags.memoAnalysis(Dao.all(ctx, lf, lt).filter { it.kind == "expense" })
        val outside = (lastMa["外食"]?.second ?: 0.0) + (lastMa["外卖"]?.second ?: 0.0)
        val treats = (lastMa["奶茶"]?.second ?: 0.0) + (lastMa["零食"]?.second ?: 0.0)

        val nec = (weights["必要支出"] ?: 0.0) * Prefs.necessityFactor
        weights["必要支出"] = nec.coerceAtLeast(C * 0.10)
        if (outside >= 50.0) {
            val food = weights["餐饮"] ?: 0.0
            weights["餐饮"] = food * Prefs.outsidePenalty
        }
        if (treats >= 30.0) {
            val funv = weights["休闲娱乐"] ?: 0.0
            weights["休闲娱乐"] = funv * Prefs.funPenalty
        }
        weights["餐饮"] = (weights["餐饮"] ?: 0.0).coerceAtMost(C * Prefs.foodCapPct / 100.0)

        val sum = weights.values.sum().coerceAtLeast(0.01)
        val result = linkedMapOf<String, Double>()
        for (cat in cats) {
            result[cat] = Math.round((weights[cat] ?: 0.0) / sum * C * 10.0) / 10.0
        }
        val total = C + b.B
        return total to result
    }

    fun nextYm(): String = YearMonth.now().plusMonths(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))

    /** 自动生成下月推荐快照；参数改了不覆盖，留给界面提示「重新生成」 */
    fun refreshNextRecommend(ctx: Context) {
        val ym = nextYm()
        if (Prefs.nextRecommendYm != ym || Prefs.nextRecommend.isBlank()) {
            val (total, cats) = recommend(ctx)
            Prefs.nextRecommend = Prefs.encodePlan(total, cats)
            Prefs.nextRecommendYm = ym
            Prefs.recommendVersion = Prefs.paramsVersion
        }
    }

    fun forceRefreshNextRecommend(ctx: Context) {
        val (total, cats) = recommend(ctx)
        Prefs.nextRecommend = Prefs.encodePlan(total, cats)
        Prefs.nextRecommendYm = nextYm()
        Prefs.recommendVersion = Prefs.paramsVersion
    }

    fun paramsChanged(): Boolean = Prefs.recommendVersion != Prefs.paramsVersion

    fun nextRecommendData(): Pair<Double, Map<String, Double>> =
        Prefs.decodePlan(Prefs.nextRecommend)

    /** 一键全部改为推荐值：本月立即生效，只影响本月，可恢复 */
    fun applyAllRecommendedNow(ctx: Context) {
        Prefs.backupLimits()
        val (total, cats) = recommend(ctx)
        Prefs.applyPlan(total, cats)
    }

    /** 一键设置下月限额：写入下月快照，跨月自动生效 */
    fun applyNextMonth(ctx: Context) {
        Prefs.backupLimits()
        val (total, cats) = nextRecommendData()
        Prefs.setMonthPlan(nextYm(), total, cats)
    }

    fun restore(ctx: Context) = Prefs.restoreLimits()

    fun aheadLine(ctx: Context): String? {
        val b = current(ctx)
        if (!b.isAhead) return null
        return "【超前提醒】本月超前 ¥${fmt(b.ahead)}：已花 ¥${fmt(b.spent)}，" +
            "按 ${b.passed} 号应花 ¥${fmt(b.shouldSpend)}。" +
            "接下来 ${b.daysLeft} 天每天最多 ¥${fmt(b.perDayLeft.coerceAtLeast(0.0))}。"
    }

    fun transferCap(ctx: Context): Double = Prefs.transferCapPct / 100.0
}
