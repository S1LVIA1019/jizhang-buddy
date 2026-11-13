package com.buddy.jizhang.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Blue = Color(0xFF0B57F0)
val BlueLight = Color(0xFF5E9CFF)
val TextDark = Color(0xFF14161C)
val TextGray = Color(0xFF6B7080)
val WarnOrange = Color(0xFFF59E0B)
val OverRed = Color(0xFFE23B2E)
val OkGreen = Color(0xFF0FA37A)

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 12.sp, color = TextGray)
            Spacer(Modifier.height(10.dp))
            content()
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
                    androidx.compose.foundation.layout.Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) Blue else Color(0xFFEEF1F6))
                            .clickable { onSelect(o) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            o,
                            fontSize = 13.sp,
                            color = if (sel) Color.White else Color(0xFF374151)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Bar(ratio: Float, color: Color) {
    androidx.compose.foundation.layout.Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFE8ECF2))
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier
                .fillMaxWidth(ratio.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
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
        Text(k, fontSize = 13.sp, color = TextGray)
        Text(v, fontSize = 13.sp, color = vColor, fontWeight = FontWeight.Medium)
    }
}
