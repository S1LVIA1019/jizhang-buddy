package com.buddy.jizhang.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.buddy.jizhang.core.Perm

@Composable
fun PermGuideContent(onClose: () -> Unit) {
    val ctx = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val acc = remember(refresh) { Perm.accessibilityOn(ctx) }
    val notif = remember(refresh) { Perm.notificationsOn(ctx) }
    val overlay = remember(refresh) { Perm.overlayOn(ctx) }
    val count = listOf(acc, notif, overlay).count { it }

    HeroCard(
        label = "当前状态",
        big = "$count / 3",
        note = if (acc) "权限已就绪，悬浮窗会在打开购物 App 时弹出"
        else "还差「无障碍服务」这一项，购物悬浮窗不会弹出"
    )

    SectionCard("需要开启的权限") {
        PermRow("通知权限", "限额提醒、每日总结", notif, "已开启", "未开启")
        PermRow("无障碍服务", "记账buddy · 购物提醒", acc, "已开启", "未开启")
        PermRow("悬浮窗", "显示在其他应用上层", overlay, "已开启", "未开启（无障碍方式也能用）")
        Spacer(Modifier.height(4.dp))
        Text(
            "ColorOS 还需给本应用「自启动」并关闭电池优化，否则后台定时提醒可能被系统清理。",
            fontSize = 11.sp,
            color = TextGray,
            lineHeight = 17.sp
        )
    }

    SectionCard("OPPO 特别提示 · 开关是灰的看这里", container = SoftRed) {
        Text("1  设置 → 应用管理 → 记账buddy", fontSize = 11.5.sp, color = Color(0xFF8B4038), lineHeight = 21.sp)
        Text("2  点右上角 ⋮ 三个点", fontSize = 11.5.sp, color = Color(0xFF8B4038), lineHeight = 21.sp)
        Text("3  选「允许受限设置」", fontSize = 11.5.sp, color = Color(0xFF8B4038), lineHeight = 21.sp)
        Spacer(Modifier.height(4.dp))
        Text("再回到无障碍页，开关就能打开了。", fontSize = 11.sp, color = TextGray)
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = { Perm.openAppDetails(ctx) }, modifier = Modifier.fillMaxWidth()) {
            Text("去应用详情页", fontSize = 12.sp)
        }
    }

    Button(
        onClick = { Perm.openAccessibility(ctx) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp)
    ) { Text("一键前往系统无障碍设置", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }

    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)) {
        OutlinedButton(
            onClick = { refresh++ },
            modifier = Modifier.weight(1f).padding(end = 5.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("重新检测", fontSize = 12.sp) }
        OutlinedButton(
            onClick = { Perm.openOverlay(ctx) },
            modifier = Modifier.weight(1f).padding(start = 5.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("悬浮窗设置", fontSize = 12.sp) }
    }

    SectionCard("为什么上一版找不到这个 App", container = SoftPurple) {
        Text(
            "系统里服务名称显示为「记账buddy · 购物提醒」，可以直接搜到。上一版注册权限名写错，系统压根没把它当成无障碍服务，所以列表里不会出现。",
            fontSize = 11.5.sp,
            color = Color(0xFF4A3D6E),
            lineHeight = 19.sp
        )
    }

    OutlinedButton(
        onClick = onClose,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp)
    ) { Text("关闭", fontSize = 12.sp) }
}

@Composable
private fun PermRow(
    title: String,
    sub: String,
    on: Boolean,
    onText: String,
    offText: String
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (on) OkGreen else OverRed)
        )
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 12.5.sp, color = TextDark, fontWeight = FontWeight.Bold)
            Text(sub, fontSize = 10.sp, color = TextGray)
        }
        Text(
            if (on) onText else offText,
            fontSize = 10.sp,
            color = if (on) OkGreen else OverRed,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PermScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(PageBg)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack, shape = RoundedCornerShape(12.dp)) { Text("返回", fontSize = 12.sp) }
            Text(
                "开启权限",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.padding(start = 14.dp)
            )
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item { PermGuideContent(onClose = onBack) }
            item { Spacer(Modifier.height(60.dp)) }
        }
    }
}

@Composable
fun PermGuideDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = PageBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    "先开启权限",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.padding(start = 18.dp, bottom = 4.dp)
                )
                PermGuideContent(onClose = onDismiss)
            }
        }
    }
}
