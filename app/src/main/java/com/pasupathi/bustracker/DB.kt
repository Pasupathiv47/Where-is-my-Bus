package com.pasupathi.bustracker

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.TreeSet

data class Route(
    val id: String,
    val busNo: String,
    val from: String,
    val to: String,
    val type: String,
    val stops: String
)

data class Trip(
    val id: String,
    val dep: String,
    val arr: String,
    val days: String,
    val stops: String
)

data class Bus(
    val id: String,
    val busNo: String,
    val regNo: String,
    val photo: String
)

class DB(ctx: Context) : SQLiteOpenHelper(ctx, "bus.db", null, 3) {
    private val appCtx = ctx.applicationContext

    override fun onCreate(d: SQLiteDatabase) {
        d.execSQL("CREATE TABLE routes(id TEXT PRIMARY KEY, busNo TEXT, fromPlace TEXT, toPlace TEXT, type TEXT, stops TEXT)")
        d.execSQL("CREATE TABLE timings(id TEXT PRIMARY KEY, routeId TEXT, time TEXT, arr TEXT, days TEXT, stops TEXT)")
        d.execSQL("CREATE TABLE buses(id TEXT PRIMARY KEY, busNo TEXT, regNo TEXT, photo TEXT)")
    }

    override fun onUpgrade(d: SQLiteDatabase, o: Int, n: Int) {
        d.execSQL("DROP TABLE IF EXISTS routes")
        d.execSQL("DROP TABLE IF EXISTS timings")
        d.execSQL("DROP TABLE IF EXISTS buses")
        onCreate(d)
        appCtx.getSharedPreferences("sync", Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun saveRoute(r: Route) {
        val v = ContentValues()
        v.put("id", r.id); v.put("busNo", r.busNo); v.put("fromPlace", r.from)
        v.put("toPlace", r.to); v.put("type", r.type); v.put("stops", r.stops)
        writableDatabase.insertWithOnConflict("routes", null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun removeRoute(id: String) {
        writableDatabase.delete("routes", "id=?", arrayOf(id))
        writableDatabase.delete("timings", "routeId=?", arrayOf(id))
    }

    fun saveTiming(id: String, routeId: String, time: String, arr: String, days: String, stops: String) {
        val v = ContentValues()
        v.put("id", id); v.put("routeId", routeId); v.put("time", time)
        v.put("arr", arr); v.put("days", days); v.put("stops", stops)
        writableDatabase.insertWithOnConflict("timings", null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun removeTiming(id: String) {
        writableDatabase.delete("timings", "id=?", arrayOf(id))
    }

    fun saveBus(b: Bus) {
        val v = ContentValues()
        v.put("id", b.id); v.put("busNo", b.busNo); v.put("regNo", b.regNo); v.put("photo", b.photo)
        writableDatabase.insertWithOnConflict("buses", null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun removeBus(id: String) {
        writableDatabase.delete("buses", "id=?", arrayOf(id))
    }

    fun allBuses(): List<Bus> {
        val c = readableDatabase.rawQuery("SELECT id,busNo,regNo,photo FROM buses ORDER BY busNo", null)
        val out = ArrayList<Bus>()
        while (c.moveToNext()) {
            out.add(Bus(c.getString(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getString(3) ?: ""))
        }
        c.close()
        return out
    }

    fun searchRoutes(q: String): List<Route> {
        val like = "%$q%"
        val c = readableDatabase.rawQuery(
            "SELECT id,busNo,fromPlace,toPlace,type,stops FROM routes " +
                "WHERE busNo LIKE ? OR fromPlace LIKE ? OR toPlace LIKE ? OR stops LIKE ? ORDER BY busNo",
            arrayOf(like, like, like, like)
        )
        val out = ArrayList<Route>()
        while (c.moveToNext()) {
            out.add(Route(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5) ?: ""))
        }
        c.close()
        return out
    }

    fun tripsFor(routeId: String): List<Trip> {
        val c = readableDatabase.rawQuery(
            "SELECT id,time,arr,days,stops FROM timings WHERE routeId=? ORDER BY time", arrayOf(routeId)
        )
        val out = ArrayList<Trip>()
        while (c.moveToNext()) {
            out.add(Trip(c.getString(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getString(3) ?: "", c.getString(4) ?: ""))
        }
        c.close()
        return out
    }

    fun allTrips(): List<Pair<Route, Trip>> {
        val c = readableDatabase.rawQuery(
            "SELECT r.id,r.busNo,r.fromPlace,r.toPlace,r.type,r.stops,t.id,t.time,t.arr,t.days,t.stops " +
                "FROM timings t JOIN routes r ON r.id=t.routeId", null
        )
        val out = ArrayList<Pair<Route, Trip>>()
        while (c.moveToNext()) {
            val r = Route(c.getString(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getString(3) ?: "", c.getString(4) ?: "", c.getString(5) ?: "")
            val t = Trip(c.getString(6), c.getString(7) ?: "", c.getString(8) ?: "", c.getString(9) ?: "", c.getString(10) ?: "")
            out.add(Pair(r, t))
        }
        c.close()
        return out
    }

    fun stopNames(): List<String> {
        val c = readableDatabase.rawQuery("SELECT stops FROM routes", null)
        val set = TreeSet<String>()
        while (c.moveToNext()) {
            (c.getString(0) ?: "").split("|").forEach { if (it.isNotBlank()) set.add(it.trim()) }
        }
        c.close()
        return set.toList()
    }

    fun routeCount(): Int {
        val c = readableDatabase.rawQuery("SELECT COUNT(*) FROM routes", null)
        c.moveToFirst()
        val n = c.getInt(0)
        c.close()
        return n
    }
}
