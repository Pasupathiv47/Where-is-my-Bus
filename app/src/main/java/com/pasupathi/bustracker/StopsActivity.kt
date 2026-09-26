package com.pasupathi.bustracker

import android.app.Activity
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.view.View
import android.view.Gravity

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

        val scroll = HorizontalScrollView(this)
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(24, 48, 24, 48)

        for ((idx, s) in stops.withIndex()) {
            val (time, name) = s
            val isBoard = idx == h.fromIdx
            val isAlight = idx == h.toIdx
            val highlight = isBoard || isAlight

            val col = LinearLayout(this)
            col.orientation = LinearLayout.VERTICAL
            col.gravity = Gravity.CENTER_HORIZONTAL
            col.layoutParams = LinearLayout.LayoutParams(220, -2)

            val timeTv = TextView(this)
            timeTv.text = time
            timeTv.textSize = 14f
            timeTv.gravity = Gravity.CENTER
            timeTv.setTypeface(null, if (highlight) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            timeTv.setTextColor(if (highlight) 0xFF1A73E8.toInt() else 0xFF333333.toInt())

            val lineRow = LinearLayout(this)
            lineRow.orientation = LinearLayout.HORIZONTAL
            lineRow.gravity = Gravity.CENTER_VERTICAL

            val leftLine = View(this)
            leftLine.setBackgroundColor(0xFFBBBBBB.toInt())
            val rightLine = View(this)
            rightLine.setBackgroundColor(0xFFBBBBBB.toInt())

            val dot = TextView(this)
            dot.text = when {
                isBoard -> "▶"
                isAlight -> "■"
                else -> "●"
            }
            dot.textSize = if (highlight) 20f else 12f
            dot.setTextColor(if (highlight) 0xFF1A73E8.toInt() else 0xFF999999.toInt())
            dot.setPadding(8, 0, 8, 0)

            lineRow.addView(leftLine, LinearLayout.LayoutParams(0, 4, 1f))
            lineRow.addView(dot)
            lineRow.addView(rightLine, LinearLayout.LayoutParams(0, 4, 1f))
            if (idx == 0) leftLine.visibility = View.INVISIBLE
            if (idx == stops.size - 1) rightLine.visibility = View.INVISIBLE

            val nameTv = TextView(this)
            nameTv.text = name
            nameTv.textSize = 13f
            nameTv.gravity = Gravity.CENTER
            nameTv.setTypeface(null, if (highlight) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            nameTv.setTextColor(if (highlight) 0xFF1A73E8.toInt() else 0xFF555555.toInt())
            nameTv.setPadding(4, 8, 4, 0)

            col.addView(timeTv)
            col.addView(lineRow)
            col.addView(nameTv)
            row.addView(col)
        }

        scroll.addView(row)
        root.addView(scroll)
        setContentView(root)
    }
}
