package com.buddy.jizhang.core

import com.buddy.jizhang.data.Prefs

object Tags {

    val rules = linkedMapOf(
        "食堂" to "食堂", "饭堂" to "食堂",
        "外卖" to "外卖",
        "奶茶" to "奶茶", "咖啡" to "奶茶", "果茶" to "奶茶", "茶百道" to "奶茶", "蜜雪" to "奶茶",
        "零食" to "零食", "薯片" to "零食", "辣条" to "零食", "饼干" to "零食",
        "化妆品" to "化妆品", "口红" to "化妆品", "护肤" to "化妆品", "粉底" to "化妆品",
        "商场" to "外食", "餐厅" to "外食", "聚餐" to "外食", "出去吃" to "外食", "火锅" to "外食",
        "烤肉" to "外食", "麦当劳" to "外食", "肯德基" to "外食", "必胜客" to "外食", "探店" to "外食",
        "打车" to "打车", "滴滴" to "打车", "出租" to "打车", "网约车" to "打车",
        "地铁" to "公交地铁", "公交" to "公交地铁", "巴士" to "公交地铁",
        "单车" to "公交地铁", "轻轨" to "公交地铁",
        "火车" to "长途", "高铁" to "长途",
        "水果" to "水果",
        "游戏" to "娱乐", "电影" to "娱乐", "会员" to "娱乐", "KTV" to "娱乐", "演出" to "娱乐"
    )

    /** 返回逗号分隔的全部标签；备注含合理理由的打车额外打「合理打车」 */
    fun tag(memo: String): String? {
        val m = memo.trim()
        if (m.isEmpty()) return null
        val out = linkedSetOf<String>()
        for ((k, v) in rules) if (m.contains(k)) out.add(v)
        if (out.contains("打车")) {
            for (r in Prefs.reasonList()) {
                if (m.contains(r)) { out.add("合理打车"); break }
            }
        }
        return if (out.isEmpty()) null else out.joinToString(",")
    }

    fun memoAnalysis(txs: List<com.buddy.jizhang.data.Tx>): LinkedHashMap<String, Pair<Int, Double>> {
        val m = LinkedHashMap<String, Pair<Int, Double>>()
        for (t in txs) {
            if (t.kind != "expense" && t.kind != "piggy") continue
            for (g in t.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }) {
                val cur = m[g] ?: (0 to 0.0)
                m[g] = (cur.first + 1) to (cur.second + t.amount)
            }
        }
        return m
    }
}
