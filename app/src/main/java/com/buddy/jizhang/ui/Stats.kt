package com.buddy.jizhang.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ButtonDefaults
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
import com.buddy.jizhang.core.Tags
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
    val balance = income - expense

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                Period.entries.forEach { pd ->
                    val sel = pd == p
                    Box(
                        Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (sel) Blue else ChipBg)
                            .clickable { periodName = pd.name }
                            .padding(horizontal = 13.dp, vertical = 8.dp)
                    ) {
                        Text(pd.label, fontSize = 12.5.sp, color = if (sel) Color.White else Color(0xFF5A6072))
                    }
                }
            }
        }

        item {
            HeroCard(
                label = "${p.label}支出",
                big = "¥${fmt(expense)}",
                note = if (txs.isEmpty()) "本期还没有记录" else "共 ${txs.size} 笔，含未记收入",
                split = listOf(
                    "收入" to "¥${fmt(income)}",
                    (if (balance >= 0) "结余" else "超支") to "¥${fmt(kotlin.math.abs(balance))}",
                    "结余率" to if (income > 0) "${((balance / income) * 100).toInt()}%" else "—"
                )
            )
        }

        item {
            SectionCard("各品类限额") {
                if (ex.isEmpty()) {
                    Text("本${p.label}还没有支出记录", fontSize = 12.5.sp, color = TextGray)
                } else {
                    val byCat = ex.groupBy { it.category }
                        .map { it.key to it.value.sumOf { v -> v.amount } }
                        .sortedByDescending { it.second }
                    val maxV = byCat.maxOf { it.second }.coerceAtLeast(1.0)
                    byCat.forEach { (cat, v) ->
                        val lim = Prefs.catLimit(cat)
                        val over = lim > 0 && v >= lim
                        KV(cat, "¥${fmt(v)}" + if (lim > 0) " / ¥${fmt(lim)}" else "",
                            if (over) OverRed else TextDark)
                        Spacer(Modifier.height(2.dp))
                        Bar((v / maxV).toFloat().coerceIn(0.02f, 1f), if (over) OverRed else Blue)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        item {
            SectionCard("钱花在哪了 · 按你的备注") {
                val ma = Tags.memoAnalysis(ex)
                if (ma.isEmpty()) {
                    Text("还没有带标签的记录，记账时写清楚备注就能自动归类", fontSize = 11.5.sp, color = TextGray)
                } else {
                    ma.entries.take(6).forEach { (k, v) ->
                        KV(
                            "$k ${v.first} 次",
                            "¥${fmt(v.second)}",
                            if (k in listOf("外食", "外卖", "奶茶", "零食", "化妆品")) OverRed else TextDark
                        )
                    }
                }
            }
        }

        item {
            SectionCard("流水（${txs.size} 笔）") {
                if (txs.isEmpty()) {
                    Text("暂无记录", fontSize = 12.5.sp, color = TextGray)
                } else {
                    txs.take(50).forEach { tx ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.fillMaxWidth(0.7f)) {
                                Text(tx.memo.ifBlank { tx.category }, fontSize = 13.sp, color = TextDark)
                                Text(
                                    "${timeShort(tx.ts)} · ${tx.category} · ${tx.account}" +
                                        if (tx.tags.isNotBlank()) " · ${tx.tags}" else "",
                                    fontSize = 10.5.sp, color = TextGray
                                )
                            }
                            Text(
                                (if (tx.kind == "expense") "-¥" else "+¥") + fmt(tx.amount),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (tx.kind == "expense") TextDark else OkGreen
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { Export.share(ctx, p) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HeroInk)
            ) { Text("点击此处使用 Buddy 生成${p.label}个性化分析", fontSize = 12.5.sp) }

            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                Button(
                    onClick = { showReport = true },
                    modifier = Modifier.weight(1f).padding(end = 5.dp)
                ) { Text("生成${p.label}总结", fontSize = 12.sp) }
                OutlinedButton(
                    onClick = { Export.share(ctx, p) },
                    modifier = Modifier.weight(1f).padding(start = 5.dp)
                ) { Text("导出数据", fontSize = 12.sp) }
            }
            Spacer(Modifier.height(90.dp))
        }
    }
}

@Composable
fun ReportView(p: Period, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val text = remember(p) { Summary.forPeriod(ctx, p) }
    Column(Modifier.fillMaxSize().background(PageBg)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onClose) { Text("返回", fontSize = 12.sp) }
            Text(
                "${p.label}总结",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.padding(start = 14.dp)
            )
        }
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
            item {
                SectionCard("本地报告", container = SoftPurple) {
                    Text(text, fontSize = 12.5.sp, color = TextDark, lineHeight = 21.sp)
                }
            }
            item {
                SectionCard("导出") {
                    Text(
                        "把本期完整数据（含每笔备注原文和自动标签）导出成 JSON / CSV / MD，发到电脑上就能用 WorkBuddy 生成个性化分析。",
                        fontSize = 11.5.sp, color = TextGray, lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Row {
                        Button(
                            onClick = { Export.share(ctx, p) },
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        ) { Text("导出给 Buddy 分析", fontSize = 11.5.sp) }
                        OutlinedButton(
                            onClick = { Prefs.saveReport("${p.label}报·" + java.time.LocalDate.now().toString(), text) },
                            modifier = Modifier.weight(1f).padding(start = 6.dp)
                        ) { Text("存入历史总结", fontSize = 11.5.sp) }
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
