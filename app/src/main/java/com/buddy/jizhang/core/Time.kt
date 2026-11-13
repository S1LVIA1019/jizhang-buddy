package com.buddy.jizhang.core

import com.buddy.jizhang.data.Prefs
import java.time.LocalDate
import java.time.ZoneId

enum class Period(val label: String) {
    DAY("日"), WEEK("周"), MONTH("月"), SEMESTER("学期"), YEAR("年")
}

private val zone: ZoneId = ZoneId.systemDefault()

private fun epoch(d: LocalDate): Long = d.atStartOfDay(zone).toInstant().toEpochMilli()

fun periodRange(p: Period, base: LocalDate = LocalDate.now()): Pair<Long, Long> = when (p) {
    Period.DAY -> epoch(base) to epoch(base.plusDays(1))
    Period.WEEK -> {
        val dow = base.dayOfWeek.value
        val s = base.minusDays((dow - 1).toLong())
        epoch(s) to epoch(s.plusDays(7))
    }
    Period.MONTH -> {
        val s = base.withDayOfMonth(1)
        epoch(s) to epoch(s.plusMonths(1))
    }
    Period.YEAR -> {
        val s = base.withDayOfYear(1)
        epoch(s) to epoch(s.plusYears(1))
    }
    Period.SEMESTER -> {
        val s = runCatching { LocalDate.parse(Prefs.semesterStart) }.getOrNull()
        val e = runCatching { LocalDate.parse(Prefs.semesterEnd) }.getOrNull()
        if (s == null || e == null || base.isBefore(s) || base.isAfter(e)) {
            val ms = base.withDayOfMonth(1)
            epoch(ms) to epoch(ms.plusMonths(1))
        } else {
            epoch(s) to epoch(e.plusDays(1))
        }
    }
}

fun fmt(d: Double): String {
    val abs = kotlin.math.abs(d)
    if (abs < 0.005) return "0"
    return if (kotlin.math.abs(d - d.toLong()) < 0.005) d.toLong().toString()
    else String.format(java.util.Locale.CHINA, "%.2f", d)
}
