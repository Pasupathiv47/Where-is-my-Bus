package com.pasupathi.bustracker

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore

object Sync {
    fun run(ctx: Context, done: (String) -> Unit) {
        val db = DB(ctx)
        db.writableDatabase
        val prefs = ctx.getSharedPreferences("sync", Context.MODE_PRIVATE)
        val fs = FirebaseFirestore.getInstance()
        val lastR = prefs.getLong("routes", 0L)
        val lastT = prefs.getLong("timings", 0L)
        val fail = { done("Offline - showing saved data") }

        fs.collection("routes").whereGreaterThan("updatedAt", lastR).get()
            .addOnSuccessListener { rs ->
                var max = lastR
                for (d in rs) {
                    val u = d.getLong("updatedAt") ?: 0L
                    if (u > max) max = u
                    if (d.getBoolean("deleted") == true) {
                        db.removeRoute(d.id)
                    } else {
                        val stops = (d.get("stops") as? List<*>)
                            ?.joinToString("|") { it.toString() } ?: ""
                        db.saveRoute(
                            Route(
                                d.id,
                                d.getString("busNo") ?: "",
                                d.getString("from") ?: "",
                                d.getString("to") ?: "",
                                d.getString("type") ?: "",
                                stops
                            )
                        )
                    }
                }
                prefs.edit().putLong("routes", max).apply()

                fs.collection("timings").whereGreaterThan("updatedAt", lastT).get()
                    .addOnSuccessListener { ts ->
                        var maxT = lastT
                        for (d in ts) {
                            val u = d.getLong("updatedAt") ?: 0L
                            if (u > maxT) maxT = u
                            if (d.getBoolean("deleted") == true) {
                                db.removeTiming(d.id)
                            } else {
                                val lines = (d.get("stops") as? List<*>)?.mapNotNull { item ->
                                    val m = item as? Map<*, *> ?: return@mapNotNull null
                                    val name = m["n"]?.toString() ?: ""
                                    val t = m["t"]?.toString() ?: ""
                                    (if (t.isBlank()) "--:--" else t) + "   " + name
                                }?.joinToString("\n") ?: ""
                                db.saveTiming(
                                    d.id,
                                    d.getString("routeId") ?: "",
                                    d.getString("time") ?: "",
                                    d.getString("arr") ?: "",
                                    d.getString("days") ?: "",
                                    lines
                                )
                            }
                        }
                        prefs.edit().putLong("timings", maxT).apply()
                        done("Up to date · ${db.routeCount()} routes")
                    }
                    .addOnFailureListener { fail() }
            }
            .addOnFailureListener { fail() }
    }
}
