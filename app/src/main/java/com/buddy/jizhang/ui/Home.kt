package com.buddy.jizhang.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.buddy.jizhang.data.Tx
import com.buddy.jizhang.service.WatchService

@Composable
fun HomeScreen(tick: Int) {
    val ctx = LocalContext.current
    val day = remember(tick) { periodRange(Period.DAY) }
    val todaySpent = remember(tick) { Dao.sum(ctx, day.first, day.second, "expense") }
    val dayTxs = remember(tick) { Dao.all(ctx, day.first, day.second) }
    val snackCount = dayTxs.count { it.tags.contains("零食") }
    val month = remember(tick) { periodRange(Period.MONTH) }
    val monthUsed = remember(tick) { Dao.sum(ctx, month.first, month.second, "expense") }
    val monthLimit = Prefs.monthLimit
    val monthRemain = (monthLimit - monthUsed).coerceAtLeast(0.0)
    val saved = remember(tick) { Dao.balance(ctx) }
    val recent = remember(tick) { Dao.recent(ctx, 3) }
    val accessibilityOn = Perm.accessibilityOn(ctx)

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            HeroCard(
                label = "今天花了",
                big = "¥${fmt(todaySpent)}",
                note = when {
                    snackCount >= 2 -> "今天 $snackCount 笔是零食，少吃点"
                    todaySpent == 0.0 -> "还没记账，点底部「记账」记第一笔"
                    else -> "本月还剩 ¥${fmt(monthRemain)}，今天没超支"
                },
                split = listOf(
                    "本月已花" to "¥${fmt(monthUsed)}",
                    "本月还剩" to "¥${fmt(monthRemain)}"
                )
            )
        }

        item {
            SectionCard("本月预算") {
                if (monthLimit > 0) {
                    val ratio = (monthUsed / monthLimit).toFloat()
                    val c = when {
                        ratio >= 1f -> OverRed
                        ratio >= Prefs.warnPct / 100f -> WarnOrange
                        else -> Blue
                    }
                    Text(
                        "剩余 ¥${fmt(monthRemain)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = c
                    )
                    Spacer(Modifier.height(10.dp))
                    Bar(ratio, c)
                    Spacer(Modifier.height(8.dp))
                    KV("已用", "¥${fmt(monthUsed)} / ¥${fmt(monthLimit)}（${(ratio * 100).toInt()}%）")
                    KV(
                        "预警线",
                        "${Prefs.warnPct}%（¥${fmt(monthLimit * Prefs.warnPct / 100.0)}）"
                    )
                } else {
                    Text("未设限额，去 我的 → 设置 → 限额 设置", fontSize = 12.5.sp, color = TextGray)
                }
            }
        }

        item {
            SectionCard("购物提醒悬浮窗") {
                KV(
                    "无障碍服务",
                    if (accessibilityOn) "已开启" else "未开启",
                    if (accessibilityOn) OkGreen else OverRed
                )
                Spacer(Modifier.height(6.dp))
                val names = Prefs.monitored.map { Prefs.appNames[it] ?: it }
                if (names.isEmpty()) {
                    Text("未选择监控应用", fontSize = 11.5.sp, color = TextGray)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        names.take(5).forEach { TagPill(it) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    Prefs.overlayText,
                    fontSize = 11.5.sp,
                    color = TextDark,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { Perm.openAccessibility(ctx) },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (accessibilityOn) "去检查" else "去开启", fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = {
                            val ok = WatchService.showPreview(10)
                            if (!ok) Toast.makeText(ctx, "请先开启无障碍服务", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("预览 10 秒", fontSize = 12.sp) }
                }
            }
        }

        item {
            SectionCard("攒钱目标「${Prefs.goalName}」") {
                val goalLeft = (Prefs.goalAmount - saved).coerceAtLeast(0.0)
                Text(
                    if (goalLeft <= 0) "已达成！" else "还差 ¥${fmt(goalLeft)}",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = if (goalLeft <= 0) OkGreen else TextDark
                )
                Spacer(Modifier.height(10.dp))
                Bar((saved / Prefs.goalAmount).toFloat().coerceIn(0f, 1f), Blue)
                Spacer(Modifier.height(8.dp))
                KV("累计结余", "¥${fmt(saved)} / ¥${fmt(Prefs.goalAmount)}")
            }
        }

        item {
            SectionCard("最近 3 笔") {
                if (recent.isEmpty()) {
                    Text("还没有记录，点底部「记账」记第一笔", fontSize = 12.5.sp, color = TextGray)
                } else {
                    recent.forEach { tx ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SquareMark()
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tx.memo.ifBlank { tx.category }, fontSize = 13.sp, color = TextDark,
                                    fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${tx.category} · ${tx.account}" +
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
                onClick = {
                    Dao.insert(
                        ctx,
                        Tx(
                            ts = System.currentTimeMillis(), amount = Prefs.allowance, kind = "income",
                            category = "生活费", account = "微信", memo = "生活费到账",
                            tags = "", source = "allowance", merchant = ""
                        )
                    )
                    Toast.makeText(ctx, "生活费 ¥${fmt(Prefs.allowance)} 已入账", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            ) { Text("点击自动入账生活费 ¥${fmt(Prefs.allowance)}", fontSize = 13.sp) }
        }

        if (!accessibilityOn) {
            item {
                SectionCard("无障碍服务未开启", container = SoftRed) {
                    Text(
                        "不开启购物悬浮窗不会弹出。系统里搜「记账buddy · 购物提醒」即可找到。",
                        fontSize = 11.5.sp,
                        color = OverRed,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { Perm.openAccessibility(ctx) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("去开启", fontSize = 12.sp) }
                }
            }
        }

        item { Spacer(Modifier.height(90.dp)) }
    }
}
