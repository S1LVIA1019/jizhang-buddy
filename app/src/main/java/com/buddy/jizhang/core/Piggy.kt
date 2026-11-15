package com.buddy.jizhang.core

import android.content.Context
import com.buddy.jizhang.data.Dao
import com.buddy.jizhang.data.Prefs
import com.buddy.jizhang.data.Settlement
import com.buddy.jizhang.data.toLocalDate
import com.buddy.jizhang.data.ymFormat
import java.time.LocalDate
import java.time.YearMonth

/**
 * 攒钱罐：余额 = 所有「已完结自然月」结余之和 − 累计罐内支出。
 * 当月进行中不计入；月末结算存快照，关机漏算时下次打开自动补算。
 */
object Piggy {

    fun ensureSettled(ctx: Context) {
        val firstTs = Dao.firstTxTs(ctx) ?: return
        var ym = YearMonth.from(firstTs.toLocalDate())
        val thisYm = YearMonth.now()
        if (!ym.isBefore(thisYm)) return
        val done = Dao.settlements(ctx).map { it.ym }.toHashSet()
        while (ym.isBefore(thisYm)) {
            val key = ym.format(ymFormat)
            if (key !in done) {
                val (f, t) = periodRange(Period.MONTH, ym.atDay(1))
                val income = Dao.sum(ctx, f, t, "income")
                val expense = Dao.sum(ctx, f, t, "expense")
                val bal = income - expense
                val after = Dao.settlementBalanceTotal(ctx) + bal - Dao.piggyTotal(ctx)
                Dao.insertSettlement(ctx, Settlement(key, income, expense, bal, after))
                Prefs.appendSettlement(
                    listOf(key, fmt(income), fmt(expense), fmt(bal), fmt(after)).joinToString("~")
                )
                done.add(key)
            }
            ym = ym.plusMonths(1)
        }
    }

    fun balance(ctx: Context): Double {
        ensureSettled(ctx)
        return Dao.settlementBalanceTotal(ctx) - Dao.piggyTotal(ctx)
    }

    fun goalLeft(ctx: Context): Double = Prefs.goalAmount - balance(ctx)

    fun reached(ctx: Context): Boolean = Prefs.goalAmount > 0 && balance(ctx) >= Prefs.goalAmount

    fun usedInMonth(ctx: Context, base: LocalDate = LocalDate.now()): Double {
        val (f, t) = periodRange(Period.MONTH, base)
        return Dao.sum(ctx, f, t, "piggy")
    }

    /** 本月预计结余 = 本月收入 − 本月日常支出 − (剩余天数 × 近 7 日日均支出) */
    fun expectedBalance(ctx: Context): Double {
        val now = LocalDate.now()
        val (mf, mt) = periodRange(Period.MONTH, now)
        val income = Dao.sum(ctx, mf, mt, "income")
        val spent = Dao.sum(ctx, mf, mt, "expense")
        val ym = YearMonth.from(now)
        val remainDays = (ym.lengthOfMonth() - now.dayOfMonth).coerceAtLeast(0)

        val sevenStart = now.minusDays(6)
        val sf = periodRange(Period.DAY, sevenStart).first
        val st = periodRange(Period.DAY, now.plusDays(1)).second
        val recent = Dao.sum(ctx, sf, st, "expense")
        val daily = if (recent > 0.0) recent / 7.0 else Budget.current(ctx).dailyCeil
        return income - spent - remainDays * daily
    }

    /** 悬浮窗第二行「还差」：目标 − 当前罐余额，动用后会变大，可为负 */
    fun goalLeftText(ctx: Context): String {
        val left = goalLeft(ctx)
        return if (left <= 0) "已攒够，还多出 ¥${fmt(-left)}" else "¥${fmt(left)}"
    }
}
