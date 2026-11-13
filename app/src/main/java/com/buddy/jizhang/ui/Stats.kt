package com.buddy.jizhang.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buddy.jizhang.core.Export
import com.buddy.jizhang.core.Period
import com.buddy.jizhang.core.Summary
import com.buddy.jizhang.core.fmt
import com.buddy.jizhang.core.periodRange
import com.buddy.jizhang.data.Dao
import com.buddy.jizhang.data.Prefs

@Composable
fun StatsScreen(tick: Int) {
    val ctx = LocalContext.current
    var periodName by rememberSaveable { mutableStateOf(Period.MONTH.name) }
    var showReport by remember { mutableStateOf(false) }
    val p = Period.valueOf(periodName)

    if (showReport) {
        ReportView(p) { showReport = false }
        return
    }

    val range = remember(tick, periodName) { periodRange(p) }
    val expense = remember(tick, periodName) { Dao.sum(ctx, range.first, range.second, "expense") }
    val income = remember(tick, periodName) { Dao.sum(ctx, range.first, range.second, "income") }
    val txs = remember(tick, periodName) { Dao.all(ctx, range.first, range.second) }
    val ex = remember(tick, periodName) { txs.filter { it.kind == "expense" } }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Period.entries.forEach { pd ->
                    val sel = pd == p
                    Box(
                        Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) Blue else Color(0xFFEEF1F6))
                            .clickable { periodName = pd.name }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(pd.label, fontSize = 13.sp, color = if (sel) Color.White else Color(0xFF374151))
                    }
                }
            }
        }

        item {
            SectionCard("${p.label}收支（${if (expense == 0.0 && txs.isEmpty()) "无记录" else "含未记收入"}）") {
                Text("支出 ¥${fmt(expense)}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(Modifier.height(8.dp))
                KV("收入", "¥${fmt(income)}")
                KV(
                    if (income - expense >= 0) "结余" else "超支",
                    "¥${fmt(kotlin.math.abs(income - expense))}",
                    if (income - expense >= 0) OkGreen else OverRed
                )
                if (income > 0) {
                    KV("结余率", "${((income - expense) / income * 100).toInt()}%")
                }
            }
        }

        item {
            SectionCard("分类") {
                if (ex.isEmpty()) {
                    Text("本${p.label}还没有支出记录", fontSize = 13.sp, color = TextGray)
                } else {
                    val byCat = ex.groupBy { it.category }
                        .map { it.key to it.value.sumOf { v -> v.amount } }
                        .sortedByDescending { it.second }
                    val maxV = byCat.maxOf { it.second }.coerceAtLeast(1.0)
                    byCat.forEach { (cat, v) ->
                        val lim = Prefs.catLimit(cat)
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat, fontSize = 13.sp, color = TextDark, modifier = Modifier.fillMaxWidth(0.28f))
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Color(0xFFE8ECF2))
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth((v / maxV).toFloat().coerceIn(0.02f, 1f))
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(if (lim > 0 && v >= lim) OverRed else Blue)
                                )
                            }
                            Text(
                                "¥${fmt(v)}" + if (lim > 0) " / ${fmt(lim)}" else "",
                                fontSize = 11.sp,
                                color = TextGray,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionCard("流水（${txs.size} 笔）") {
                if (txs.isEmpty()) {
                    Text("暂无记录", fontSize = 13.sp, color = TextGray)
                } else {
                    txs.take(50).forEach { tx ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.fillMaxWidth(0.7f)) {
                                Text(
                                    tx.memo.ifBlank { tx.category },
                                    fontSize = 13.5.sp, color = TextDark
                                )
                                Text(
                                    "${timeShort(tx.ts)} · ${tx.category} · ${tx.account}" +
                                        if (tx.tags.isNotBlank()) " · ${tx.tags}" else "",
                                    fontSize = 10.5.sp, color = TextGray
                                )
                            }
                            Text(
                                (if (tx.kind == "expense") "-¥" else "+¥") + fmt(tx.amount),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (tx.kind == "expense") TextDark else OkGreen
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = { showReport = true },
                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                ) { Text("生成${p.label}总结") }
                OutlinedButton(
                    onClick = { Export.share(ctx, p) },
                    modifier = Modifier.weight(1f).padding(start = 6.dp)
                ) { Text("导出数据") }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun ReportView(p: Period, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val text = remember(p) { Summary.forPeriod(ctx, p) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6FA))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onClose) { Text("返回") }
            Text(
                "${p.label}总结",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 14.dp)
            )
        }
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            item {
                SectionCard("本地报告") {
                    Text(text, fontSize = 13.sp, color = TextDark, lineHeight = 21.sp)
                }
            }
            item {
                SectionCard("导出") {
                    Text(
                        "把本期完整数据（含每笔备注原文）导出成 JSON/CSV/MD，发到电脑上就能用 WorkBuddy 生成个性化分析。",
                        fontSize = 12.sp, color = TextGray
                    )
                    Spacer(Modifier.height(10.dp))
                    Row {
                        Button(
                            onClick = { Export.share(ctx, p) },
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        ) { Text("导出给 Buddy 分析") }
                        OutlinedButton(
                            onClick = { Prefs.saveReport("${p.label}报·" + java.time.LocalDate.now().toString(), text) },
                            modifier = Modifier.weight(1f).padding(start = 6.dp)
                        ) { Text("存入历史总结") }
                    }
                }
            }
            item { Spacer(Modifier.height(60.dp)) }
        }
    }
}

private fun timeShort(ts: Long): String {
    val d = java.time.Instant.ofEpochMilli(ts).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
    return "%02d-%02d %02d:%02d".format(d.monthValue, d.dayOfMonth, d.hour, d.minute)
}
