package com.pasupathi.bustracker

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Route(
    val id: String,
    val busNo: String,
    val from: String,
    val to: String,
    val type: String
)

class DB(ctx: Context) : SQLiteOpenHelper(ctx, "bus.db", null, 1) {

    override fun onCreate(d: SQLiteDatabase) {
        d.execSQL("CREATE TABLE routes(id TEXT PRIMARY KEY, busNo TEXT, fromPlace TEXT, toPlace TEXT, type TEXT)")
        d.execSQL("CREATE TABLE timings(id TEXT PRIMARY KEY, routeId TEXT, time TEXT, days TEXT)")
    }

    override fun onUpgrade(d: SQLiteDatabase, o: Int, n: Int) {}

    fun saveRoute(r: Route) {
        val v = ContentValues()
        v.put("id", r.id)
        v.put("busNo", r.busNo)
        v.put("fromPlace", r.from)
        v.put("toPlace", r.to)
        v.put("type", r.type)
        writableDatabase.insertWithOnConflict("routes", null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun removeRoute(id: String) {
        writableDatabase.delete("routes", "id=?", arrayOf(id))
        writableDatabase.delete("timings", "routeId=?", arrayOf(id))
    }

    fun saveTiming(id: String, routeId: String, time: String, days: String) {
        val v = ContentValues()
        v.put("id", id)
        v.put("routeId", routeId)
        v.put("time", time)
        v.put("days", days)
        writableDatabase.insertWithOnConflict("timings", null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun removeTiming(id: String) {
        writableDatabase.delete("timings", "id=?", arrayOf(id))
    }

    fun searchRoutes(q: String): List<Route> {
        val like = "%$q%"
        val c = readableDatabase.rawQuery(
            "SELECT id,busNo,fromPlace,toPlace,type FROM routes " +
                "WHERE busNo LIKE ? OR fromPlace LIKE ? OR toPlace LIKE ? ORDER BY busNo",
            arrayOf(like, like, like)
        )
        val out = ArrayList<Route>()
        while (c.moveToNext()) {
            out.add(Route(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4)))
        }
        c.close()
        return out
    }

    fun timingsFor(routeId: String): List<String> {
        val c = readableDatabase.rawQuery(
            "SELECT time,days FROM timings WHERE routeId=? ORDER BY time",
            arrayOf(routeId)
        )
        val out = ArrayList<String>()
        while (c.moveToNext()) out.add("${c.getString(0)}   (${c.getString(1)})")
        c.close()
        return out
    }

    fun routeCount(): Int {
        val c = readableDatabase.rawQuery("SELECT COUNT(*) FROM routes", null)
        c.moveToFirst()
        val n = c.getInt(0)
        c.close()
        return n
    }
}
