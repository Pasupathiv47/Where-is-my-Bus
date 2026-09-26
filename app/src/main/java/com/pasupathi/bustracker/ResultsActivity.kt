package com.pasupathi.bustracker

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import java.util.Calendar

class ResultsActivity : Activity() {
    private lateinit var db: DB
    private var hits: List<Hit> = emptyList()
    private var busPhotos: Map<String, String> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = DB(this)
        busPhotos = db.allBuses().associate { it.busNo to it.photo }
        hits = SearchState.hits

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL

        val header = TextView(this)
        header.text = "${SearchState.fromQ}  →  ${SearchState.toQ}"
        header.textSize = 18f
        header.setPadding(32, 48, 32, 24)
        header.setTextColor(0xFFFFFFFF.toInt())
        header.setBackgroundColor(0xFF1A237E.toInt())

        val list = ListView(this)
        list.adapter = ResultAdapter(this, hits, busPhotos)
        list.setOnItemClickListener { _, _, pos, _ ->
            SearchState.selected = hits[pos]
            startActivity(Intent(this, StopsActivity::class.java))
        }

        root.addView(header)
        if (hits.isEmpty()) {
            val empty = TextView(this)
            empty.text = "No buses found for today"
            empty.setPadding(32, 64, 32, 32)
            empty.gravity = Gravity.CENTER
            root.addView(empty)
        } else {
            root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        setContentView(root)
    }
}

class ResultAdapter(
    private val ctx: Activity,
    private val hits: List<Hit>,
    private val photos: Map<String, String>
) : BaseAdapter() {
    override fun getCount() = hits.size
    override fun getItem(i: Int) = hits[i]
    override fun getItemId(i: Int) = i.toLong()

    override fun getView(i: Int, convertView: View?, parent: ViewGroup?): View {
        val h = hits[i]
        val cal = Calendar.getInstance()
        val now = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        val isTime = h.fromTime.firstOrNull()?.isDigit() == true
        var isNext = false
        if (isTime && h.fromTime >= now) {
            isNext = hits.take(i).none { it.fromTime.firstOrNull()?.isDigit() == true && it.fromTime >= now }
        }

        val card = LinearLayout(ctx)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(32, 24, 32, 24)
        card.setBackgroundColor(if (isNext) 0xFFE8F0FE.toInt() else 0xFFFFFFFF.toInt())

        val topRow = LinearLayout(ctx)
        topRow.orientation = LinearLayout.HORIZONTAL

        val badge = TextView(ctx)
        badge.text = "  ${h.route.busNo}  "
        badge.setBackgroundColor(0xFF1A73E8.toInt())
        badge.setTextColor(0xFFFFFFFF.toInt())
        badge.textSize = 14f

        val dur = duration(h.fromTime, h.toTime)
        val times = TextView(ctx)
        times.text = "   ${h.fromTime}   —   $dur   —   ${h.toTime}"
        times.textSize = 16f
        times.setPadding(16, 0, 0, 0)

        topRow.addView(badge)
        topRow.addView(times)

        val nameRow = TextView(ctx)
        nameRow.text = "${h.route.type}${if (isNext) "  ·  NEXT ▸" else ""}"
        nameRow.textSize = 14f
        nameRow.setTextColor(if (isNext) 0xFF1A73E8.toInt() else 0xFF555555.toInt())
        nameRow.setPadding(0, 8, 0, 0)

        val stopsRow = TextView(ctx)
        stopsRow.text = "${h.fromName}  →  ${h.toName}   (${h.trip.days})"
        stopsRow.textSize = 13f
        stopsRow.setTextColor(0xFF777777.toInt())
        stopsRow.setPadding(0, 4, 0, 0)

        card.addView(topRow)
        card.addView(nameRow)
        card.addView(stopsRow)

        val photo = photos[h.route.busNo]
        val outer = LinearLayout(ctx)
        outer.orientation = LinearLayout.VERTICAL
        if (!photo.isNullOrBlank()) {
            val row = LinearLayout(ctx)
            row.orientation = LinearLayout.HORIZONTAL
            val img = ImageView(ctx)
            img.layoutParams = LinearLayout.LayoutParams(120, 120)
            try {
                val bytes = Base64.decode(photo.substringAfter(",", ""), Base64.DEFAULT)
                img.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            } catch (e: Exception) { }
            row.addView(img)
            row.addView(card)
            outer.addView(row)
        } else {
            outer.addView(card)
        }
        val divider = View(ctx)
        divider.setBackgroundColor(0xFFDDDDDD.toInt())
        outer.addView(divider, LinearLayout.LayoutParams(-1, 2))
        return outer
    }

    private fun duration(a: String, b: String): String {
        if (a.firstOrNull()?.isDigit() != true || b.firstOrNull()?.isDigit() != true) return "–"
        val (ah, am) = a.split(":").map { it.toInt() }
        val (bh, bm) = b.split(":").map { it.toInt() }
        var mins = (bh * 60 + bm) - (ah * 60 + am)
        if (mins < 0) mins += 1440
        return "${mins / 60}hr ${mins % 60}min"
    }
}
