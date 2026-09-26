package com.pasupathi.bustracker

import android.app.Activity
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class StopsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val h = SearchState.selected
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL

        val header = TextView(this)
        header.text = if (h != null) "${h.route.busNo}   ${h.route.from} → ${h.route.to}" else "Trip"
        header.textSize = 18f
        header.setTextColor(0xFFFFFFFF.toInt())
        header.setBackgroundColor(0xFF1A237E.toInt())
        header.setPadding(32, 48, 32, 24)
        root.addView(header)

        if (h == null) { setContentView(root); return }

        val db = DB(this)
        val photo = db.allBuses().find { it.busNo == h.route.busNo }?.photo
        if (!photo.isNullOrBlank()) {
            val img = ImageView(this)
            img.adjustViewBounds = true
            img.setPadding(32, 24, 32, 0)
            try {
                val bytes = Base64.decode(photo.substringAfter(",", ""), Base64.DEFAULT)
                img.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            } catch (e: Exception) { }
            root.addView(img)
        }

        val lines = h.trip.stops.lines().filter { it.isNotBlank() }
        val stops = lines.map { l ->
            val i = l.indexOf("   ")
            if (i < 0) Pair("--:--", l.trim()) else Pair(l.substring(0, i), l.substring(i + 3).trim())
        }

        val scroll = ScrollView(this)
        val list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL
        list.setPadding(32, 24, 32, 48)

        for ((idx, s) in stops.withIndex()) {
            val (time, name) = s
            val isBoard = idx == h.fromIdx
            val isAlight = idx == h.toIdx
            val highlight = isBoard || isAlight

            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(0, 20, 0, 20)

            val railCol = LinearLayout(this)
            railCol.orientation = LinearLayout.VERTICAL
            railCol.gravity = Gravity.CENTER_HORIZONTAL
            railCol.layoutParams = LinearLayout.LayoutParams(80, -2)

            val topLine = View(this)
            topLine.setBackgroundColor(0xFFBBBBBB.toInt())
            val bottomLine = View(this)
            bottomLine.setBackgroundColor(0xFFBBBBBB.toInt())

            val dot = TextView(this)
            dot.text = when {
                isBoard -> "▶"
                isAlight -> "■"
                else -> "●"
            }
            dot.textSize = if (highlight) 18f else 11f
            dot.setTextColor(if (highlight) 0xFF1A73E8.toInt() else 0xFF999999.toInt())
            dot.gravity = Gravity.CENTER

            if (idx > 0) railCol.addView(topLine, LinearLayout.LayoutParams(4, 30))
            else railCol.addView(View(this), LinearLayout.LayoutParams(4, 30))
            railCol.addView(dot)
            if (idx < stops.size - 1) railCol.addView(bottomLine, LinearLayout.LayoutParams(4, 30))
            else railCol.addView(View(this), LinearLayout.LayoutParams(4, 30))

            val textCol = LinearLayout(this)
            textCol.orientation = LinearLayout.VERTICAL
            textCol.setPadding(24, 0, 0, 0)

            val nameTv = TextView(this)
            nameTv.text = name
            nameTv.textSize = 16f
            nameTv.setTypeface(null, if (highlight) Typeface.BOLD else Typeface.NORMAL)
            nameTv.setTextColor(if (highlight) 0xFF1A73E8.toInt() else 0xFF222222.toInt())

            val timeTv = TextView(this)
            timeTv.text = time
            timeTv.textSize = 14f
            timeTv.setTextColor(0xFF777777.toInt())

            textCol.addView(nameTv)
            textCol.addView(timeTv)

            row.addView(railCol)
            row.addView(textCol)
            list.addView(row)
        }

        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }
}
