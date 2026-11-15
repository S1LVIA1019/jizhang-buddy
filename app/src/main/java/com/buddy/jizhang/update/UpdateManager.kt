package com.buddy.jizhang.update

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import com.buddy.jizhang.BuildConfig
import com.buddy.jizhang.core.Export
import com.buddy.jizhang.core.Period
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object UpdateManager {

    private const val OWNER = "S1LVIA1019"
    private const val REPO = "jizhang-buddy"
    private val main = Handler(Looper.getMainLooper())

    data class Info(val build: Int, val url: String, val current: Int)

    fun currentVersion(): Int = BuildConfig.VERSION_CODE

    fun check(ctx: Context, cb: (Info?) -> Unit) {
        Thread {
            var result: Info? = null
            try {
                val conn = URL("https://api.github.com/repos/$OWNER/$REPO/releases/latest")
                    .openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val n = Regex("\"tag_name\"\\s*:\\s*\"build-(\\d+)\"")
                    .find(body)?.groupValues?.getOrNull(1)?.toIntOrNull()
                if (n != null && n > currentVersion()) {
                    result = Info(
                        n,
                        "https://github.com/$OWNER/$REPO/releases/download/build-$n/app-debug.apk",
                        currentVersion()
                    )
                }
            } catch (e: Exception) {
                result = null
            }
            val r = result
            main.post { cb(r) }
        }.start()
    }

    /** 更新前自动导出一份完整 JSON 备份到应用外部私有目录 */
    fun backup(ctx: Context): File? = try {
        val (json, _, _) = Export.build(ctx, Period.YEAR, true)
        val dir = File(ctx.getExternalFilesDir(null), "backup").apply { mkdirs() }
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
        val f = File(dir, "backup-$stamp.json")
        f.writeText(json)
        f
    } catch (e: Exception) {
        null
    }

    fun download(ctx: Context, info: Info, onDone: (File?) -> Unit) {
        Thread {
            var out: File? = null
            try {
                val dir = File(ctx.cacheDir, "downloads").apply { mkdirs() }
                val f = File(dir, "app-debug-${info.build}.apk")
                val conn = URL(info.url).openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.instanceFollowRedirects = true
                conn.inputStream.use { input ->
                    FileOutputStream(f).use { os -> input.copyTo(os) }
                }
                conn.disconnect()
                if (f.length() > 100_000) out = f
            } catch (e: Exception) {
                out = null
            }
            val r = out
            main.post { onDone(r) }
        }.start()
    }

    fun install(ctx: Context, file: File) {
        val uri = FileProvider.getUriForFile(ctx, "com.buddy.jizhang.fileprovider", file)
        val i = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(i)
    }

    fun manualUrl(): String = "https://github.com/$OWNER/$REPO/releases/latest"
}
