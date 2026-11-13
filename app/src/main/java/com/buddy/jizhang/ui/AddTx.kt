package com.buddy.jizhang.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buddy.jizhang.core.LimitNotifier
import com.buddy.jizhang.core.Tags
import com.buddy.jizhang.data.Dao
import com.buddy.jizhang.data.Prefs
import com.buddy.jizhang.data.Tx
import com.buddy.jizhang.ocr.ImportActivity

@Composable
fun AddScreen(onSaved: () -> Unit) {
    val ctx = LocalContext.current
    var kind by rememberSaveable { mutableStateOf("expense") }
    var amount by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(Prefs.categories.firstOrNull() ?: "其他") }
    var account by rememberSaveable { mutableStateOf(Prefs.accounts.firstOrNull() ?: "现金") }
    var memo by rememberSaveable { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }

    val tag = Tags.tag(memo)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp)
    ) {
        SectionCard("记一笔") {
            Row {
                listOf("支出" to "expense", "收入" to "income").forEach { (label, k) ->
                    val sel = kind == k
                    androidx.compose.foundation.layout.Box(
                        Modifier
                            .padding(end = 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) Blue else Color(0xFFEEF1F6))
                            .clickable { kind = k }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(label, fontSize = 13.sp, color = if (sel) Color.White else Color(0xFF374151))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' }.take(10) },
                label = { Text(if (kind == "expense") "支出金额" else "收入金额") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            Text("品类", fontSize = 12.sp, color = TextGray)
            Spacer(Modifier.height(8.dp))
            Chips(Prefs.categories, category) { category = it }
            Spacer(Modifier.height(14.dp))
            Text("账户", fontSize = 12.sp, color = TextGray)
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
            if (memo.isNotBlank() && tag != null) {
                Spacer(Modifier.height(6.dp))
                Text("自动标签：$tag", fontSize = 12.sp, color = BlueLight)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val a = amount.toDoubleOrNull()
                    if (a == null || a <= 0.0) {
                        msg = "金额没填对"
                        return@Button
                    }
                    val nowTag = Tags.tag(memo) ?: ""
                    Dao.insert(
                        ctx,
                        Tx(
                            ts = System.currentTimeMillis(),
                            amount = a,
                            kind = kind,
                            category = category,
                            account = account,
                            memo = memo.trim(),
                            tags = nowTag,
                            source = "manual",
                            merchant = ""
                        )
                    )
                    if (kind == "expense") LimitNotifier.afterExpense(ctx)
                    onSaved()
                    amount = ""
                    memo = ""
                    msg = if (kind == "expense") "已入账，超限会立刻提醒你" else "已入账"
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存", fontSize = 15.sp, fontWeight = FontWeight.Medium) }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    runCatching { ctx.startActivity(Intent(ctx, ImportActivity::class.java)) }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("校园卡截图导入", fontSize = 15.sp) }
            if (msg.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(msg, fontSize = 12.sp, color = TextGray)
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}
