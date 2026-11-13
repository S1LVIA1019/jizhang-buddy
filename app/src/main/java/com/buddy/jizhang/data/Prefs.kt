package com.buddy.jizhang.data

import android.content.Context
import android.content.SharedPreferences

object Prefs {

    private lateinit var sp: SharedPreferences

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("jizhang", Context.MODE_PRIVATE)
    }

    private fun gs(k: String, def: String): String = sp.getString(k, def) ?: def
    private fun ps(k: String, v: String) { sp.edit().putString(k, v).apply() }
    private fun gd(k: String, def: Double): Double = sp.getString(k, null)?.toDoubleOrNull() ?: def
    private fun pd(k: String, v: Double) { sp.edit().putString(k, v.toString()).apply() }

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
        set(v) = pd("limit_month", v)
    var yearLimit: Double
        get() = gd("limit_year", 0.0)
        set(v) = pd("limit_year", v)

    fun catLimit(cat: String): Double = gd("limit_cat_" + cat, 0.0)
    fun setCatLimit(cat: String, v: Double) = pd("limit_cat_" + cat, v)

    var allowance: Double
        get() = gd("allowance", 2000.0)
        set(v) = pd("allowance", v)

    var goalName: String
        get() = gs("goal_name", "旅游")
        set(v) = ps("goal_name", v)
    var goalAmount: Double
        get() = gd("goal_amount", 1000.0)
        set(v) = pd("goal_amount", v)

    var semesterStart: String
        get() = gs("semester_start", "")
        set(v) = ps("semester_start", v)
    var semesterEnd: String
        get() = gs("semester_end", "")
        set(v) = ps("semester_end", v)

    var warnPct: Int
        get() = gs("warn_pct", "80").toIntOrNull() ?: 80
        set(v) = ps("warn_pct", v.toString())

    var largeThreshold: Double
        get() = gd("large_threshold", 50.0)
        set(v) = pd("large_threshold", v)

    var overlayText: String
        get() = gs("overlay_text", "又来了？本月已花 ¥{已用}。")
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

    var triggerLog: String
        get() = gs("trigger_log", "")
        private set
    fun appendTriggerLog(s: String) {
        val old = triggerLog.split("\n").filter { it.isNotBlank() }.toMutableList()
        old.add(0, s)
        ps("trigger_log", old.take(20).joinToString("\n"))
    }
    fun clearTriggerLog() { ps("trigger_log", "") }

    var financeLog: String
        get() = gs("finance_log", "")
        private set
    fun appendFinanceLog(s: String) {
        val old = financeLog.split("\n").filter { it.isNotBlank() }.toMutableList()
        old.add(0, s)
        ps("finance_log", old.take(30).joinToString("\n"))
    }

    var reportHistory: String
        get() = gs("report_history", "")
        private set
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
