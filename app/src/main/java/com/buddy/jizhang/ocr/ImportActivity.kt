package com.buddy.jizhang.ocr

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.buddy.jizhang.core.Tags
import com.buddy.jizhang.data.Dao
import com.buddy.jizhang.data.Prefs
import com.buddy.jizhang.data.Tx
import com.buddy.jizhang.ui.Blue
import com.buddy.jizhang.ui.Chips
import com.buddy.jizhang.ui.OverRed
import com.buddy.jizhang.ui.SectionCard
import com.buddy.jizhang.ui.TextDark
import com.buddy.jizhang.ui.TextGray
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions

class ImportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ImportScreen() }
    }
}

data class OcrItem(
    var checked: Boolean,
    var amount: Double,
    var merchant: String,
    var memo: String,
    var dup: Boolean
)

object Parser {
    fun parse(text: String): List<Pair<Double, String>> {
        val out = mutableListOf<Pair<Double, String>>()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line.contains("充值") || line.contains("圈存") || line.contains("余额")) {
                // 消费明细行也可能带余额字样，只有在没有负号金额时才跳过
                if (Regex("-\\d+(?:\\.\\d{1,2})?").find(line) == null) continue
            }
            val nums = Regex("-?\\d+(?:\\.\\d{1,2})?").findAll(line).map { it.value.toDouble() }.toList()
            if (nums.isEmpty()) continue
            val negative = nums.firstOrNull { it < 0 }
            val amount = negative ?: nums.filter { it > 0.5 }.minOrNull() ?: nums.minOrNull() ?: continue
            if (kotlin.math.abs(amount) < 0.01) continue
            val merchant = line
                .replace(Regex("[\\d:：\\-/.\\s]+"), " ")
                .split(" ")
                .filter { it.isNotBlank() && it.length >= 2 }
                .joinToString("")
            out.add(amount to merchant)
        }
        return out.distinctBy { it.first.toString() + it.second }
    }
}

@Composable
fun ImportScreen() {
    val ctx = LocalContext.current
    var items by remember { mutableStateOf(listOf<OcrItem>()) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("餐饮") }
    var account by remember { mutableStateOf("校园卡") }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        msg = "识别中…"
        runCatching {
            val image = InputImage.fromFilePath(ctx, uri)
            val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val parsed = Parser.parse(result.text)
                    val now = System.currentTimeMillis()
                    items = parsed.map { (amount, merchant) ->
                        OcrItem(
                            checked = amount < 0,
                            amount = kotlin.math.abs(amount),
                            merchant = merchant,
                            memo = merchant,
                            dup = Dao.exists(ctx, now, kotlin.math.abs(amount), merchant)
                        )
                    }
                    busy = false
                    msg = "识别到 ${items.size} 条（勾选后保存，时间记为现在）"
                }
                .addOnFailureListener {
                    busy = false
                    msg = "识别失败：${it.message ?: "未知错误"}，可以换一张更清晰的截图"
                }
        }.onFailure {
            busy = false
            msg = "图片读取失败：${it.message}"
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF4F6FA))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { (ctx as? ComponentActivity)?.finish() }) { Text("返回") }
            Text("截图导入", fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 14.dp))
        }
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            item {
                SectionCard("第一步：选截图") {
                    Text(
                        "选校园卡明细截图（支持相册里任意图片）。充值记录自动过滤；疑似重复只标记、不自动删。",
                        fontSize = 12.sp, color = TextGray
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { pick.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (busy) "识别中…" else "从相册选择截图")
                    }
                    if (msg.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(msg, fontSize = 12.sp, color = TextGray)
                    }
                }
            }
            if (items.isNotEmpty()) {
                item {
                    SectionCard("第二步：统一设置") {
                        Text("品类", fontSize = 12.sp, color = TextGray)
                        Spacer(Modifier.height(6.dp))
                        Chips(Prefs.categories, category) { category = it }
                        Spacer(Modifier.height(10.dp))
                        Text("账户", fontSize = 12.sp, color = TextGray)
                        Spacer(Modifier.height(6.dp))
                        Chips(Prefs.accounts, account) { account = it }
                    }
                }
                item {
                    SectionCard("第三步：逐条确认（共 ${items.size} 条）") {
                        items.forEachIndexed { idx, it ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = it.checked,
                                    onCheckedChange = { c ->
                                        items = items.toMutableList().also { l -> l[idx] = l[idx].copy(checked = c) }
                                    }
                                )
                                OutlinedTextField(
                                    value = if (it.amount % 1.0 == 0.0) it.amount.toLong().toString() else it.amount.toString(),
                                    onValueChange = { s ->
                                        val v = s.filter { c -> c.isDigit() || c == '.' }.toDoubleOrNull() ?: return@OutlinedTextField
                                        items = items.toMutableList().also { l -> l[idx] = l[idx].copy(amount = v) }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.width(110.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.fillMaxWidth(0.5f)) {
                                    Text(it.merchant.ifBlank { "（无名称）" }, fontSize = 13.sp, color = TextDark)
                                    if (it.dup) Text("疑似重复", fontSize = 11.sp, color = OverRed)
                                }
                                if (it.dup) {
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFFDEBEB))
                                            .clickable {
                                                items = items.toMutableList().also { l -> l[idx] = l[idx].copy(checked = !l[idx].checked) }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("删/留", fontSize = 11.sp, color = OverRed)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val chosen = items.filter { it.checked }
                                if (chosen.isEmpty()) {
                                    msg = "没有勾选任何一条"
                                    return@Button
                                }
                                val now = System.currentTimeMillis()
                                chosen.forEach { o ->
                                    Dao.insert(
                                        ctx,
                                        Tx(
                                            ts = now, amount = o.amount, kind = "expense",
                                            category = category, account = account,
                                            memo = o.memo, tags = Tags.tag(o.memo) ?: "",
                                            source = "ocr", merchant = o.merchant
                                        )
                                    )
                                }
                                LimitNotifier.afterExpense(ctx)
                                Toast.makeText(ctx, "已导入 ${chosen.size} 笔", Toast.LENGTH_SHORT).show()
                                (ctx as? ComponentActivity)?.finish()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("保存勾选的 ${items.count { it.checked }} 笔") }
                    }
                }
            }
            item { Spacer(Modifier.height(60.dp)) }
        }
    }
}
