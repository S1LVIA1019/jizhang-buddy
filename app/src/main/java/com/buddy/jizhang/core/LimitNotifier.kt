package com.buddy.jizhang.core

import android.content.Context
import com.buddy.jizhang.service.Notify

object LimitNotifier {
    /** 每记一笔支出后调用：超限的每笔都提醒，接近阈值的提醒一次 */
    fun afterExpense(ctx: Context) {
        for (s in Limits.statuses(ctx)) {
            if (s.limit <= 0) continue
            when (s.state) {
                2 -> Notify.show(
                    ctx, s.name.hashCode(), Notify.CH_LIMIT,
                    "已超${s.name}",
                    "已用 ¥${fmt(s.used)} / ¥${fmt(s.limit)}，超支 ¥${fmt(s.used - s.limit)}"
                )
                1 -> Notify.show(
                    ctx, s.name.hashCode(), Notify.CH_LIMIT,
                    "${s.name}接近上限",
                    "已用 ${s.pct}%（¥${fmt(s.used)} / ¥${fmt(s.limit)}）"
                )
            }
        }
    }
}
