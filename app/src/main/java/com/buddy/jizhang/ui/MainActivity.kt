package com.buddy.jizhang.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buddy.jizhang.core.Perm

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        setContent {
            JizhangApp()
        }
    }
}

@Composable
fun JizhangApp() {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
    var showGuide by rememberSaveable { mutableStateOf(!Perm.accessibilityOn(ctx)) }

    JizhangTheme {
        Scaffold(
            containerColor = PageBg,
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    listOf("首页", "记账", "统计", "我的").forEachIndexed { i, label ->
                        val sel = tab == i
                        NavigationBarItem(
                            selected = sel,
                            onClick = { tab = i },
                            icon = {
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (sel) HeroBg else Color.Transparent)
                                        .padding(horizontal = 11.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) { RoundTabIcon(i, sel) }
                            },
                            label = {
                                Text(
                                    label,
                                    fontSize = 10.sp,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad)) {
                when (tab) {
                    0 -> HomeScreen(tick)
                    1 -> AddScreen(onSaved = { tick++ })
                    2 -> StatsScreen(tick)
                    3 -> MeScreen(tick, onTick = { tick++ })
                }
            }
        }

        if (showGuide) {
            PermGuideDialog(onDismiss = { showGuide = false })
        }
    }
}

@Composable
fun RoundTabIcon(which: Int, active: Boolean) {
    val c = if (active) Blue else Color(0xFFA9AFBC)
    Canvas(Modifier.size(19.dp)) {
        val s = size.minDimension
        when (which) {
            0 -> {
                val roof = Path().apply {
                    moveTo(s * 0.5f, s * 0.08f)
                    lineTo(s * 0.97f, s * 0.46f)
                    lineTo(s * 0.03f, s * 0.46f)
                    close()
                }
                drawPath(path = roof, color = c)
                drawCircle(color = c, radius = s * 0.10f, center = Offset(s * 0.5f, s * 0.10f))
                drawCircle(color = c, radius = s * 0.08f, center = Offset(s * 0.05f, s * 0.45f))
                drawCircle(color = c, radius = s * 0.08f, center = Offset(s * 0.95f, s * 0.45f))
                drawRoundRect(
                    color = c,
                    topLeft = Offset(s * 0.16f, s * 0.42f),
                    size = Size(s * 0.68f, s * 0.50f),
                    cornerRadius = CornerRadius(s * 0.10f)
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(s * 0.43f, s * 0.63f),
                    size = Size(s * 0.14f, s * 0.29f),
                    cornerRadius = CornerRadius(s * 0.05f)
                )
            }
            1 -> {
                drawRoundRect(
                    color = c,
                    topLeft = Offset.Zero,
                    size = Size(s, s),
                    cornerRadius = CornerRadius(s * 0.28f)
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(s * 0.44f, s * 0.24f),
                    size = Size(s * 0.12f, s * 0.52f),
                    cornerRadius = CornerRadius(s * 0.06f)
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(s * 0.24f, s * 0.44f),
                    size = Size(s * 0.52f, s * 0.12f),
                    cornerRadius = CornerRadius(s * 0.06f)
                )
            }
            2 -> {
                val w = s * 0.19f
                drawRoundRect(
                    color = c,
                    topLeft = Offset(s * 0.06f, s * 0.55f),
                    size = Size(w, s * 0.39f),
                    cornerRadius = CornerRadius(w / 2f)
                )
                drawRoundRect(
                    color = c,
                    topLeft = Offset(s * 0.405f, s * 0.32f),
                    size = Size(w, s * 0.62f),
                    cornerRadius = CornerRadius(w / 2f)
                )
                drawRoundRect(
                    color = c,
                    topLeft = Offset(s * 0.75f, s * 0.08f),
                    size = Size(w, s * 0.86f),
                    cornerRadius = CornerRadius(w / 2f)
                )
            }
            3 -> {
                drawCircle(color = c, radius = s * 0.21f, center = Offset(s * 0.5f, s * 0.30f))
                drawRoundRect(
                    color = c,
                    topLeft = Offset(s * 0.14f, s * 0.60f),
                    size = Size(s * 0.72f, s * 0.40f),
                    cornerRadius = CornerRadius(s * 0.20f)
                )
            }
        }
    }
}
