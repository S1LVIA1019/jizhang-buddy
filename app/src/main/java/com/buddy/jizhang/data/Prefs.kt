package com.buddy.jizhang.data

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object Prefs {

    private lateinit var sp: SharedPreferences

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("jizhang", Context.MODE_PRIVATE)
        if (gs("base_plan", "").isBlank()) {
            val (t, c) = snapshotCurrent()
            basePlan = encodePlan(t, c)
        }
        ensureMonthPlan(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")))
    }

    private fun gs(k: String, def: String): String = sp.getString(k, def) ?: def
    private fun ps(k: String, v: String) { sp.edit().putString(k, v).apply() }
    private fun gd(k: String, def: Double): Double = sp.getString(k, null)?.toDoubleOrNull() ?: def
    private fun pd(k: String, v: Double) { sp.edit().putString(k, v.toString()).apply() }
    private fun gi(k: String, def: Int): Int = sp.getString(k, null)?.toIntOrNull() ?: def
    private fun pi(k: String, v: Int) { sp.edit().putString(k, v.toString()).apply() }
    private fun gb(k: String, def: Boolean): Boolean = sp.getString(k, null)?.toBooleanStrictOrNull() ?: def
    private fun pb(k: String, v: Boolean) { sp.edit().putString(k, v.toString()).apply() }

    val defaultCategories = listOf("必要支出", "餐饮", "休闲娱乐", "医疗", "工作学习", "出行", "其他")
    val defaultAccounts = listOf("校园卡", "微信", "支付宝", "银行卡", "现金")

    var categories: List<String>
        get() = gs("categories", defaultCategories.joinToString(",")).split(",").filter { it.isNotBlank() }
        set(v) = ps("categories", v.joinToString(","))

    var accounts: List<String>
        get() = gs("accounts", defaultAccounts.joinToString(",")).split(",").filter { it.isNotBlank() }
        set(v) = ps("accounts", v.joinToString(","))

    var dayLimit: Double
        get() = gd("limit_day", 0.0)
        set(v) = pd("limit_day", v)
    var weekLimit: Double
        get() = gd("limit_week", 0.0)
        set(v) = pd("limit_week", v)
    var monthLimit: Double
        get() = gd("limit_month", 2000.0)
        set(v) { pd("limit_month", v); syncCurrentPlan() }
    var yearLimit: Double
        get() = gd("limit_year", 0.0)
        set(v) = pd("limit_year", v)

    fun catLimit(cat: String): Double = gd("limit_cat_" + cat, 0.0)
    fun setCatLimit(cat: String, v: Double) {
        pd("limit_cat_" + cat, v)
        syncCurrentPlan()
    }

    var allowance: Double
        get() = gd("allowance", 2000.0)
        set(v) = pd("allowance", v)

    var goalName: String
        get() = gs("goal_name", "旅游")
        set(v) = ps("goal_name", v)
    var goalAmount: Double
        get() = gd("goal_amount", 1000.0)
        set(v) = pd("goal_amount", v)

    /** 排队中的目标，每行 名称~金额 */
    var goalQueue: String
        get() = gs("goal_queue", "")
        set(v) = ps("goal_queue", v)
    fun queuedGoals(): List<Pair<String, Double>> =
        goalQueue.split("\n").filter { it.isNotBlank() }.mapNotNull { line ->
            val i = line.indexOf('~')
            if (i < 0) null else line.substring(0, i) to (line.substring(i + 1).toDoubleOrNull() ?: 0.0)
        }
    fun setQueuedGoals(list: List<Pair<String, Double>>) {
        goalQueue = list.joinToString("\n") { it.first + "~" + it.second }
    }

    /** 已达成目标归档，每行 名称~金额~日期 */
    val goalArchive: String
        get() = gs("goal_archive", "")
    fun archiveGoal(name: String, amount: Double, date: String) {
        ps("goal_archive", (name + "~" + amount + "~" + date + "\n" + goalArchive).take(2000))
    }

    var semesterStart: String
        get() = gs("semester_start", "")
        set(v) = ps("semester_start", v)
    var semesterEnd: String
        get() = gs("semester_end", "")
        set(v) = ps("semester_end", v)

    var warnPct: Int
        get() = gi("warn_pct", 80)
        set(v) = pi("warn_pct", v)

    var largeThreshold: Double
        get() = gd("large_threshold", 50.0)
        set(v) = pd("large_threshold", v)

    // ---------- 生活助手可配置参数 ----------
    var planSave: Double
        get() = gd("plan_save", 500.0)
        set(v) { pd("plan_save", v); bumpParams() }
    var bufferPct: Double
        get() = gd("buffer_pct", 10.0)
        set(v) { pd("buffer_pct", v); bumpParams() }
    var aheadPct: Double
        get() = gd("ahead_pct", 10.0)
        set(v) = pd("ahead_pct", v)
    var foodCapPct: Double
        get() = gd("food_cap_pct", 60.0)
        set(v) { pd("food_cap_pct", v); bumpParams() }
    var outsidePenalty: Double
        get() = gd("outside_penalty", 0.9)
        set(v) { pd("outside_penalty", v); bumpParams() }
    var funPenalty: Double
        get() = gd("fun_penalty", 0.8)
        set(v) { pd("fun_penalty", v); bumpParams() }
    var necessityFactor: Double
        get() = gd("necessity_factor", 1.05)
        set(v) { pd("necessity_factor", v); bumpParams() }
    var transferCapPct: Double
        get() = gd("transfer_cap_pct", 20.0)
        set(v) = pd("transfer_cap_pct", v)

    /** 打车「合理理由」关键词，逗号分隔，可编辑 */
    var reasonKeywords: String
        get() = gs("reason_keywords", "赶时间,下雨,生病,着急,同学,一起,聚会,行李,太晚,特殊,搬家,考试,加班")
        set(v) = ps("reason_keywords", v)
    fun reasonList(): List<String> = reasonKeywords.split(",", "，").map { it.trim() }.filter { it.isNotEmpty() }

    /** 参数版本号：改动后提示重新生成下月推荐 */
    val paramsVersion: Int
        get() = gi("params_version", 0)
    private fun bumpParams() { pi("params_version", paramsVersion + 1) }
    var recommendVersion: Int
        get() = gi("recommend_version", 0)
        set(v) = pi("recommend_version", v)

    // ---------- 月度限额快照（只对本月生效、可恢复） ----------
    private val ymFmt = DateTimeFormatter.ofPattern("yyyy-MM")
    fun currentYm(): String = LocalDate.now().format(ymFmt)

    /** 编码：总支出上限|品类=值;品类=值 */
    fun encodePlan(total: Double, cats: Map<String, Double>): String =
        total.toString() + "|" + cats.entries.joinToString(";") { it.key + "=" + it.value }

    fun decodePlan(s: String): Pair<Double, Map<String, Double>> {
        if (s.isBlank()) return 0.0 to emptyMap()
        val parts = s.split("|", limit = 2)
        val total = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
        val cats = linkedMapOf<String, Double>()
        if (parts.size > 1) for (e in parts[1].split(";")) {
            if (e.isBlank()) continue
            val i = e.indexOf('=')
            if (i > 0) {
                val v = e.substring(i + 1).toDoubleOrNull()
                if (v != null) cats[e.substring(0, i)] = v
            }
        }
        return total to cats
    }

    fun monthPlan(ym: String): String = gs("plan_" + ym, "")
    fun setMonthPlan(ym: String, total: Double, cats: Map<String, Double>) =
        ps("plan_" + ym, encodePlan(total, cats))

    var basePlan: String
        get() = gs("base_plan", "")
        set(v) = ps("base_plan", v)

    /** 改限额前自动留一份快照，供「恢复我原来的限额」 */
    var limitBackup: String
        get() = gs("limit_backup", "")
        set(v) = ps("limit_backup", v)

    private var appliedMonth: String
        get() = gs("applied_month", "")
        set(v) = ps("applied_month", v)

    fun snapshotCurrent(): Pair<Double, Map<String, Double>> {
        val cats = linkedMapOf<String, Double>()
        for (c in categories) cats[c] = catLimit(c)
        return monthLimit to cats
    }

    fun backupLimits() {
        val (t, c) = snapshotCurrent()
        limitBackup = encodePlan(t, c)
    }

    fun restoreLimits() {
        val (t, c) = decodePlan(limitBackup)
        if (limitBackup.isBlank()) return
        pd("limit_month", t)
        for (cat in categories) pd("limit_cat_" + cat, c[cat] ?: 0.0)
        syncCurrentPlan()
    }

    fun applyPlan(total: Double, cats: Map<String, Double>) {
        pd("limit_month", total)
        for (cat in categories) pd("limit_cat_" + cat, cats[cat] ?: 0.0)
        syncCurrentPlan()
    }

    private fun syncCurrentPlan() {
        val (t, c) = snapshotCurrent()
        setMonthPlan(currentYm(), t, c)
    }

    /** 跨月自动切换：有该月快照就用，没有则回基准 */
    fun ensureMonthPlan(ym: String) {
        if (appliedMonth == ym) return
        val custom = monthPlan(ym)
        if (custom.isNotBlank()) {
            val (t, c) = decodePlan(custom)
            pd("limit_month", t)
            for (cat in categories) pd("limit_cat_" + cat, c[cat] ?: 0.0)
        } else {
            val (t, c) = decodePlan(basePlan)
            if (basePlan.isNotBlank()) {
                pd("limit_month", t)
                for (cat in categories) pd("limit_cat_" + cat, c[cat] ?: 0.0)
            }
        }
        appliedMonth = ym
    }

    fun saveBasePlan() {
        val (t, c) = snapshotCurrent()
        basePlan = encodePlan(t, c)
    }

    /** 月末自动生成的下月推荐，编码同 plan */
    var nextRecommend: String
        get() = gs("next_recommend", "")
        set(v) = ps("next_recommend", v)
    var nextRecommendYm: String
        get() = gs("next_recommend_ym", "")
        set(v) = ps("next_recommend_ym", v)

    /** 月末结算快照，每行 yyyy-MM~收入~支出~结余~结算后罐余额 */
    val settlements: String
        get() = gs("settlements", "")
    fun appendSettlement(line: String) {
        ps("settlements", (line + "\n" + settlements).take(8000))
    }
    fun settlementMonths(): Set<String> =
        settlements.split("\n").filter { it.isNotBlank() }.map { it.substringBefore('~') }.toSet()

    // ---------- 提醒 / 悬浮窗 ----------
    var overlayText: String
        get() = gs("overlay_text", "又来了？本月已花 ¥{已用}。{超前}")
        set(v) = ps("overlay_text", v)

    val defaultMonitored = setOf(
        "com.xunmeng.pinduoduo",
        "com.taobao.taobao",
        "com.jingdong.app.mall",
        "com.sankuai.meituan",
        "com.eg.android.AlipayGphone"
    )

    val candidateMonitored = mapOf(
        "com.xunmeng.pinduoduo" to "拼多多",
        "com.taobao.taobao" to "淘宝",
        "com.jingdong.app.mall" to "京东",
        "com.sankuai.meituan" to "美团",
        "com.eg.android.AlipayGphone" to "支付宝",
        "com.tmall.wireless" to "天猫",
        "com.ss.android.ug.aweme" to "抖音",
        "com.xingin.xhs" to "小红书"
    )

    val appNames: Map<String, String> = candidateMonitored

    var monitored: Set<String>
        get() = gs("monitored", defaultMonitored.joinToString(",")).split(",").filter { it.isNotBlank() }.toSet()
        set(v) = ps("monitored", v.joinToString(","))

    var dailyTime: String
        get() = gs("daily_time", "22:30")
        set(v) = ps("daily_time", v)
    var weeklyTime: String
        get() = gs("weekly_time", "21:00")
        set(v) = ps("weekly_time", v)
    var financeTime: String
        get() = gs("finance_time", "22:00")
        set(v) = ps("finance_time", v)

    var apiKey: String
        get() = gs("api_key", "")
        set(v) = ps("api_key", v)

    // ---------- 引导 / 更新 ----------
    var guideShown: Boolean
        get() = gb("guide_shown", false)
        set(v) = pb("guide_shown", v)
    var widgetGuideShown: Boolean
        get() = gb("widget_guide_shown", false)
        set(v) = pb("widget_guide_shown", v)
    var updateCheckOn: Boolean
        get() = gb("update_check_on", true)
        set(v) = pb("update_check_on", v)

    val triggerLog: String
        get() = gs("trigger_log", "")
    fun appendTriggerLog(s: String) {
        val old = triggerLog.split("\n").filter { it.isNotBlank() }.toMutableList()
        old.add(0, s)
        ps("trigger_log", old.take(20).joinToString("\n"))
    }
    fun clearTriggerLog() { ps("trigger_log", "") }

    val financeLog: String
        get() = gs("finance_log", "")
    fun appendFinanceLog(s: String) {
        val old = financeLog.split("\n").filter { it.isNotBlank() }.toMutableList()
        old.add(0, s)
        ps("finance_log", old.take(30).joinToString("\n"))
    }

    val reportHistory: String
        get() = gs("report_history", "")
    fun saveReport(title: String, text: String) {
        val entry = title + "\u241F" + text.replace("\n", "\u241E")
        val old = reportHistory.split("\n").filter { it.isNotBlank() }.toMutableList()
        old.add(0, entry)
        ps("report_history", old.take(30).joinToString("\n"))
    }
    fun historyEntries(): List<Pair<String, String>> =
        reportHistory.split("\n").filter { it.isNotBlank() }.map { e ->
            val i = e.indexOf("\u241F")
            if (i < 0) e to ""
            else e.substring(0, i) to e.substring(i + 1).replace("\u241E", "\n")
        }
}
