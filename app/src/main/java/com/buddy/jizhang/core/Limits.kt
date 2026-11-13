package com.buddy.jizhang.core

import android.content.Context
import com.buddy.jizhang.data.Dao
import com.buddy.jizhang.data.Prefs
import kotlin.math.roundToInt

data class LimitStatus(val name: String, val limit: Double, val used: Double) {
    val pct: Int
        get() = if (limit <= 0) 0 else (used / limit * 100).roundToInt()

    /** -1 未设限额；0 正常；1 接近（达到预警阈值）；2 超限 */
    val state: Int
        get() {
            if (limit <= 0.0) return -1
            val r = used / limit
            return when {
                r >= 1.0 -> 2
                r >= Prefs.warnPct / 100.0 -> 1
                else -> 0
            }
        }
}

object Limits {

    fun periodLimit(p: Period): Double = when (p) {
        Period.DAY -> Prefs.dayLimit
        Period.WEEK -> Prefs.weekLimit
        Period.MONTH -> Prefs.monthLimit
        Period.YEAR -> Prefs.yearLimit
        Period.SEMESTER -> 0.0
    }

    fun statuses(ctx: Context, base: java.time.LocalDate = java.time.LocalDate.now()): List<LimitStatus> {
        val list = mutableListOf<LimitStatus>()
        for (p in listOf(Period.DAY, Period.WEEK, Period.MONTH, Period.YEAR)) {
            val lim = periodLimit(p)
            if (lim <= 0) continue
            val (f, t) = periodRange(p, base)
            list += LimitStatus(p.label + "限额", lim, Dao.sum(ctx, f, t, "expense"))
        }
        val (mf, mt) = periodRange(Period.MONTH, base)
        for (cat in Prefs.categories) {
            val lim = Prefs.catLimit(cat)
            if (lim <= 0) continue
            list += LimitStatus("$cat·月", lim, Dao.sum(ctx, mf, mt, "expense", cat))
        }
        return list
    }
}
