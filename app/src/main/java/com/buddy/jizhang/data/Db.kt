package com.buddy.jizhang.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class Tx(
    val id: Long = 0,
    val ts: Long,
    val amount: Double,
    val kind: String,
    val category: String,
    val account: String,
    val memo: String,
    val tags: String,
    val source: String,
    val merchant: String
)

data class Settlement(
    val ym: String,
    val income: Double,
    val expense: Double,
    val balance: Double,
    val piggyAfter: Double
)

class Db private constructor(ctx: Context) :
    SQLiteOpenHelper(ctx.applicationContext, "jizhang.db", null, 2) {

    companion object {
        @Volatile private var inst: Db? = null
        fun get(ctx: Context): Db =
            inst ?: synchronized(this) { inst ?: Db(ctx).also { inst = it } }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE tx(_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ts INTEGER NOT NULL, amount REAL NOT NULL, kind TEXT NOT NULL, " +
                "category TEXT, account TEXT, memo TEXT, tags TEXT, source TEXT, merchant TEXT)"
        )
        db.execSQL("CREATE INDEX idx_ts ON tx(ts)")
        createSettle(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        createSettle(db)
    }

    private fun createSettle(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS settle(ym TEXT PRIMARY KEY, income REAL, expense REAL, " +
                "balance REAL, piggy_after REAL, created_at INTEGER)"
        )
    }
}

object Dao {

    private val COLS = "_id,ts,amount,kind,category,account,memo,tags,source,merchant"

    private fun cv(t: Tx): ContentValues = ContentValues().apply {
        put("ts", t.ts)
        put("amount", t.amount)
        put("kind", t.kind)
        put("category", t.category)
        put("account", t.account)
        put("memo", t.memo)
        put("tags", t.tags)
        put("source", t.source)
        put("merchant", t.merchant)
    }

    private fun row(c: android.database.Cursor): Tx = Tx(
        id = c.getLong(0),
        ts = c.getLong(1),
        amount = c.getDouble(2),
        kind = c.getString(3) ?: "",
        category = c.getString(4) ?: "",
        account = c.getString(5) ?: "",
        memo = c.getString(6) ?: "",
        tags = c.getString(7) ?: "",
        source = c.getString(8) ?: "",
        merchant = c.getString(9) ?: ""
    )

    fun insert(ctx: Context, t: Tx): Long =
        Db.get(ctx).writableDatabase.insert("tx", null, cv(t))

    fun update(ctx: Context, t: Tx) {
        Db.get(ctx).writableDatabase.update("tx", cv(t), "_id=?", arrayOf(t.id.toString()))
    }

    fun delete(ctx: Context, id: Long) {
        Db.get(ctx).writableDatabase.delete("tx", "_id=?", arrayOf(id.toString()))
    }

    fun all(ctx: Context, from: Long, to: Long): List<Tx> {
        val out = mutableListOf<Tx>()
        Db.get(ctx).readableDatabase.rawQuery(
            "SELECT $COLS FROM tx WHERE ts>=? AND ts<? ORDER BY ts DESC",
            arrayOf(from.toString(), to.toString())
        ).use { c ->
            while (c.moveToNext()) out += row(c)
        }
        return out
    }

    fun recent(ctx: Context, n: Int): List<Tx> {
        val out = mutableListOf<Tx>()
        Db.get(ctx).readableDatabase.rawQuery(
            "SELECT $COLS FROM tx ORDER BY ts DESC LIMIT ?",
            arrayOf(n.toString())
        ).use { c ->
            while (c.moveToNext()) out += row(c)
        }
        return out
    }

    fun sum(ctx: Context, from: Long, to: Long, kind: String, category: String? = null): Double {
        val sel = if (category == null) "ts>=? AND ts<? AND kind=?"
        else "ts>=? AND ts<? AND kind=? AND category=?"
        val args = if (category == null) arrayOf(from.toString(), to.toString(), kind)
        else arrayOf(from.toString(), to.toString(), kind, category)
        Db.get(ctx).readableDatabase.rawQuery(
            "SELECT total(amount) FROM tx WHERE $sel", args
        ).use { c ->
            return if (c.moveToFirst()) c.getDouble(0) else 0.0
        }
    }

    fun expenseCount(ctx: Context, from: Long, to: Long): Int {
        Db.get(ctx).readableDatabase.rawQuery(
            "SELECT count(*) FROM tx WHERE ts>=? AND ts<? AND kind='expense'",
            arrayOf(from.toString(), to.toString())
        ).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    fun exists(ctx: Context, ts: Long, amount: Double, merchant: String): Boolean {
        Db.get(ctx).readableDatabase.rawQuery(
            "SELECT 1 FROM tx WHERE ts BETWEEN ? AND ? AND abs(amount-?)<0.001 AND merchant=?",
            arrayOf((ts - 120000).toString(), (ts + 120000).toString(), amount.toString(), merchant)
        ).use { c ->
            return c.moveToFirst()
        }
    }

    /** 账本总余额（含所有月份、不含罐内支出） */
    fun balance(ctx: Context): Double {
        Db.get(ctx).readableDatabase.rawQuery(
            "SELECT (SELECT total(amount) FROM tx WHERE kind='income') - " +
                "(SELECT total(amount) FROM tx WHERE kind='expense')", null
        ).use { c ->
            return if (c.moveToFirst()) c.getDouble(0) else 0.0
        }
    }

    fun firstTxTs(ctx: Context): Long? {
        Db.get(ctx).readableDatabase.rawQuery("SELECT min(ts) FROM tx", null).use { c ->
            return if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
        }
    }

    fun insertSettlement(ctx: Context, s: Settlement) {
        val cv = ContentValues().apply {
            put("ym", s.ym)
            put("income", s.income)
            put("expense", s.expense)
            put("balance", s.balance)
            put("piggy_after", s.piggyAfter)
            put("created_at", System.currentTimeMillis())
        }
        Db.get(ctx).writableDatabase.insertWithOnConflict("settle", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun settlementBalanceTotal(ctx: Context): Double {
        Db.get(ctx).readableDatabase.rawQuery("SELECT total(balance) FROM settle", null).use { c ->
            return if (c.moveToFirst()) c.getDouble(0) else 0.0
        }
    }

    fun settlements(ctx: Context): List<Settlement> {
        val out = mutableListOf<Settlement>()
        Db.get(ctx).readableDatabase.rawQuery(
            "SELECT ym,income,expense,balance,piggy_after FROM settle ORDER BY ym DESC", null
        ).use { c ->
            while (c.moveToNext()) out += Settlement(c.getString(0), c.getDouble(1), c.getDouble(2), c.getDouble(3), c.getDouble(4))
        }
        return out
    }

    fun piggyTotal(ctx: Context): Double {
        Db.get(ctx).readableDatabase.rawQuery(
            "SELECT total(amount) FROM tx WHERE kind='piggy'", null
        ).use { c ->
            return if (c.moveToFirst()) c.getDouble(0) else 0.0
        }
    }

    fun clearAll(ctx: Context) {
        val db = Db.get(ctx).writableDatabase
        db.delete("tx", null, null)
        db.delete("settle", null, null)
    }
}

fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

val ymFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
