package com.pasupathi.bustracker

import android.app.Activity
import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import java.util.Calendar

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

class MainActivity : Activity() {
    private lateinit var db: DB
    private lateinit var status: TextView
    private lateinit var fromBox: AutoCompleteTextView
    private lateinit var toBox: AutoCompleteTextView
    private lateinit var list: ListView
    private var hits: List<Hit> = emptyList()
    private var busPhotos: Map<String, String> = emptyMap()

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

        list = ListView(this)

        root.addView(status)
        root.addView(fromBox)
        root.addView(toBox)
        root.addView(findBtn)
        root.addView(swap)
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        list.setOnItemClickListener { _, _, pos, _ ->
            if (pos < hits.size) showHit(hits[pos])
        }

        refreshNames()
        showPrompt()
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
        busPhotos = db.allBuses().associate { it.busNo to it.photo }
    }

    private fun photoFor(busNo: String): String? {
        val p = busPhotos[busNo]
        return if (p.isNullOrBlank()) null else p
    }

    private fun showPrompt() {
        hits = emptyList()
        list.adapter = PhotoRowAdapter(this, listOf(Pair("Enter From / To and tap Find Bus", null)))
    }

    private fun search() {
        val f = fromBox.text.toString().trim()
        val t = toBox.text.toString().trim()
        if (f.isEmpty() && t.isEmpty()) {
            showPrompt()
            return
        }

        hits = findHits(f, t)
        val rows = ArrayList<Pair<String, String?>>()
        var nextPos = -1
        val cal = Calendar.getInstance()
        val now = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        for ((idx, h) in hits.withIndex()) {
            val isTime = h.fromTime.firstOrNull()?.isDigit() == true
            val next = nextPos < 0 && isTime && h.fromTime >= now
            if (next) nextPos = idx
            val head = "${h.fromTime}  ${h.fromName}  →  ${h.toTime}  ${h.toName}"
            val sub = "${h.route.busNo} · ${h.route.type} · ${h.trip.days}"
            rows.add(Pair((if (next) "NEXT ▸ " else "") + head + "\n" + sub, photoFor(h.route.busNo)))
        }
        if (hits.isEmpty()) rows.add(Pair("No buses found for today", null))

        list.adapter = PhotoRowAdapter(this, rows)
        if (nextPos > 0) list.setSelection(nextPos)
    }

    private fun parseStops(s: String): List<Pair<String, String>> =
        s.lines().filter { it.isNotBlank() }.map { l ->
            val i = l.indexOf("   ")
            if (i < 0) Pair("--:--", l.trim()) else Pair(l.substring(0, i), l.substring(i + 3).trim())
        }

    private fun findHits(from: String, to: String): List<Hit> {
        val f = from.lowercase()
        val t = to.lowercase()
        val sunday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
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

    private fun showHit(h: Hit) {
        val lines = h.trip.stops.lines().filter { it.isNotBlank() }
        val body = lines.mapIndexed { i, l ->
            when (i) {
                h.fromIdx -> "▶ $l"
                h.toIdx -> "■ $l"
                else -> "   $l"
            }
        }.joinToString("\n")
        val d = AlertDialog.Builder(this)
            .setTitle("${h.route.busNo}  ${h.route.from} → ${h.route.to}")
            .setMessage(body)
            .setPositiveButton("OK", null)
        photoFor(h.route.busNo)?.let { d.setView(photoView(it)) }
        d.show()
    }

    private fun photoView(dataUrl: String): ImageView {
        val iv = ImageView(this)
        iv.adjustViewBounds = true
        iv.setPadding(32, 16, 32, 0)
        try {
            val b64 = dataUrl.substringAfter(",", "")
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            iv.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
        } catch (e: Exception) { }
        return iv
    }
}

class PhotoRowAdapter(
    private val ctx: Activity,
    private val rows: List<Pair<String, String?>>
) : BaseAdapter() {
    override fun getCount() = rows.size
    override fun getItem(i: Int) = rows[i]
    override fun getItemId(i: Int) = i.toLong()
    override fun getView(i: Int, convertView: View?, parent: ViewGroup?): View {
        val (text, photo) = rows[i]
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(8, 24, 8, 24)

        if (photo != null) {
            val img = ImageView(ctx)
            img.layoutParams = LinearLayout.LayoutParams(140, 140)
            try {
                val b64 = photo.substringAfter(",", "")
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                img.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            } catch (e: Exception) { }
            row.addView(img)
        }

        val tv = TextView(ctx)
        tv.text = text
        tv.textSize = 16f
        tv.setPadding(if (photo != null) 24 else 0, 0, 0, 0)
        row.addView(tv)
        return row
    }
}
