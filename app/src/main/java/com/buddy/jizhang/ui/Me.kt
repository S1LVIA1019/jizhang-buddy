package com.buddy.jizhang.ui

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buddy.jizhang.core.Export
import com.buddy.jizhang.core.Perm
import com.buddy.jizhang.core.Period
import com.buddy.jizhang.core.fmt
import com.buddy.jizhang.core.periodRange
import com.buddy.jizhang.data.Dao
import com.buddy.jizhang.data.Prefs
import com.buddy.jizhang.service.WatchService
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun MeScreen(tick: Int, onTick: () -> Unit) {
    var page by rememberSaveable { mutableStateOf("me") }
    when (page) {
        "me" -> MeMenu(tick) { page = it }
        "settings" -> SettingsScreen(onBack = { page = "me" }, onTick = onTick)
        "history" -> HistoryScreen(onBack = { page = "me" })
    }
}

@Composable
private fun MeMenu(tick: Int, go: (String) -> Unit) {
    val ctx = LocalContext.current
    val saved = remember(tick) { Dao.balance(ctx) }
    val month = remember(tick) { periodRange(Period.MONTH) }
    val monthTx = remember(tick) { Dao.all(ctx, month.first, month.second) }
    val byAccount = remember(tick) {
        monthTx.filter { it.kind == "expense" }.groupBy { it.account }
            .map { it.key to it.value.sumOf { v -> v.amount } }.sortedByDescending { it.second }
    }
    val streak = remember(tick) {
        val days = Dao.recent(ctx, 500).map {
            Instant.ofEpochMilli(it.ts).atZone(ZoneId.systemDefault()).toLocalDate()
        }.toSet()
        var s = 0
        var d = LocalDate.now()
        while (d in days) { s++; d = d.minusDays(1) }
        s
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            SectionCard("记账buddy") {
                Text("累计结余 ¥${fmt(saved)}", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    color = if (saved >= 0) OkGreen else OverRed)
                KV("攒钱目标", "「${Prefs.goalName}」还差 ¥${fmt((Prefs.goalAmount - saved).coerceAtLeast(0.0))}")
                KV("连续记账", "$streak 天")
                KV("成就徽章", buildString {
                    if (streak >= 7) append("坚持一周 ")
                    if (streak >= 28) append("坚持一月 ")
                    if (saved >= 500) append("攒下五百 ")
                    if (saved >= Prefs.goalAmount && Prefs.goalAmount > 0) append("目标达成")
                    if (isEmpty()) append("记几笔就能点亮")
                })
            }
        }
        item {
            SectionCard("本月各账户") {
                if (byAccount.isEmpty()) Text("本月还没有支出", fontSize = 13.sp, color = TextGray)
                else byAccount.forEach { (a, v) -> KV(a, "¥${fmt(v)}") }
            }
        }
        item {
            SectionCard("理财提醒记录（每周六 ${Prefs.financeTime}）") {
                Text(
                    Prefs.financeLog.ifBlank { "还没有发送过提醒，等待最近的周六" },
                    fontSize = 12.sp, color = TextGray, lineHeight = 19.sp
                )
            }
        }
        item {
            SectionCard("更多") {
                MenuRow("历史总结", "保存过的日/周/月报告") { go("history") }
                MenuRow("设置", "限额、品类、悬浮窗、提醒、数据") { go("settings") }
            }
        }
        item { Spacer(Modifier.height(90.dp)) }
    }
}

@Composable
private fun MenuRow(title: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.fillMaxWidth(0.8f)) {
            Text(title, fontSize = 14.5.sp, color = TextDark, fontWeight = FontWeight.Medium)
            Text(sub, fontSize = 11.5.sp, color = TextGray)
        }
        Text("›", fontSize = 18.sp, color = Color(0xFFB6BAC4))
    }
}

@Composable
private fun HistoryScreen(onBack: () -> Unit) {
    val entries = remember { Prefs.historyEntries() }
    Column(Modifier.fillMaxSize().background(Color(0xFFF4F6FA))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text("返回") }
            Text("历史总结", fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 14.dp))
        }
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            if (entries.isEmpty()) {
                item {
                    SectionCard("暂无历史") {
                        Text("在统计页生成报告后点「存入历史总结」，或等每周日 ${Prefs.weeklyTime} 的自动周报。", fontSize = 12.sp, color = TextGray)
                    }
                }
            } else {
                entries.forEach { (title, text) ->
                    item {
                        SectionCard(title) {
                            Text(text, fontSize = 12.5.sp, color = TextDark, lineHeight = 20.sp)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(60.dp)) }
        }
    }
}

@Composable
private fun SettingsScreen(onBack: () -> Unit, onTick: () -> Unit) {
    val ctx = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    var newCat by rememberSaveable { mutableStateOf("") }
    var newAcc by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6FA))
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text("返回") }
            Text("设置", fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 14.dp))
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp)
        ) {
            SectionCard("限额") {
                NumField("日限额（元，0 为不设）", Prefs.dayLimit) { Prefs.dayLimit = it }
                NumField("周限额（元）", Prefs.weekLimit) { Prefs.weekLimit = it }
                NumField("月限额（元）", Prefs.monthLimit) { Prefs.monthLimit = it }
                NumField("年限额（元）", Prefs.yearLimit) { Prefs.yearLimit = it }
                Spacer(Modifier.height(8.dp))
                Text("各品类月限额（0 为不设）", fontSize = 12.sp, color = TextGray)
                Prefs.categories.forEach { cat ->
                    NumField("$cat", Prefs.catLimit(cat)) { Prefs.setCatLimit(cat, it) }
                }
                NumField("预警阈值（%，默认 80）", Prefs.warnPct.toDouble()) { Prefs.warnPct = it.toInt() }
                NumField("大额阈值（元，默认 50）", Prefs.largeThreshold) { Prefs.largeThreshold = it }
            }

            SectionCard("品类管理") {
                Prefs.categories.forEach { cat ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(cat, fontSize = 13.5.sp, color = TextDark, modifier = Modifier.fillMaxWidth(0.7f))
                        TextButton(onClick = {
                            Prefs.categories = Prefs.categories - cat
                            onTick()
                        }) { Text("删除", color = OverRed) }
                    }
                }
                OutlinedTextField(
                    value = newCat,
                    onValueChange = { newCat = it.take(8) },
                    label = { Text("新增品类名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        val n = newCat.trim()
                        if (n.isNotBlank() && n !in Prefs.categories) {
                            Prefs.categories = Prefs.categories + n
                            newCat = ""
                            onTick()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                ) { Text("添加品类") }
            }

            SectionCard("账户管理") {
                Prefs.accounts.forEach { acc ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(acc, fontSize = 13.5.sp, color = TextDark, modifier = Modifier.fillMaxWidth(0.7f))
                        TextButton(onClick = {
                            if (Prefs.accounts.size > 1) {
                                Prefs.accounts = Prefs.accounts - acc
                                onTick()
                            }
                        }) { Text("删除", color = OverRed) }
                    }
                }
                OutlinedTextField(
                    value = newAcc,
                    onValueChange = { newAcc = it.take(8) },
                    label = { Text("新增账户名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        val n = newAcc.trim()
                        if (n.isNotBlank() && n !in Prefs.accounts) {
                            Prefs.accounts = Prefs.accounts + n
                            newAcc = ""
                            onTick()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                ) { Text("添加账户") }
            }

            SectionCard("生活费与攒钱目标") {
                NumField("每月生活费（元）", Prefs.allowance) { Prefs.allowance = it }
                TextField1("目标名称", Prefs.goalName) { Prefs.goalName = it }
                NumField("目标金额（元）", Prefs.goalAmount) { Prefs.goalAmount = it }
            }

            SectionCard("学期") {
                TextField1("学期开始（如 2026-09-01）", Prefs.semesterStart) { Prefs.semesterStart = it }
                TextField1("学期结束（如 2027-01-15）", Prefs.semesterEnd) { Prefs.semesterEnd = it }
            }

            SectionCard("提醒时间（HH:mm）") {
                TextField1("每日总结", Prefs.dailyTime) { Prefs.dailyTime = it }
                TextField1("周报（周日）", Prefs.weeklyTime) { Prefs.weeklyTime = it }
                TextField1("理财提醒（周六）", Prefs.financeTime) { Prefs.financeTime = it }
            }

            SectionCard("悬浮窗") {
                TextField1("提醒文案", Prefs.overlayText) { Prefs.overlayText = it }
                Text(
                    "可用变量：{已用} {剩余} {目标名} {还差} {N个月}",
                    fontSize = 11.sp, color = TextGray
                )
                Spacer(Modifier.height(10.dp))
                Text("监控的应用", fontSize = 12.sp, color = TextGray)
                Prefs.candidateMonitored.forEach { (pkg, name) ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = pkg in Prefs.monitored,
                            onCheckedChange = {
                                Prefs.monitored =
                                    if (it) Prefs.monitored + pkg
                                    else Prefs.monitored - pkg
                                onTick()
                            }
                        )
                        Text(name, fontSize = 13.5.sp, color = TextDark)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    OutlinedButton(onClick = {
                        val ok = WatchService.showPreview(10)
                        if (!ok) Toast.makeText(ctx, "请先开启无障碍服务", Toast.LENGTH_SHORT).show()
                    }) { Text("预览 10 秒") }
                    Spacer(Modifier.width2())
                    OutlinedButton(onClick = {
                        runCatching { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                    }) { Text("无障碍设置") }
                }
                Spacer(Modifier.height(10.dp))
                Text("最近触发记录", fontSize = 12.sp, color = TextGray)
                Text(
                    Prefs.triggerLog.ifBlank { "暂无。打开被监控的应用试试。" },
                    fontSize = 11.5.sp, color = TextDark, lineHeight = 18.sp
                )
                TextButton(onClick = { Prefs.clearTriggerLog(); onTick() }) { Text("清空记录", color = TextGray) }
            }

            SectionCard("权限自检") {
                val accOn = Perm.accessibilityOn(ctx)
                KV("无障碍服务（悬浮窗监控）", if (accOn) "已开启" else "未开启", if (accOn) OkGreen else OverRed)
                KV("通知权限", if (Perm.notificationsOn(ctx)) "已开启" else "未开启", if (Perm.notificationsOn(ctx)) OkGreen else OverRed)
                KV("悬浮窗备用权限", if (Perm.overlayOn(ctx)) "已开启" else "未开启（无障碍方式不需要）",
                    if (Perm.overlayOn(ctx)) OkGreen else TextGray)
                Spacer(Modifier.height(8.dp))
                Text(
                    "ColorOS 提示：给本应用「自启动」并锁定后台，否则定时提醒和悬浮窗可能被系统清理。",
                    fontSize = 11.5.sp, color = TextGray
                )
            }

            SectionCard("AI（可选）") {
                TextField1("DeepSeek API Key（v1 暂存，后续版本启用）", Prefs.apiKey) { Prefs.apiKey = it }
            }

            SectionCard("数据") {
                Button(
                    onClick = { Export.share(ctx, Period.YEAR, allTime = true) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("备份导出全部数据") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { confirmClear = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("清空全部记录", color = OverRed) }
            }

            SectionCard("关于") {
                KV("版本", "1.0.0 (v1)")
                KV("数据存储", "全部在本机，不上传")
            }
            Spacer(Modifier.height(60.dp))
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空全部记录？") },
            text = { Text("所有账目和报告都会删除，无法恢复。建议先「备份导出全部数据」。") },
            confirmButton = {
                TextButton(onClick = {
                    Dao.clearAll(ctx)
                    confirmClear = false
                    onTick()
                    Toast.makeText(ctx, "已清空", Toast.LENGTH_SHORT).show()
                }) { Text("确定清空", color = OverRed) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun SpacerWidth() = Spacer(Modifier.padding(0.dp))

private fun Modifier.width2(): Modifier = this.padding(6.dp)

@Composable
private fun NumField(label: String, value: Double, onChange: (Double) -> Unit) {
    var text by rememberSaveable {
        mutableStateOf(if (value <= 0.0) "" else if (value % 1.0 == 0.0) value.toLong().toString() else value.toString())
    }
    OutlinedTextField(
        value = text,
        onValueChange = { s ->
            val filtered = s.filter { it.isDigit() || it == '.' }.take(10)
            text = filtered
            filtered.toDoubleOrNull()?.let(onChange)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    )
}

@Composable
private fun TextField1(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.take(60)) },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    )
}
