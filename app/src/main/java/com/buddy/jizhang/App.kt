package com.buddy.jizhang

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.buddy.jizhang.data.Prefs
import com.buddy.jizhang.service.Alarms
import com.buddy.jizhang.service.Notify

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(Notify.CH_SUMMARY, "总结提醒", NotificationManager.IMPORTANCE_DEFAULT)
        )
        nm.createNotificationChannel(
            NotificationChannel(Notify.CH_LIMIT, "限额提醒", NotificationManager.IMPORTANCE_HIGH)
        )
        nm.createNotificationChannel(
            NotificationChannel(Notify.CH_FINANCE, "理财提醒", NotificationManager.IMPORTANCE_DEFAULT)
        )
        Alarms.scheduleAll(this)
    }
}
