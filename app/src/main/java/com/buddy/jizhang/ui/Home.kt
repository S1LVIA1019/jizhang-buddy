package com.buddy.jizhang.ui

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buddy.jizhang.core.Period
import com.buddy.jizhang.core.Perm
import com.buddy.jizhang.core.fmt
import com.buddy.jizhang.core.periodRange
import com.buddy.jizhang.data.Dao
import com.buddy.jizhang.data.Prefs

@Composable
fun HomeScreen(tick: Int) {
    val ctx = LocalContext.current
    val day = remember(tick) { periodRange(Period.DAY) }
    val todaySpent = remember(tick) { Dao.sum(ctx, day.first, day.second, "expense") }
    val todayCount = remember(tick) { Dao.expenseCount(ctx, day.first, day.second) }
    val month = remember(tick) { periodRange(Period.MONTH) }
    val monthUsed = remember(tick) { Dao.sum(ctx, month.first, month.second, "expense") }
    val monthLimit = Prefs.monthLimit
    val saved = remember(tick) { Dao.balance(ctx) }
    val recent = remember(tick) { Dao.recent(ctx, 3) }
    val accessibilityOn = Perm.accessibilityOn(ctx)

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            SectionCard("今日") {
                Row(Modifier.padding(vertical = 2.dp)) {
                    Text("¥", fontSize = 20.sp, color = TextDark, fontWeight = FontWeight.Bold)
                    Text(fmt(todaySpent), fontSize = 34.sp, color = TextDark, fontWeight = FontWeight.Bold)
                }
                KV("笔数", todayCount.toString())
                if (Prefs.dayLimit > 0) {
                    KV("今日限额剩余", "¥" + fmt((Prefs.dayLimit - todaySpent).coerceAtLeast(0.0)))
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = {
                    val now = System.currentTimeMillis()
                    Dao.insert(
                        ctx,
                        com.buddy.jizhang.data.Tx(
                            ts = now, amount = Prefs.allowance, kind = "income",
                            category = "生活费", account = "微信", memo = "生活费到账",
                            tags = "", source = "allowance", merchant = ""
                        )
                    )
                    Toast.makeText(ctx, "生活费 ¥${fmt(Prefs.allowance)} 已入账", Toast.LENGTH_SHORT).show()
                }) { Text("生活费到账 ¥${fmt(Prefs.allowance)}") }
            }
        }

        item {
            SectionCard("本月预算") {
                if (monthLimit > 0) {
                    val remain = (monthLimit - monthUsed).coerceAtLeast(0.0)
                    val ratio = (monthUsed / monthLimit).toFloat()
                    Text(
                        "剩余 ¥${fmt(remain)}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            ratio >= 1f -> OverRed
                            ratio >= Prefs.warnPct / 100f -> WarnOrange
                            else -> TextDark
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    Bar(ratio, when {
                        ratio >= 1f -> OverRed
                        ratio >= Prefs.warnPct / 100f -> WarnOrange
                        else -> Blue
                    })
                    Spacer(Modifier.height(8.dp))
                    KV("已用", "¥${fmt(monthUsed)} / ¥${fmt(monthLimit)}（${(ratio * 100).toInt()}%）")
                } else {
                    Text("未设限额，去 设置→限额 设置", fontSize = 13.sp, color = TextGray)
                }
            }
        }

        item {
            SectionCard("攒钱目标「${Prefs.goalName}」") {
                val goalLeft = (Prefs.goalAmount - saved).coerceAtLeast(0.0)
                Text(
                    if (goalLeft <= 0) "已达成！" else "还差 ¥${fmt(goalLeft)}",
                    fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    color = if (goalLeft <= 0) OkGreen else TextDark
                )
                Spacer(Modifier.height(10.dp))
                Bar((saved / Prefs.goalAmount).toFloat().coerceIn(0f, 1f), OkGreen)
                Spacer(Modifier.height(8.dp))
                KV("累计结余", "¥${fmt(saved)} / ¥${fmt(Prefs.goalAmount)}")
            }
        }

        item {
            SectionCard("悬浮窗监控") {
                KV("无障碍服务", if (accessibilityOn) "已开启" else "未开启",
                    if (accessibilityOn) OkGreen else OverRed)
                KV("监控应用", Prefs.monitored.joinToString("、").ifBlank { "未选择" })
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = {
                        runCatching {
                            ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    }) { Text(if (accessibilityOn) "去检查" else "去开启") }
                    OutlinedButton(onClick = {
                        val ok = com.buddy.jizhang.service.WatchService.showPreview(10)
                        if (!ok) Toast.makeText(ctx, "请先开启无障碍服务", Toast.LENGTH_SHORT).show()
                    }) { Text("预览 10 秒") }
                }
            }
        }

        item {
            SectionCard("最近 3 笔") {
                if (recent.isEmpty()) {
                    Text("还没有记录，点底部「记账」记第一笔", fontSize = 13.sp, color = TextGray)
                } else {
                    recent.forEach { tx ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    tx.memo.ifBlank { tx.category },
                                    fontSize = 14.sp, color = TextDark
                                )
                                Text(
                                    "${tx.category} · ${tx.account}",
                                    fontSize = 11.sp, color = TextGray
                                )
                            }
                            Text(
                                (if (tx.kind == "expense") "-¥" else "+¥") + fmt(tx.amount),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (tx.kind == "expense") TextDark else OkGreen
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(90.dp))
        }
    }
}
