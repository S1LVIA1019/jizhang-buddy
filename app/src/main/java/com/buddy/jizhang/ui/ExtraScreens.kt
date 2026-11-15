package com.buddy.jizhang.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buddy.jizhang.core.Piggy
import com.buddy.jizhang.core.fmt
import com.buddy.jizhang.data.Dao
import com.buddy.jizhang.data.Prefs
import com.buddy.jizhang.update.UpdateManager
import java.time.LocalDate

@Composable
private fun Page(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(PageBg)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack) { Text("返回", fontSize = 12.sp) }
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark,
                modifier = Modifier.padding(start = 14.dp))
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 50.dp)
        ) { content() }
    }
}

@Composable
private fun NumEdit(label: String, value: Double, onChange: (Double) -> Unit) {
    var text by remember { mutableStateOf(if (value <= 0.0) "" else fmt(value)) }
    OutlinedTextField(
        value = text,
        onValueChange = { s ->
            val f = s.filter { it.isDigit() || it == '.' }.take(10)
            text = f
            f.toDoubleOrNull()?.let(onChange)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    )
}

@Composable
private fun TextEdit(label: String, value: String, onChange: (String) -> Unit) {
    var text by remember { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it.take(60); onChange(text) },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    )
}

@Composable
fun GoalsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    var newName by remember { mutableStateOf("") }
    var newAmount by remember { mutableStateOf("") }
    val bal = remember(refresh) { Piggy.balance(ctx) }
    val queue = remember(refresh) { Prefs.queuedGoals() }
    val archive = remember(refresh) { Prefs.goalArchive }
    Page("攒钱罐与目标", onBack) {
        HeroCard(
            label = "攒钱罐余额",
            big = "¥${fmt(bal)}",
            note = "只累计已完结月份的结余，再减去累计动用；可为负"
        )
        SectionCard("当前目标（改名 / 改金额不清空已攒的钱）") {
            TextEdit("目标名称", Prefs.goalName) { Prefs.goalName = it; refresh++ }
            NumEdit("目标金额（元）", Prefs.goalAmount) { Prefs.goalAmount = it; refresh++ }
            KV("还差", Piggy.goalLeftText(ctx))
            Text(
                if (Piggy.reached(ctx)) "已达成，金额继续累加，可在下面切换到排队目标" else "达成后会发通知并点亮徽章，但不封顶",
                fontSize = 11.sp, color = TextGray, lineHeight = 17.sp
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                if (Piggy.reached(ctx)) {
                    Prefs.archiveGoal(Prefs.goalName, Prefs.goalAmount, LocalDate.now().toString())
                    Toast.makeText(ctx, "已归档「${Prefs.goalName}」", Toast.LENGTH_SHORT).show()
                    refresh++
                } else {
                    Toast.makeText(ctx, "还没达成，先攒够再归档", Toast.LENGTH_SHORT).show()
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("归档当前已达成目标", fontSize = 12.sp) }
        }

        SectionCard("排队中的目标") {
            if (queue.isEmpty()) Text("还没有排队目标", fontSize = 12.sp, color = TextGray)
            queue.forEach { g ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${g.first} ¥${fmt(g.second)}", fontSize = 13.sp, color = TextDark,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        if (Piggy.reached(ctx)) {
                            Prefs.archiveGoal(Prefs.goalName, Prefs.goalAmount, LocalDate.now().toString())
                        }
                        Prefs.setQueuedGoals(Prefs.queuedGoals().filter { it.first != g.first })
                        Prefs.goalName = g.first
                        Prefs.goalAmount = g.second
                        refresh++
                    }) { Text("设为主目标", fontSize = 11.sp) }
                    TextButton(onClick = {
                        Prefs.setQueuedGoals(Prefs.queuedGoals().filter { it.first != g.first })
                        refresh++
                    }) { Text("删除", fontSize = 11.sp, color = OverRed) }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = newName, onValueChange = { newName = it.take(12) },
                label = { Text("新目标名，如 旅游+买衣服") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = newAmount, onValueChange = { newAmount = it.filter { c -> c.isDigit() || c == '.' }.take(8) },
                label = { Text("目标金额（元）") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                val a = newAmount.toDoubleOrNull()
                if (newName.isBlank() || a == null || a <= 0) {
                    Toast.makeText(ctx, "名称和金额都要填", Toast.LENGTH_SHORT).show()
                } else {
                    Prefs.setQueuedGoals(Prefs.queuedGoals() + (newName.trim() to a))
                    newName = ""; newAmount = ""
                    refresh++
                }
            }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text("增添目标", fontSize = 12.sp)
            }
        }

        if (archive.isNotBlank()) {
            SectionCard("已达成归档") {
                archive.split("\n").filter { it.isNotBlank() }.take(10).forEach {
                    Text(it.replace("~", " · "), fontSize = 11.5.sp, color = TextGray,
                        modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
fun UpdateScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var state by remember { mutableStateOf("") }
    var info by remember { mutableStateOf<UpdateManager.Info?>(null) }
    var busy by remember { mutableStateOf(false) }
    var auto by remember { mutableStateOf(Prefs.updateCheckOn) }

    Page("检查更新", onBack) {
        HeroCard(
            label = "当前版本",
            big = "1.2.0 · build ${UpdateManager.currentVersion()}",
            note = "同签名覆盖安装，账目 / 设置 / 历史总结全部保留"
        )
        SectionCard("更新") {
            Text(state.ifBlank { "点下面的按钮检查 GitHub 上是否有新版本。" },
                fontSize = 12.5.sp, color = TextDark, lineHeight = 19.sp)
            Spacer(Modifier.height(10.dp))
            Button(onClick = {
                busy = true
                state = "正在检查…"
                UpdateManager.check(ctx) { r ->
                    busy = false
                    info = r
                    state = if (r == null) "已是最新版本（或网络暂不可用）"
                    else "发现新版本 build ${r.build}，点下面下载并安装。"
                }
            }, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                Text("检查更新", fontSize = 13.sp)
            }
            if (info != null) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    val i = info ?: return@Button
                    busy = true
                    state = "正在下载 APK…"
                    UpdateManager.download(ctx, i) { f ->
                        busy = false
                        if (f == null) {
                            state = "下载失败，点下面用浏览器手动下载"
                        } else {
                            val backup = UpdateManager.backup(ctx)
                            state = "已下载，更新前备份：" + (backup?.absolutePath ?: "备份失败，可稍后手动导出")
                            runCatching { UpdateManager.install(ctx, f) }
                                .onFailure { state = "无法调起安装界面，请手动打开下载的 APK" }
                        }
                    }
                }, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                    Text("下载并安装（覆盖安装，数据不丢）", fontSize = 12.5.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                runCatching {
                    ctx.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(UpdateManager.manualUrl()))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("用浏览器打开下载页", fontSize = 12.sp) }
        }

        SectionCard("备份与说明") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = auto, onCheckedChange = { auto = it; Prefs.updateCheckOn = it })
                Text("允许 App 检查更新", fontSize = 12.5.sp, color = TextDark)
            }
            Text("每次安装前会自动导出一份完整 JSON 备份到：",
                fontSize = 11.sp, color = TextGray)
            Text("Android/data/com.buddy.jizhang/files/backup/",
                fontSize = 10.5.sp, color = TextGray)
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = {
                val f = UpdateManager.backup(ctx)
                Toast.makeText(ctx, if (f != null) "备份已生成" else "备份失败", Toast.LENGTH_SHORT).show()
            }, modifier = Modifier.fillMaxWidth()) { Text("立即备份一次", fontSize = 12.sp) }
            Spacer(Modifier.height(8.dp))
            Text("首次安装或更新时，系统可能要求「允许安装未知来源 / 允许安装未知应用」，允许一次即可。",
                fontSize = 11.sp, color = TextGray, lineHeight = 17.sp)
        }
    }
}

@Composable
fun SettlementsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val rows = remember { Dao.settlements(ctx) }
    Page("月末结算记录", onBack) {
        HeroCard(label = "攒钱罐余额", big = "¥${fmt(Piggy.balance(ctx))}",
            note = "每月结算一次，关机漏算会在下次打开时自动补上")
        SectionCard("每月快照") {
            if (rows.isEmpty()) {
                Text("还没有已完结的月份。第一个月末会自动结算。",
                    fontSize = 12.sp, color = TextGray, lineHeight = 18.sp)
            } else {
                rows.forEach { s ->
                    Text(s.ym, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark,
                        modifier = Modifier.padding(top = 8.dp))
                    KV("收入 / 支出", "¥${fmt(s.income)} / ¥${fmt(s.expense)}")
                    KV("当月结余", (if (s.balance >= 0) "+" else "") + "¥${fmt(s.balance)}",
                        if (s.balance >= 0) OkGreen else OverRed)
                    KV("结算后罐余额", "¥${fmt(s.piggyAfter)}")
                }
            }
        }
    }
}

@Composable
fun WidgetHelpScreen(onBack: () -> Unit) {
    Page("桌面小组件", onBack) {
        SectionCard("怎么添加") {
            Text("1  在手机桌面空白处长按", fontSize = 12.5.sp, color = TextDark, lineHeight = 22.sp)
            Text("2  点「插件 / 卡片 / 小组件」", fontSize = 12.5.sp, color = TextDark, lineHeight = 22.sp)
            Text("3  找到「记账buddy」的两种尺寸，拖到桌面", fontSize = 12.5.sp, color = TextDark, lineHeight = 22.sp)
            Text("4  想关掉时长按卡片拖走即可", fontSize = 12.5.sp, color = TextDark, lineHeight = 22.sp)
        }
        SectionCard("两种尺寸") {
            Text("小卡 2×1：本月进度 %、圆头进度条、今日 / 日均金额",
                fontSize = 12.sp, color = TextDark, lineHeight = 19.sp)
            Text("大卡 2×2：再加本月剩余、每天最多、攒钱罐还差，右下角「记一笔」直达记账页",
                fontSize = 12.sp, color = TextDark, lineHeight = 19.sp)
        }
        SectionCard("刷新与数据", container = SoftPurple) {
            Text("每记一笔立即刷新，另有 30 分钟兜底与跨零点自动归零。超前时数字变红并显示超前金额。",
                fontSize = 12.sp, color = TextDark, lineHeight = 19.sp)
            Spacer(Modifier.height(6.dp))
            Text("读的是手机本地账本，和 App 内完全一致，不联网、不上传。不需要单独的桌面小组件权限；只要给 App 自启动并关闭电池优化，卡片就会一直更新。",
                fontSize = 11.5.sp, color = TextGray, lineHeight = 18.sp)
        }
    }
}
