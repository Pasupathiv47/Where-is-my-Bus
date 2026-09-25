package com.pasupathi.bustracker

import android.app.Activity
import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

class BusesActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = DB(this)
        val buses = db.allBuses()

        val list = ListView(this)
        list.adapter = object : BaseAdapter() {
            override fun getCount() = buses.size
            override fun getItem(i: Int) = buses[i]
            override fun getItemId(i: Int) = i.toLong()
            override fun getView(i: Int, convertView: View?, parent: ViewGroup?): View {
                val b = buses[i]
                val row = LinearLayout(this@BusesActivity)
                row.orientation = LinearLayout.HORIZONTAL
                row.setPadding(24, 24, 24, 24)

                val img = ImageView(this@BusesActivity)
                img.layoutParams = LinearLayout.LayoutParams(160, 160)
                val b64 = b.photo.substringAfter(",", "")
                if (b64.isNotBlank()) {
                    try {
                        val bytes = Base64.decode(b64, Base64.DEFAULT)
                        img.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                    } catch (e: Exception) { }
                }

                val col = LinearLayout(this@BusesActivity)
                col.orientation = LinearLayout.VERTICAL
                col.setPadding(24, 0, 0, 0)
                val name = TextView(this@BusesActivity)
                name.text = b.busNo
                name.textSize = 18f
                val reg = TextView(this@BusesActivity)
                reg.text = "${b.regNo} · ${b.type}"
                col.addView(name)
                col.addView(reg)

                row.addView(img)
                row.addView(col)
                return row
            }
        }
        list.setOnItemClickListener { _, _, pos, _ ->
            val b = buses[pos]
            if (b.photo.isBlank()) return@setOnItemClickListener
            val iv = ImageView(this)
            iv.adjustViewBounds = true
            iv.setPadding(32, 32, 32, 32)
            try {
                val bytes = Base64.decode(b.photo.substringAfter(",", ""), Base64.DEFAULT)
                iv.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            } catch (e: Exception) { }
            AlertDialog.Builder(this)
                .setTitle("${b.busNo}  ${b.regNo}")
                .setView(iv)
                .setPositiveButton("Close", null)
                .show()
        }
        setContentView(list)
    }
}
