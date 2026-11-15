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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buddy.jizhang.core.Budget
import com.buddy.jizhang.core.LifeAssist
import com.buddy.jizhang.core.Perm
import com.buddy.jizhang.core.Period
import com.buddy.jizhang.core.Piggy
import com.buddy.jizhang.core.fmt
import com.buddy.jizhang.core.periodRange
import com.buddy.jizhang.data.Dao
import com.buddy.jizhang.data.Prefs
import com.buddy.jizhang.data.Tx
import com.buddy.jizhang.service.WatchService
import com.buddy.jizhang.widget.WidgetUpdater

@Composable
fun HomeScreen(tick: Int) {
    val ctx = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val day = remember(tick, refresh) { periodRange(Period.DAY) }
    val todaySpent = remember(tick, refresh) { Dao.sum(ctx, day.first, day.second, "expense") }
    val dayTxs = remember(tick, refresh) { Dao.all(ctx, day.first, day.second) }
    val snackCount = dayTxs.count { it.tags.contains("零食") }
    val budget = remember(tick, refresh) { Budget.current(ctx) }
    val piggyBal = remember(tick, refresh) { Piggy.balance(ctx) }
    val piggyUsed = remember(tick, refresh) { Piggy.usedInMonth(ctx) }
    val plans = remember(tick, refresh) { LifeAssist.plans(ctx) }
    val recommend = remember(tick, refresh) { Budget.nextRecommendData() }
    val recent = remember(tick, refresh) { Dao.recent(ctx, 3) }
    val accessibilityOn = Perm.accessibilityOn(ctx)
    var showRecommend by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            HeroCard(
                label = "今天花了",
                big = "¥${fmt(todaySpent)}",
                note = when {
                    snackCount >= 2 -> "今天 $snackCount 笔是零食，少吃点"
                    todaySpent == 0.0 -> "还没记账，点底部「记账」记第一笔"
                    else -> "本月还剩 ¥${fmt(budget.remain.coerceAtLeast(0.0))}，${LifeAssist.status(ctx)}"
                },
                split = listOf(
                    "本月已花" to "¥${fmt(budget.spent)}",
                    "本月还剩" to "¥${fmt(budget.remain.coerceAtLeast(0.0))}"
                )
            )
        }

        item {
            LifeAssistCard(
                ctx = ctx,
                b = budget,
                plans = plans,
                recommend = recommend,
                showRecommend = showRecommend,
                onToggleRecommend = { showRecommend = !showRecommend },
                onChanged = { m: String ->
                    refresh++
                    msg = m
                    WidgetUpdater.refresh(ctx)
                }
            )
        }

        item {
            SectionCard("本月预算") {
                val ratio = if (budget.C > 0) (budget.spent / budget.C).toFloat() else 0f
                val c = when {
                    ratio >= 1f -> OverRed
                    budget.isAhead -> WarnOrange
                    else -> Blue
                }
                Text("可花 ¥${fmt(budget.C)}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = c)
                Spacer(Modifier.height(10.dp))
                Bar(ratio, c)
                Spacer(Modifier.height(8.dp))
                KV("有效生活费", "¥${fmt(budget.L)}" + if (budget.lastOverspend > 0) "（上月超支扣 ¥${fmt(budget.lastOverspend)}）" else "")
                KV("应急缓冲", "¥${fmt(budget.B)}（${fmt(Prefs.bufferPct)}%）")
                KV("计划存下", "¥${fmt(budget.S)}")
                KV("到今天应花", "¥${fmt(budget.shouldSpend)}")
                KV("实际已花", "¥${fmt(budget.spent)}", if (budget.isAhead) OverRed else TextDark)
                KV("今日上限", "¥${fmt(budget.dailyCeil)}（可花额 ÷ ${budget.days} 天）")
                if (Prefs.monthLimit > 0) KV("本月支出上限", "¥${fmt(Prefs.monthLimit)}")
            }
        }

        item {
            SectionCard("攒钱罐「${Prefs.goalName}」", container = SoftPurple) {
                Text(
                    "¥${fmt(piggyBal)}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (piggyBal < 0) OverRed else HeroInk
                )
                Text(
                    if (piggyBal < 0) "已透支，下月会少花一点补回来" else "只累计已完结月份的结余，月末更新一次",
                    fontSize = 11.sp, color = TextGray
                )
                Spacer(Modifier.height(10.dp))
                Bar(
                    (piggyBal / Prefs.goalAmount.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f),
                    if (piggyBal < 0) OverRed else Blue
                )
                Spacer(Modifier.height(8.dp))
                KV("目标", "¥${fmt(Prefs.goalAmount)}（达成后继续累加）")
                KV("还差", Piggy.goalLeftText(ctx))
                KV("本月动用", "¥${fmt(piggyUsed)}")
                KV("本月预计能存", "¥${fmt(Piggy.expectedBalance(ctx))}")
                Text(
                    "临时和同学旅游等，在「记账」勾选攒钱罐即可扣除；这笔不计入当月日常支出。",
                    fontSize = 11.sp, color = TextGray, lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        item {
            SectionCard("购物提醒悬浮窗") {
                KV("无障碍服务", if (accessibilityOn) "已开启" else "未开启", if (accessibilityOn) OkGreen else OverRed)
                Spacer(Modifier.height(6.dp))
                val names = Prefs.monitored.map { Prefs.appNames[it] ?: it }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    names.take(5).forEach { TagPill(it) }
                }
                Spacer(Modifier.height(10.dp))
                Text(Prefs.overlayText, fontSize = 11.5.sp, color = TextDark, lineHeight = 18.sp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { Perm.openAccessibility(ctx) }, modifier = Modifier.weight(1f)) {
                        Text(if (accessibilityOn) "去检查" else "去开启", fontSize = 12.sp)
                    }
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
            SectionCard("最近 3 笔") {
                if (recent.isEmpty()) {
                    Text("还没有记录，点底部「记账」记第一笔", fontSize = 12.5.sp, color = TextGray)
                } else {
                    recent.forEach { tx ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SquareMark(if (tx.kind == "piggy") OverRed else Blue)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tx.memo.ifBlank { tx.category }, fontSize = 13.sp, color = TextDark,
                                    fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${if (tx.kind == "piggy") "攒钱罐 · " else ""}${tx.category} · ${tx.account}" +
                                        if (tx.tags.isNotBlank()) " · ${tx.tags}" else "",
                                    fontSize = 10.5.sp, color = TextGray
                                )
                            }
                            Text(
                                (if (tx.kind == "income") "+¥" else "-¥") + fmt(tx.amount),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (tx.kind == "income") OkGreen else TextDark
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
                    refresh++
                    WidgetUpdater.refresh(ctx)
                    Toast.makeText(ctx, "生活费 ¥${fmt(Prefs.allowance)} 已入账", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            ) { Text("点击自动入账生活费 ¥${fmt(Prefs.allowance)}", fontSize = 13.sp) }
            if (msg.isNotEmpty()) {
                Text(msg, fontSize = 11.5.sp, color = OkGreen,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
            }
        }

        if (!Prefs.widgetGuideShown) {
            item {
                SectionCard("把进度放上桌面", container = SoftPurple) {
                    Text("长按桌面空白处 → 「插件 / 卡片」→ 找到「记账buddy」→ 拖到桌面。",
                        fontSize = 12.sp, color = TextDark, lineHeight = 19.sp)
                    Text("想关掉时长按卡片拖走即可，App 里不需要开关。",
                        fontSize = 11.5.sp, color = TextGray, lineHeight = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { Prefs.widgetGuideShown = true; refresh++ }) {
                        Text("知道了", fontSize = 12.sp)
                    }
                }
            }
        }

        if (!accessibilityOn) {
            item {
                SectionCard("无障碍服务未开启", container = SoftRed) {
                    Text("不开启购物悬浮窗不会弹出。系统里搜「记账buddy · 购物提醒」即可找到。",
                        fontSize = 11.5.sp, color = OverRed, lineHeight = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { Perm.openAccessibility(ctx) }, modifier = Modifier.fillMaxWidth()) {
                        Text("去开启", fontSize = 12.sp)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(90.dp)) }
    }
}

@Composable
private fun LifeAssistCard(
    ctx: android.content.Context,
    b: com.buddy.jizhang.core.BudgetInfo,
    plans: List<com.buddy.jizhang.core.PlanOption>,
    recommend: Pair<Double, Map<String, Double>>,
    showRecommend: Boolean,
    onToggleRecommend: () -> Unit,
    onChanged: (String) -> Unit
) {
    val pct = LifeAssist.progressPct(ctx)
    SectionCard("生活助手", container = HeroBg) {
        Text(
            if (b.isAhead) "本月超前 ¥${fmt(b.ahead)}" else "本月进度 $pct%",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = if (b.isAhead) OverRed else HeroInk
        )
        Spacer(Modifier.height(4.dp))
        Text(LifeAssist.status(ctx), fontSize = 11.5.sp,
            color = HeroInk.copy(alpha = 0.78f), lineHeight = 18.sp)
        Spacer(Modifier.height(10.dp))
        Bar(
            (b.spent / b.shouldSpend.coerceAtLeast(0.01)).toFloat(),
            if (b.isAhead) OverRed else Blue
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "接下来 ${b.daysLeft} 天每天最多 ¥${fmt(b.perDayLeft.coerceAtLeast(0.0))}",
            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HeroInk
        )
        Text(LifeAssist.note(), fontSize = 10.5.sp, color = TextGray)

        Spacer(Modifier.height(14.dp))
        Text("这 ¥${fmt(b.remain.coerceAtLeast(0.0))} 可以怎么用", fontSize = 12.sp,
            fontWeight = FontWeight.Bold, color = HeroInk)
        Spacer(Modifier.height(6.dp))
        plans.forEach { p ->
            val c = when {
                !p.enabled -> TextGray
                p.recommended -> Blue
                else -> TextDark
            }
            Text(
                (if (p.recommended) "★ " else "· ") + p.title + "：" + p.detail,
                fontSize = 11.5.sp, color = c, lineHeight = 18.sp,
                modifier = Modifier.padding(vertical = 2.dp)
            )
            Text("    到月末预计结余 ¥${fmt(p.monthEndBalance)}", fontSize = 10.5.sp, color = TextGray)
        }
        if (LifeAssist.treatHeavy(ctx)) {
            Text("本月奶茶 / 外食偏多，「放松」方案已划掉。", fontSize = 11.sp,
                color = OverRed, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(LifeAssist.transferAdvice(ctx), fontSize = 11.sp, color = TextGray, lineHeight = 17.sp)

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onToggleRecommend, modifier = Modifier.fillMaxWidth()) {
            Text(if (showRecommend) "收起推荐限额" else "查看 / 应用推荐限额", fontSize = 12.sp)
        }
        if (showRecommend) {
            Spacer(Modifier.height(8.dp))
            if (Budget.paramsChanged()) {
                Text("配置改过了，下面的推荐是按旧参数生成的。", fontSize = 11.sp, color = OverRed)
                OutlinedButton(onClick = {
                    Budget.forceRefreshNextRecommend(ctx)
                    onChanged("已按最新参数重新生成推荐")
                }, modifier = Modifier.fillMaxWidth()) { Text("按新参数重新生成", fontSize = 12.sp) }
                Spacer(Modifier.height(6.dp))
            }
            Text("推荐总支出上限 ¥${fmt(recommend.first)}", fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold, color = HeroInk)
            recommend.second.forEach { (cat, v) ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$cat ¥${fmt(v)}", fontSize = 11.5.sp, color = TextDark,
                        modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = {
                        Prefs.backupLimits()
                        Prefs.setCatLimit(cat, v)
                        onChanged("$cat 限额已改为 ¥${fmt(v)}")
                    }) { Text("应用此值", fontSize = 10.5.sp) }
                }
            }
            Spacer(Modifier.height(6.dp))
            Button(onClick = {
                Budget.applyAllRecommendedNow(ctx)
                onChanged("本月品类限额已全部改为推荐值")
            }, modifier = Modifier.fillMaxWidth()) { Text("一键全部改为推荐限额", fontSize = 12.sp) }
            Row(Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = {
                    Budget.applyNextMonth(ctx)
                    onChanged("下月限额已设置为推荐值，跨月自动生效")
                }, modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                    Text("一键设置下月限额", fontSize = 11.sp)
                }
                OutlinedButton(onClick = {
                    Budget.restore(ctx)
                    onChanged("已恢复我原来的限额")
                }, modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                    Text("恢复原限额", fontSize = 11.sp)
                }
            }
            Text("改动只对本月生效，跨月自动回默认；「恢复」可还原。",
                fontSize = 10.5.sp, color = TextGray, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
