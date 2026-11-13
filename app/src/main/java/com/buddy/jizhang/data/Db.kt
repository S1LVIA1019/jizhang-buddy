package com.buddy.jizhang.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

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

class Db private constructor(ctx: Context) :
    SQLiteOpenHelper(ctx.applicationContext, "jizhang.db", null, 1) {

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
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { }
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

    fun balance(ctx: Context): Double {
        Db.get(ctx).readableDatabase.rawQuery(
            "SELECT (SELECT total(amount) FROM tx WHERE kind='income') - " +
                "(SELECT total(amount) FROM tx WHERE kind='expense')", null
        ).use { c ->
            return if (c.moveToFirst()) c.getDouble(0) else 0.0
        }
    }

    fun clearAll(ctx: Context) {
        Db.get(ctx).writableDatabase.delete("tx", null, null)
    }
}
