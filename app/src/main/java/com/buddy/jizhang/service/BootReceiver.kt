package com.buddy.jizhang.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.buddy.jizhang.widget.WidgetUpdater

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> Alarms.scheduleAll(ctx)
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> Unit
        }
        runCatching { WidgetUpdater.refresh(ctx) }
    }
}
