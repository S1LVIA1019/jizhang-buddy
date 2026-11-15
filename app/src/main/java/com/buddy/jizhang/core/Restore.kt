package com.buddy.jizhang.core

import android.content.Context
import com.buddy.jizhang.data.Dao
import com.buddy.jizhang.data.Tx
import com.buddy.jizhang.widget.WidgetUpdater
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/** 从旧版导出的 JSON 备份恢复逐笔记录（含备注原文与自动标签），按时间+金额+备注去重 */
object Restore {

    private val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun run(ctx: Context, json: String): Int {
        val root = JSONObject(json)
        val arr = root.optJSONArray("transactions") ?: return 0
        val existing = Dao.all(ctx, 0L, Long.MAX_VALUE)
        var n = 0
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val timeStr = o.optString("time", "")
            val ts = runCatching {
                LocalDateTime.parse(timeStr, dtf)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }.getOrNull() ?: continue
            val amount = o.optDouble("amount", 0.0)
            if (amount <= 0.0) continue
            val memo = o.optString("memo", "")
            val dup = existing.any {
                abs(it.ts - ts) < 60_000 && abs(it.amount - amount) < 0.001 && it.memo == memo
            }
            if (dup) continue
            Dao.insert(
                ctx,
                Tx(
                    ts = ts,
                    amount = amount,
                    kind = o.optString("type", "expense"),
                    category = o.optString("category", "其他"),
                    account = o.optString("account", "现金"),
                    memo = memo,
                    tags = o.optString("tags", ""),
                    source = "restore",
                    merchant = o.optString("merchant", "")
                )
            )
            n++
        }
        if (n > 0) {
            runCatching {
                Piggy.ensureSettled(ctx)
                Budget.refreshNextRecommend(ctx)
                WidgetUpdater.refresh(ctx)
            }
        }
        return n
    }
}
