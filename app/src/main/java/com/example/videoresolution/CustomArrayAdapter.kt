package com.example.videoresolution

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class CustomArrayAdapter(context: Context, resource: Int, items: Array<String>) : ArrayAdapter<String>(context, resource, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val textView = view.findViewById<TextView>(android.R.id.text1)

        // Aplica el tipo de letra personalizado
        val typeface = Typeface.createFromAsset(context.assets, "fonts/your_font_file.ttf")
        textView.typeface = typeface

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        val textView = view.findViewById<TextView>(android.R.id.text1)

        // Aplica el tipo de letra personalizado
        val typeface = Typeface.createFromAsset(context.assets, "font/sf_pro_display_bold.ttf")
        textView.typeface = typeface

        return view
    }
}
