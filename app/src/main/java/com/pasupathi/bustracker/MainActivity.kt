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
        search.hint = "Search bus number, from or to"
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

        list.setOnItemClickListener { _, _, pos, _ -> showTimings(items[pos]) }

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
        val labels = items.map { "${it.busNo}   ${it.from} → ${it.to}  (${it.type})" }
        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
    }

    private fun showTimings(r: Route) {
        val t = db.timingsFor(r.id)
        AlertDialog.Builder(this)
            .setTitle("${r.busNo}  ${r.from} → ${r.to}")
            .setMessage(if (t.isEmpty()) "No timings yet" else t.joinToString("\n"))
            .setPositiveButton("OK", null)
            .show()
    }
}
