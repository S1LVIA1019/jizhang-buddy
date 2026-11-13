package com.buddy.jizhang.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 配色 A：淡紫主卡 + 浅灰内容卡 + 紫色正常 / 红色警示
val Blue = Color(0xFF7C5CD6)
val BlueLight = Color(0xFFA88BE8)
val TextDark = Color(0xFF1E2230)
val TextGray = Color(0xFF8A90A0)
val WarnOrange = Color(0xFFE06A5F)
val OverRed = Color(0xFFD8382F)
val OkGreen = Color(0xFF0FA37A)
val HeroBg = Color(0xFFEFE4FF)
val HeroInk = Color(0xFF2E1A5E)
val CardBg = Color(0xFFF0F0F3)
val SoftPurple = Color(0xFFEDE7FB)
val SoftRed = Color(0xFFFFF0EE)
val PageBg = Color(0xFFFFFFFF)
val TrackBg = Color(0xFFE0E2E9)
val ChipBg = Color(0xFFE5E6EC)

@Composable
fun SectionCard(
    title: String,
    container: Color = CardBg,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(Modifier.padding(15.dp)) {
            if (title.isNotEmpty()) {
                Text(title, fontSize = 12.5.sp, color = TextGray, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
            }
            content()
        }
    }
}

@Composable
fun HeroCard(
    label: String,
    big: String,
    note: String,
    split: List<Pair<String, String>> = emptyList()
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = HeroBg),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(label, fontSize = 12.sp, color = HeroInk.copy(alpha = 0.72f), fontWeight = FontWeight.SemiBold)
            Text(
                big,
                fontSize = 32.sp,
                color = HeroInk,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1.4).sp,
                lineHeight = 38.sp
            )
            if (note.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(note, fontSize = 11.5.sp, color = HeroInk.copy(alpha = 0.72f), lineHeight = 17.sp)
            }
            if (split.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(HeroInk.copy(alpha = 0.10f)))
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    split.forEach { (k, v) ->
                        Column(Modifier.weight(1f)) {
                            Text(k, fontSize = 10.sp, color = HeroInk.copy(alpha = 0.7f))
                            Text(v, fontSize = 15.sp, color = HeroInk, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Chips(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(4).forEach { rowOpts ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowOpts.forEach { o ->
                    val sel = o == selected
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (sel) Blue else ChipBg)
                            .clickable { onSelect(o) }
                            .padding(horizontal = 13.dp, vertical = 8.dp)
                    ) {
                        Text(
                            o,
                            fontSize = 12.5.sp,
                            color = if (sel) Color.White else Color(0xFF5A6072),
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Bar(ratio: Float, color: Color = Blue) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(TrackBg)
    ) {
        Box(
            Modifier
                .fillMaxWidth(ratio.coerceIn(0f, 1f))
                .height(7.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color)
        )
    }
}

@Composable
fun KV(k: String, v: String, vColor: Color = TextDark) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(k, fontSize = 12.5.sp, color = TextGray)
        Text(v, fontSize = 12.5.sp, color = vColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TagPill(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0xFFE8DDFB))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 10.sp, color = Color(0xFF5B3BA8), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SquareMark(color: Color = Blue) {
    Box(
        Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(9.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.92f))
        )
    }
}
