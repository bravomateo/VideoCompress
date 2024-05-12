package com.example.videoresolution

import CustomTypefaceSpan
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class LoginActivity : AppCompatActivity() {

    private lateinit var farmsLoginDropdown: AutoCompleteTextView
    private lateinit var selectedFarm: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        selectedFarm = intent.getStringExtra("selectedFarm") ?: ""

        farmsLoginDropdown = findViewById(R.id.dropdown_field_farms)
        val farmsItems = arrayOf("Sin fincas")
        val farmsAdapter = ArrayAdapter(this, R.layout.list_item, farmsItems)

        farmsLoginDropdown.setAdapter(farmsAdapter)

        farmsLoginDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedFarm = farmsLoginDropdown.adapter.getItem(position).toString()
        }

        val syncButtonFarm: Button = findViewById(R.id.syncButtonFarm)
        syncButtonFarm.setOnClickListener {
            ApiUtils.getAndSetFarmsDropdown(this, farmsLoginDropdown)
        }

        val buttonLogin: Button = findViewById(R.id.buttonLogin)
        buttonLogin.setOnClickListener {
            if (selectedFarm.isNotBlank() && selectedFarm != "Sin fincas") {
                val intent = Intent(this, LoginSecActivity::class.java)
                intent.putExtra("selectedFarm", selectedFarm)
                startActivity(intent)
            } else {
                showToastCustom(this, "Sincronizar y seleccionar una finca válida.")

            }
        }
    }

    private fun showToastCustom(context: Context, msg: String?) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = inflater.inflate(R.layout.custom_toast,null)

        val txtMensaje = view.findViewById<TextView>(R.id.txtMensajeToast1)
        txtMensaje.text = msg

        val toast = Toast(context)
        toast.setGravity(Gravity.CENTER_VERTICAL or Gravity.BOTTOM, 0, 200)
        toast.duration = Toast.LENGTH_LONG
        toast.view = view
        toast.show()

    }

}
