package com.buddy.jizhang.core

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import com.buddy.jizhang.service.WatchService

object Perm {

    /**
     * 三重检测：
     * 1. 服务实例是否真的连上了（最贴近「悬浮窗会不会弹」）
     * 2. 系统 Secure 字符串按 ComponentName 解析，兼容 .缩写 / 全类名 / 各种 ROM 写法
     * 3. AccessibilityManager 已启用列表兜底
     */
    fun accessibilityOn(ctx: Context): Boolean {
        if (WatchService.instance != null) return true

        val enabled = runCatching {
            Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        }.getOrNull().orEmpty()
        if (enabled.isNotBlank()) {
            for (part in enabled.split(':')) {
                val cn = ComponentName.unflattenFromString(part) ?: continue
                if (cn.packageName == ctx.packageName && cn.className.endsWith("WatchService")) return true
            }
        }

        return runCatching {
            val am = ctx.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { it.serviceInfo.packageName == ctx.packageName && it.serviceInfo.name.endsWith("WatchService") }
        }.getOrDefault(false)
    }

    fun overlayOn(ctx: Context): Boolean = Settings.canDrawOverlays(ctx)

    fun notificationsOn(ctx: Context): Boolean =
        NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    fun openAccessibility(ctx: Context) {
        runCatching {
            ctx.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure {
            runCatching {
                ctx.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    fun openAppDetails(ctx: Context) {
        runCatching {
            ctx.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + ctx.packageName)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun openOverlay(ctx: Context) {
        runCatching {
            ctx.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + ctx.packageName)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
