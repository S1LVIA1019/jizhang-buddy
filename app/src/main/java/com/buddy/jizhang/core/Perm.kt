package com.buddy.jizhang.core

import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object Perm {
    fun accessibilityOn(ctx: Context): Boolean {
        val s = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return s.contains("com.buddy.jizhang/.service.WatchService") ||
            s.contains("com.buddy.jizhang/service.WatchService")
    }

    fun overlayOn(ctx: Context): Boolean = Settings.canDrawOverlays(ctx)

    fun notificationsOn(ctx: Context): Boolean =
        NotificationManagerCompat.from(ctx).areNotificationsEnabled()
}
