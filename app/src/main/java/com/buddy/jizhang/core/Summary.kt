package com.buddy.jizhang.core

import android.content.Context
import com.buddy.jizhang.data.Dao
import com.buddy.jizhang.data.Prefs
import java.time.LocalDate

object Summary {

    fun forPeriod(ctx: Context, p: Period): String = when (p) {
        Period.DAY -> daily(ctx)
        Period.WEEK -> weekly(ctx)
        Period.MONTH -> monthly(ctx)
        Period.SEMESTER -> longPeriod(ctx, "学期")
        Period.YEAR -> longPeriod(ctx, "年")
    }

    fun daily(ctx: Context): String {
        val (f, t) = periodRange(Period.DAY)
        val ex = Dao.all(ctx, f, t).filter { it.kind == "expense" }
        val spent = ex.sumOf { it.amount }
        val (mf, mt) = periodRange(Period.MONTH)
        val monthUsed = Dao.sum(ctx, mf, mt, "expense")
        val sb = StringBuilder()
        sb.append("今天花了 ¥${fmt(spent)}（${ex.size} 笔）")
        if (ex.isEmpty()) {
            sb.append("\n今天还没有记录，记得记一笔。")
            return sb.toString()
        }
        val byCat = ex.groupBy { it.category }.map { "${it.key}¥${fmt(it.value.sumOf { v -> v.amount })}" }
        sb.append("\n").append(byCat.joinToString(" / "))
        if (Prefs.monthLimit > 0) {
            sb.append("\n本月已用 ¥${fmt(monthUsed)}，剩余 ¥${fmt((Prefs.monthLimit - monthUsed).coerceAtLeast(0.0))}")
        }
        val noMemo = ex.count { it.memo.isBlank() }
        if (noMemo > 0) sb.append("\n有 $noMemo 笔没写备注，补上能分析得更准。")
        sb.append("\n").append(dailyComment(ex))
        return sb.toString()
    }

    private fun dailyComment(ex: List<com.buddy.jizhang.data.Tx>): String {
        val ma = Tags.memoAnalysis(ex)
        val tea = ma["奶茶"]?.first ?: 0
        val snack = ma["零食"]?.first ?: 0
        val outside = ma["外食"]?.first ?: 0
        val large = ex.filter { it.amount >= Prefs.largeThreshold }
        return when {
            large.isNotEmpty() -> "今天最大一笔 ¥${fmt(large.maxOf { it.amount })}（${large.first { it.amount == large.maxOf { it2 -> it2.amount } }.memo.ifBlank { large[0].category } }），留意一下。"
            outside > 0 -> "今天外食 $outside 次，比食堂贵不少。"
            tea + snack >= 2 -> "今天奶茶+零食 ${tea + snack} 次了，控制一下。"
            else -> "今天花得挺克制，继续保持。"
        }
    }

    fun weekly(ctx: Context): String {
        val (f, t) = periodRange(Period.WEEK)
        val ex = Dao.all(ctx, f, t).filter { it.kind == "expense" }
        val spent = ex.sumOf { it.amount }
        val lastWeek = periodRange(Period.WEEK, LocalDate.now().minusWeeks(1))
        val lastSpent = Dao.sum(ctx, lastWeek.first, lastWeek.second, "expense")
        val ma = Tags.memoAnalysis(ex)
        val sb = StringBuilder()
        sb.append("本周花费 ¥${fmt(spent)}，日均 ¥${fmt(spent / 7.0)}")
        if (lastSpent > 0) {
            val diff = spent - lastSpent
            val pct = kotlin.math.abs(diff) / lastSpent * 100
            sb.append(if (diff >= 0) "，比上周多 ${pct.toInt()}%" else "，比上周少 ${pct.toInt()}%")
        }
        sb.append("\n外食 ${ma["外食"]?.first ?: 0} 次 / 外卖 ${ma["外卖"]?.first ?: 0} 次 / 奶茶 ${ma["奶茶"]?.first ?: 0} 杯 / 零食 ${ma["零食"]?.first ?: 0} 次")
        val biggest = ex.maxByOrNull { it.amount }
        if (biggest != null) {
            sb.append("\n最贵一笔：¥${fmt(biggest.amount)}（${biggest.memo.ifBlank { biggest.category }}）")
        }
        if (Prefs.monthLimit > 0) {
            val (mf, mt) = periodRange(Period.MONTH)
            val mu = Dao.sum(ctx, mf, mt, "expense")
            sb.append("\n本月已用 ¥${fmt(mu)} / ¥${fmt(Prefs.monthLimit)}")
        }
        sb.append("\n").append(weekComment(spent, ma))
        return sb.toString()
    }

    private fun weekComment(spent: Double, ma: Map<String, Pair<Int, Double>>): String {
        val outside = (ma["外食"]?.second ?: 0.0)
        val tea = (ma["奶茶"]?.second ?: 0.0) + (ma["零食"]?.second ?: 0.0)
        return when {
            spent > Prefs.monthLimit / 4.0 && Prefs.monthLimit > 0 -> "这一周就花掉月限额的四分之一以上，后面要收着点。"
            outside >= 100 -> "外食花了 ¥${fmt(outside)}，够食堂吃 ${outside.div(12).toInt()} 顿。"
            tea >= 50 -> "奶茶零食花了 ¥${fmt(tea)}，这部分最容易省。"
            else -> "节奏不错，照这个花法月底能留出结余。"
        }
    }

    fun monthly(ctx: Context): String {
        val (f, t) = periodRange(Period.MONTH)
        val all = Dao.all(ctx, f, t)
        val ex = all.filter { it.kind == "expense" }
        val income = all.filter { it.kind == "income" }.sumOf { it.amount }
        val spent = ex.sumOf { it.amount }
        val balance = income - spent
        val rate = if (income > 0) balance / income else 0.0
        val limit = Prefs.monthLimit
        val limitUse = if (limit > 0) spent / limit else 0.0
        val memoRate = if (ex.isNotEmpty()) ex.count { it.memo.isNotBlank() }.toDouble() / ex.size else 0.0
        val ma = Tags.memoAnalysis(ex)
        val canteen = (ma["食堂"]?.second ?: 0.0) + (ma["水果"]?.second ?: 0.0)
        val foodKeys = setOf("食堂", "外食", "外卖", "奶茶", "零食", "水果")
        val foodTotal = ma.filterKeys { it in foodKeys }.values.sumOf { it.second }
        val canteenRatio = if (foodTotal > 0) canteen / foodTotal else 0.0
        val score = healthScore(limitUse, rate, canteenRatio, memoRate)

        val sb = StringBuilder()
        sb.append("【1】结论：本月花 ¥${fmt(spent)}")
        if (limit > 0) sb.append("，占限额 ${(limitUse * 100).toInt()}%")
        val lastM = periodRange(Period.MONTH, LocalDate.now().minusMonths(1))
        val lastSpent = Dao.sum(ctx, lastM.first, lastM.second, "expense")
        if (lastSpent > 0) {
            val diff = spent - lastSpent
            sb.append(if (diff >= 0) "，比上月多 ¥${fmt(diff)}" else "，比上月少 ¥${fmt(-diff)}")
        }
        sb.append("。消费健康度 $score 分，").append(tone(score)).append("\n")

        sb.append("【2】收支：收入 ¥${fmt(income)}，支出 ¥${fmt(spent)}，")
        if (balance >= 0) sb.append("省下 ¥${fmt(balance)}（结余率 ${(rate * 100).toInt()}%）")
        else sb.append("超支 ¥${fmt(-balance)}")
        sb.append("\n")

        val byCat = ex.groupBy { it.category }.map { it.key to it.value.sumOf { v -> v.amount } }.sortedByDescending { it.second }
        sb.append("【3】流向：").append(
            byCat.take(3).joinToString("、") { "${it.first} ¥${fmt(it.second)}(${(it.second / spent * 100).toInt()}%)" }
                .ifBlank { "无记录" }
        ).append("\n")

        sb.append("【4】消费结构：食堂 ${ma["食堂"]?.first ?: 0} 顿 ¥${fmt(ma["食堂"]?.second ?: 0.0)}")
        sb.append(" / 外食 ${ma["外食"]?.first ?: 0} 次 ¥${fmt(ma["外食"]?.second ?: 0.0)}")
        sb.append(" / 外卖 ${ma["外卖"]?.first ?: 0} 次 ¥${fmt(ma["外卖"]?.second ?: 0.0)}")
        sb.append(" / 奶茶 ${ma["奶茶"]?.first ?: 0} 杯 ¥${fmt(ma["奶茶"]?.second ?: 0.0)}")
        sb.append(" / 零食 ${ma["零食"]?.first ?: 0} 次 ¥${fmt(ma["零食"]?.second ?: 0.0)}")
        val outside = ma["外食"]?.second ?: 0.0
        if (outside > 0) sb.append("\n     外食如果都改食堂，可省约 ¥${fmt(outside * 0.6)}")
        sb.append("\n")

        val large = ex.filter { it.amount >= Prefs.largeThreshold }
        val maxSingle = ex.maxByOrNull { it.amount }
        sb.append("【5】异常：")
        if (maxSingle != null) sb.append("单笔最大 ¥${fmt(maxSingle.amount)}（${maxSingle.memo.ifBlank { maxSingle.category }}）")
        val overDays = runCatching {
            val days = mutableSetOf<LocalDate>()
            for (tx in ex) days.add(java.time.Instant.ofEpochMilli(tx.ts).atZone(java.time.ZoneId.systemDefault()).toLocalDate())
            days.count { d ->
                val (df, dt) = periodRange(Period.DAY, d)
                Prefs.dayLimit > 0 && Dao.sum(ctx, df, dt, "expense") > Prefs.dayLimit
            }
        }.getOrDefault(0)
        if (overDays > 0) sb.append("；超日限额 $overDays 天")
        if (large.size > 3) sb.append("；≥¥${fmt(Prefs.largeThreshold)} 的大额共 ${large.size} 笔")
        if (maxSingle == null) sb.append("本月还没有记录")
        sb.append("\n")

        val m1 = periodRange(Period.MONTH, LocalDate.now().minusMonths(1))
        val m2 = periodRange(Period.MONTH, LocalDate.now().minusMonths(2))
        val m3 = periodRange(Period.MONTH, LocalDate.now().minusMonths(3))
        sb.append("【6】趋势：近 4 月 ¥${fmt(Dao.sum(ctx, m3.first, m3.second, "expense"))} → ¥${fmt(Dao.sum(ctx, m2.first, m2.second, "expense"))} → ¥${fmt(Dao.sum(ctx, m1.first, m1.second, "expense"))} → 本月 ¥${fmt(spent)}\n")

        sb.append("【7】建议：")
        sb.append(advice(ma, byCat, limitUse)).append("\n")

        val saved = Dao.balance(ctx)
        val goalLeft = (Prefs.goalAmount - saved).coerceAtLeast(0.0)
        sb.append("【8】攒钱：累计结余 ¥${fmt(saved)}")
        if (Prefs.goalAmount > 0) {
            sb.append("，「${Prefs.goalName}」还差 ¥${fmt(goalLeft)}")
            val months = monthsToGoal(ctx)
            if (months > 0) sb.append("，按近三月平均 $months 个月可达")
            else if (months == 0) sb.append("，已达成！")
            else sb.append("，当前结余速度还不够，先稳住支出")
        }
        sb.append("\n")
        return sb.toString()
    }

    private fun advice(ma: Map<String, Pair<Int, Double>>, byCat: List<Pair<String, Double>>, limitUse: Double): String {
        val tips = mutableListOf<String>()
        val tea = (ma["奶茶"]?.second ?: 0.0) + (ma["零食"]?.second ?: 0.0)
        if (tea >= 30) tips.add("奶茶+零食 ¥${fmt(tea)}，每周砍一半一个月能省 ¥${fmt(tea / 2)}")
        val outside = ma["外食"]?.second ?: 0.0
        if (outside >= 80) tips.add("外食 ¥${fmt(outside)}，换成食堂大概省 ¥${fmt(outside * 0.6)}")
        val funCat = byCat.firstOrNull { it.first == "休闲娱乐" }?.second ?: 0.0
        val funLimit = Prefs.catLimit("休闲娱乐")
        if (funLimit > 0 && funCat >= funLimit * 0.7) tips.add("休闲娱乐已用 ${if (funLimit > 0) (funCat / funLimit * 100).toInt() else 100}%，月底前先停一停")
        if (limitUse >= 1.0) tips.add("本月已超限，接下来的消费先问自己一句：真的需要吗")
        if (tips.isEmpty()) tips.add("没有明显失控的地方，维持现状就好")
        return tips.take(3).joinToString("；")
    }

    fun longPeriod(ctx: Context, label: String): String {
        val p = if (label == "学期") Period.SEMESTER else Period.YEAR
        val (f, t) = periodRange(p)
        val all = Dao.all(ctx, f, t)
        val ex = all.filter { it.kind == "expense" }
        val spent = ex.sumOf { it.amount }
        val income = all.filter { it.kind == "income" }.sumOf { it.amount }
        val ma = Tags.memoAnalysis(ex)
        val sb = StringBuilder()
        sb.append("本${label}累计支出 ¥${fmt(spent)}（${ex.size} 笔），收入 ¥${fmt(income)}，")
        sb.append(if (income - spent >= 0) "结余 ¥${fmt(income - spent)}" else "超支 ¥${fmt(spent - income)}").append("\n")
        val byCat = ex.groupBy { it.category }.map { it.key to it.value.sumOf { v -> v.amount } }.sortedByDescending { it.second }
        sb.append("分类前五：").append(byCat.take(5).joinToString("、") { "${it.first} ¥${fmt(it.second)}" }.ifBlank { "无记录" }).append("\n")
        val canteenRatio = run {
            val c = (ma["食堂"]?.second ?: 0.0) + (ma["水果"]?.second ?: 0.0)
            val food = ma.filterKeys { it in setOf("食堂", "外食", "外卖", "奶茶", "零食", "水果") }.values.sumOf { it.second }
            if (food > 0) c / food else 0.0
        }
        sb.append("外食/外卖/奶茶/零食合计占比 ${((1.0 - canteenRatio) * 100).toInt()}%\n")
        sb.append("累计结余 ¥${fmt(Dao.balance(ctx))}，离「${Prefs.goalName}」还差 ¥${fmt((Prefs.goalAmount - Dao.balance(ctx)).coerceAtLeast(0.0))}")
        return sb.toString()
    }

    fun healthScore(limitUse: Double, savingRate: Double, canteenRatio: Double, memoRate: Double): Int {
        var s = 0
        s += if (limitUse <= 1.0) 30 else (30 - ((limitUse - 1.0) * 60).toInt()).coerceAtLeast(0)
        s += (savingRate * 40).toInt().coerceIn(0, 40)
        s += (canteenRatio * 20).toInt().coerceIn(0, 20)
        s += (memoRate * 10).toInt().coerceIn(0, 10)
        return s.coerceIn(0, 100)
    }

    fun monthsToGoal(ctx: Context): Int {
        val saved = Dao.balance(ctx)
        val goalLeft = Prefs.goalAmount - saved
        if (goalLeft <= 0) return 0
        var total = 0.0
        var months = 0
        for (i in 0 until 3) {
            val base = LocalDate.now().minusMonths(i.toLong()).withDayOfMonth(1)
            val (f, t) = periodRange(Period.MONTH, base)
            total += Dao.sum(ctx, f, t, "income") - Dao.sum(ctx, f, t, "expense")
            months++
        }
        val avg = if (months > 0) total / months else 0.0
        if (avg < 100) return -1
        return kotlin.math.ceil(goalLeft / avg).toInt()
    }

    private fun tone(score: Int): String = when {
        score >= 90 -> "这个月你太能打了。"
        score >= 75 -> "整体不错，还有一点点空间。"
        score >= 60 -> "过得去，但钱花得有点随意。"
        else -> "问题不小，下面挑重点讲。"
    }
}
