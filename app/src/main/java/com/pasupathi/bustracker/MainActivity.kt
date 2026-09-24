package com.pasupathi.bustracker

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var db: DB
    private lateinit var status: TextView
    private lateinit var search: EditText
    private lateinit var list: ListView
    private var items: List<Route> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = DB(this)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(32, 64, 32, 32)

        status = TextView(this)
        search = EditText(this)
        search.hint = "Search bus number or any place"
        list = ListView(this)

        root.addView(status)
        root.addView(search)
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { refresh() }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        list.setOnItemClickListener { _, _, pos, _ -> showTrips(items[pos]) }

        refresh()
    }

    override fun onStart() {
        super.onStart()
        status.text = "Syncing..."
        Sync.run(this) { msg ->
            status.text = msg
            refresh()
        }
    }

    private fun refresh() {
        items = db.searchRoutes(search.text.toString().trim())
        val labels = items.map { r ->
            val mid = r.stops.split("|").filter { it.isNotBlank() }.drop(1).dropLast(1)
            val via = if (mid.isEmpty()) "" else "\nvia " + mid.joinToString(", ")
            "${r.busNo}   ${r.from} → ${r.to}  (${r.type})$via"
        }
        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
    }

    private fun showTrips(r: Route) {
        val trips = db.tripsFor(r.id)
        val title = "${r.busNo}  ${r.from} → ${r.to}"
        if (trips.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("No timings yet")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        val labels = trips.map { tripLabel(it) }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(labels) { _, i -> showStops(trips[i]) }
            .setPositiveButton("Close", null)
            .show()
    }

    private fun tripLabel(t: Trip): String {
        val times = if (t.arr.isBlank()) t.dep else "${t.dep} → ${t.arr}"
        return "$times   (${t.days})"
    }

    private fun showStops(t: Trip) {
        AlertDialog.Builder(this)
            .setTitle(tripLabel(t))
            .setMessage(if (t.stops.isBlank()) "No stop times added" else t.stops)
            .setPositiveButton("OK", null)
            .show()
    }
}
