package com.buddy.jizhang.core

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.buddy.jizhang.data.Dao
import com.buddy.jizhang.data.Prefs
import com.buddy.jizhang.data.Tx
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Export {

    private val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    private fun esc(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")

    private fun timeStr(ts: Long): String =
        dtf.format(Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDateTime())

    fun build(ctx: Context, p: Period, allTime: Boolean = false): Triple<String, String, String> {
        val (f, t) = if (allTime) 0L to Long.MAX_VALUE else periodRange(p)
        val all = Dao.all(ctx, f, t)
        val ex = all.filter { it.kind == "expense" }
        val income = all.filter { it.kind == "income" }.sumOf { it.amount }
        val spent = ex.sumOf { it.amount }
        val ma = Tags.memoAnalysis(ex)
        val saved = Dao.balance(ctx)
        val goalLeft = (Prefs.goalAmount - saved).coerceAtLeast(0.0)
        val byCat = ex.groupBy { it.category }.map { it.key to it.value.sumOf { v -> v.amount } }.sortedByDescending { it.second }
        val byAccount = ex.groupBy { it.account }.map { it.key to it.value.sumOf { v -> v.amount } }.sortedByDescending { it.second }

        val limit = Prefs.monthLimit
        val json = buildString {
            append("{\n  \"meta\": { \"period\": \"${if (allTime) "all" else p.name}\", \"exported_at\": \"${timeStr(System.currentTimeMillis())}\" },\n")
            append("  \"overview\": {\n    \"income\": $income,\n    \"expense\": $spent,\n")
            append("    \"balance\": ${income - spent},\n")
            append("    \"saving_rate\": ${if (income > 0) String.format(java.util.Locale.CHINA, "%.3f", (income - spent) / income) else "0"},\n")
            append("    \"budget\": { \"limit\": $limit, \"used\": $spent, \"remaining\": ${(limit - spent).coerceAtLeast(0.0)} }\n  },\n")
            append("  \"goal\": { \"name\": \"${esc(Prefs.goalName)}\", \"target\": ${Prefs.goalAmount}, \"saved\": $saved, \"remaining\": $goalLeft },\n")
            append("  \"transactions\": [\n")
            append(all.joinToString(",\n") { tx -> txJson(tx) })
            append("\n  ],\n")
            append("  \"memo_analysis\": {\n")
            append(ma.entries.joinToString(",\n") { (k, v) -> "    \"${esc(k)}\": { \"count\": ${v.first}, \"amount\": ${v.second}" + " }" })            append("\n  },\n")
            append("  \"by_category\": [\n")
            append(byCat.joinToString(",\n") { "    { \"name\": \"${esc(it.first)}\", \"amount\": ${it.second} }" })
            append("\n  ],\n")
            append("  \"by_account\": [\n")
            append(byAccount.joinToString(",\n") { "    { \"name\": \"${esc(it.first)}\", \"amount\": ${it.second} }" })
            append("\n  ],\n")
            append("  \"limits\": {\n")
            append("    \"day\": ${Prefs.dayLimit}, \"week\": ${Prefs.weekLimit}, \"month\": ${Prefs.monthLimit}, \"year\": ${Prefs.yearLimit},\n")
            append("    \"category\": { ")
            append(Prefs.categories.filter { cat -> Prefs.catLimit(cat) > 0 }.joinToString(", ") { cat -> "\"${esc(cat)}\": ${Prefs.catLimit(cat)}" })
            append(" }\n  }\n}")
        }

        val csv = buildString {
            append("time,amount,kind,category,account,memo,tags,source,merchant\n")
            for (tx in all) {
                append("${timeStr(tx.ts)},${tx.amount},${tx.kind},${csvEsc(tx.category)},${csvEsc(tx.account)},${csvEsc(tx.memo)},${csvEsc(tx.tags)},${csvEsc(tx.source)},${csvEsc(tx.merchant)}\n")
            }
        }

        val md = buildString {
            append("# 记账buddy 数据导出（${if (allTime) "全部" else p.label}）\n\n")
            append("收入 ¥${fmt(income)}  支出 ¥${fmt(spent)}  结余 ¥${fmt(income - spent)}\n\n")
            append("## 分类\n")
            for ((k, v) in byCat) append("- $k：¥${fmt(v)}\n")
            append("\n## 备注结构\n")
            for ((k, v) in ma) append("- $k：${v.first} 次，¥${fmt(v.second)}\n")
            append("\n## 逐笔\n")
            for (tx in all) {
                append("- ${timeStr(tx.ts)} ${if (tx.kind == "expense") "-" else "+"}¥${fmt(tx.amount)} [${tx.category}] ${tx.account} ${tx.memo} ${if (tx.tags.isNotBlank()) "(${tx.tags})" else ""}\n")
            }
        }
        return Triple(json, csv, md)
    }

    private fun csvEsc(s: String): String =
        if (s.contains(',') || s.contains('"')) "\"" + s.replace("\"", "\"\"") + "\"" else s

    private fun txJson(tx: Tx): String = buildString {
        append("    { \"time\": \"${timeStr(tx.ts)}\", \"amount\": ${tx.amount}, \"type\": \"${tx.kind}\"")
        append(", \"category\": \"${esc(tx.category)}\", \"account\": \"${esc(tx.account)}\"")
        append(", \"memo\": \"${esc(tx.memo)}\", \"tags\": \"${esc(tx.tags)}\"")
        append(", \"source\": \"${esc(tx.source)}\", \"merchant\": \"${esc(tx.merchant)}\" }")
    }

    fun share(ctx: Context, p: Period, allTime: Boolean = false) {
        val (json, csv, md) = build(ctx, p, allTime)
        val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
        val stamp = java.time.LocalDate.now().toString() + (if (allTime) "-all" else "-" + p.name.lowercase())
        val uris = listOf(
            "jizhang-$stamp.json" to json,
            "jizhang-$stamp.csv" to csv,
            "jizhang-$stamp.md" to md
        ).map { (name, content) ->
            val f = File(dir, name)
            f.writeText(content)
            FileProvider.getUriForFile(ctx, "com.buddy.jizhang.fileprovider", f)
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            ctx.startActivity(Intent.createChooser(intent, "导出数据").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
