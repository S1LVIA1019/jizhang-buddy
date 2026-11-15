package com.buddy.jizhang.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.buddy.jizhang.core.LimitNotifier
import com.buddy.jizhang.core.Piggy
import com.buddy.jizhang.core.Tags
import com.buddy.jizhang.core.fmt
import com.buddy.jizhang.data.Dao
import com.buddy.jizhang.data.Prefs
import com.buddy.jizhang.data.Tx
import com.buddy.jizhang.ocr.ImportActivity
import com.buddy.jizhang.widget.WidgetUpdater

@Composable
fun AddScreen(onSaved: () -> Unit) {
    val ctx = LocalContext.current
    var kind by rememberSaveable { mutableStateOf("expense") }
    var amount by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(Prefs.categories.firstOrNull() ?: "其他") }
    var account by rememberSaveable { mutableStateOf(Prefs.accounts.firstOrNull() ?: "现金") }
    var memo by rememberSaveable { mutableStateOf("") }
    var usePiggy by rememberSaveable { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    var bal by remember { mutableStateOf(Piggy.balance(ctx)) }
    var pending by remember { mutableStateOf(0.0) }
    var askOverdraft by remember { mutableStateOf(false) }

    val tag = Tags.tag(memo)

    fun doSave(a: Double) {
        val k = if (usePiggy) "piggy" else kind
        Dao.insert(
            ctx,
            Tx(
                ts = System.currentTimeMillis(),
                amount = a,
                kind = k,
                category = category,
                account = if (usePiggy) "攒钱罐" else account,
                memo = memo.trim(),
                tags = Tags.tag(memo) ?: "",
                source = "manual",
                merchant = ""
            )
        )
        if (k == "expense") LimitNotifier.afterExpense(ctx)
        WidgetUpdater.refresh(ctx)
        onSaved()
        amount = ""
        memo = ""
        usePiggy = false
        bal = Piggy.balance(ctx)
        msg = when (k) {
            "piggy" -> "已从攒钱罐扣除 ¥${fmt(a)}，罐余额 ¥${fmt(bal)}"
            "expense" -> "已入账，超限会立刻提醒你"
            else -> "已入账"
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 10.dp)
    ) {
        HeroCard(
            label = when {
                usePiggy -> "从攒钱罐扣除"
                kind == "expense" -> "支出金额"
                else -> "收入金额"
            },
            big = "¥${amount.ifBlank { "0.00" }}",
            note = if (usePiggy) "攒钱罐当前余额 ¥${fmt(bal)}" else "超过 ¥${fmt(Prefs.largeThreshold)} 算大额，会在总结里单列"
        )

        SectionCard("记一笔") {
            Row {
                listOf("支出" to "expense", "收入" to "income").forEach { (label, k) ->
                    val sel = !usePiggy && kind == k
                    androidx.compose.foundation.layout.Box(
                        Modifier
                            .padding(end = 10.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (sel) Blue else ChipBg)
                            .clickable { kind = k; usePiggy = false }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(label, fontSize = 12.5.sp, color = if (sel) Color.White else Color(0xFF5A6072),
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' }.take(10) },
                label = { Text("金额（元）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = usePiggy, onCheckedChange = { usePiggy = it })
                Text("从攒钱罐扣（旅游等临时大额）", fontSize = 12.5.sp, color = TextDark)
            }
            if (usePiggy) {
                Text(
                    "这笔只减攒钱罐余额，不计入当月日常支出，也不会影响月末结算。",
                    fontSize = 11.sp, color = TextGray, lineHeight = 17.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("支出品类", fontSize = 12.sp, color = TextGray)
            Spacer(Modifier.height(8.dp))
            Chips(Prefs.categories, category) { category = it }
            Spacer(Modifier.height(14.dp))
            Text("从哪个账户出", fontSize = 12.sp, color = TextGray)
            Spacer(Modifier.height(8.dp))
            Chips(Prefs.accounts, account) { account = it }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it.take(40) },
                label = { Text("这笔花在哪（如：和同学去商场吃饭）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (memo.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TagPill(category)
                    tag?.split(",")?.filter { it.isNotBlank() }?.forEach { TagPill(it.trim()) }
                }
                Text("备注原文和自动标签都会一起写进导出文件",
                    fontSize = 10.5.sp, color = TextGray, modifier = Modifier.padding(top = 6.dp))
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val a = amount.toDoubleOrNull()
                    if (a == null || a <= 0.0) {
                        msg = "金额没填对"
                        return@Button
                    }
                    if (usePiggy && a > bal) {
                        pending = a
                        askOverdraft = true
                        return@Button
                    }
                    doSave(a)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存这一笔", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
            if (msg.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(msg, fontSize = 11.5.sp, color = TextGray)
            }
        }

        SectionCard("校园卡明细截图导入", container = SoftPurple) {
            Text("离线识别金额、商户、时间 · 自动过滤充值 · 疑似重复只标黄，由你勾选删除",
                fontSize = 11.5.sp, color = Color(0xFF4A3D6E), lineHeight = 18.sp)
            Spacer(Modifier.height(10.dp))
            Button(onClick = {
                runCatching { ctx.startActivity(Intent(ctx, ImportActivity::class.java)) }
                    .onFailure { Toast.makeText(ctx, "无法打开导入页", Toast.LENGTH_SHORT).show() }
            }, modifier = Modifier.fillMaxWidth()) { Text("选择截图导入", fontSize = 13.sp) }
        }

        Spacer(Modifier.height(80.dp))
    }

    if (askOverdraft) {
        AlertDialog(
            onDismissRequest = { askOverdraft = false },
            title = { Text("攒钱罐余额不足") },
            text = {
                Text("罐子只剩 ¥${fmt(bal)}，确定动用 ¥${fmt(pending)}？超出部分会记为透支并标红，下月预算也会相应收紧。")
            },
            confirmButton = {
                TextButton(onClick = {
                    askOverdraft = false
                    doSave(pending)
                }) { Text("确定动用", color = OverRed) }
            },
            dismissButton = {
                TextButton(onClick = { askOverdraft = false }) { Text("取消") }
            }
        )
    }
}
