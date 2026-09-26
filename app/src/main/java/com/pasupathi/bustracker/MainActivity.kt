package com.pasupathi.bustracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

data class Hit(
    val route: Route,
    val trip: Trip,
    val fromTime: String,
    val fromName: String,
    val toTime: String,
    val toName: String,
    val fromIdx: Int,
    val toIdx: Int
)

object SearchState {
    var hits: List<Hit> = emptyList()
    var fromQ: String = ""
    var toQ: String = ""
}

class MainActivity : Activity() {
    private lateinit var db: DB
    private lateinit var status: TextView
    private lateinit var fromBox: AutoCompleteTextView
    private lateinit var toBox: AutoCompleteTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = DB(this)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(32, 64, 32, 32)

        status = TextView(this)

        fromBox = AutoCompleteTextView(this)
        fromBox.hint = "From (starting point)"
        fromBox.threshold = 1
        fromBox.setSingleLine(true)

        toBox = AutoCompleteTextView(this)
        toBox.hint = "To (destination)"
        toBox.threshold = 1
        toBox.setSingleLine(true)

        val findBtn = Button(this)
        findBtn.text = "🔍  Find Bus"
        findBtn.setOnClickListener { search() }

        val swap = Button(this)
        swap.text = "⇅  Swap"
        swap.setOnClickListener {
            val a = fromBox.text.toString()
            fromBox.setText(toBox.text.toString(), false)
            toBox.setText(a, false)
        }

        root.addView(status)
        root.addView(fromBox)
        root.addView(toBox)
        root.addView(findBtn)
        root.addView(swap)
        setContentView(root)

        refreshNames()
    }

    override fun onStart() {
        super.onStart()
        status.text = "Syncing..."
        Sync.run(this) { msg ->
            status.text = msg
            refreshNames()
        }
    }

    private fun refreshNames() {
        val names = db.stopNames()
        fromBox.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, names))
        toBox.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, names))
    }

    private fun parseStops(s: String): List<Pair<String, String>> =
        s.lines().filter { it.isNotBlank() }.map { l ->
            val i = l.indexOf("   ")
            if (i < 0) Pair("--:--", l.trim()) else Pair(l.substring(0, i), l.substring(i + 3).trim())
        }

    private fun findHits(from: String, to: String): List<Hit> {
        val f = from.lowercase()
        val t = to.lowercase()
        val sunday = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.SUNDAY
        val out = ArrayList<Hit>()

        for ((r, trip) in db.allTrips()) {
            if (trip.days == "Sunday" && !sunday) continue
            if (trip.days == "Mon-Sat" && sunday) continue

            val stops = parseStops(trip.stops)
            if (stops.size < 2) continue

            var fi = 0
            if (f.isNotEmpty()) {
                fi = stops.indexOfFirst { it.second.lowercase().contains(f) }
                if (fi < 0) continue
            }

            var ti = stops.size - 1
            if (t.isNotEmpty()) {
                val found = (fi + 1 until stops.size)
                    .firstOrNull { stops[it].second.lowercase().contains(t) } ?: continue
                ti = found
            } else if (fi == stops.size - 1) {
                continue
            }

            out.add(
                Hit(
                    r, trip,
                    stops[fi].first, stops[fi].second,
                    stops[ti].first, stops[ti].second,
                    fi, ti
                )
            )
        }

        out.sortBy { if (it.fromTime.firstOrNull()?.isDigit() == true) it.fromTime else "99:99" }
        return out
    }

    private fun search() {
        val f = fromBox.text.toString().trim()
        val t = toBox.text.toString().trim()
        if (f.isEmpty() && t.isEmpty()) return

        SearchState.hits = findHits(f, t)
        SearchState.fromQ = if (f.isNotEmpty()) f else "Anywhere"
        SearchState.toQ = if (t.isNotEmpty()) t else "Anywhere"
        startActivity(Intent(this, ResultsActivity::class.java))
    }
}
