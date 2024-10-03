package com.example.videoresolution.videoEdit.activity

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView

import android.widget.TextView
import android.widget.Toast
import com.example.videoresolution.videoEdit.util.ApiUtils
import com.example.videoresolution.R

class LoginActivity : AppCompatActivity() {

    private lateinit var farmsLoginDropdown: AutoCompleteTextView
    private lateinit var selectedFarm: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        val farmsItems = arrayOf("Sin fincas")
        val farmsAdapter = ArrayAdapter(this, R.layout.list_item, farmsItems)

        selectedFarm = intent.getStringExtra("selectedFarm") ?: ""

        farmsLoginDropdown = findViewById(R.id.dropdown_field_farms)
        farmsLoginDropdown.setAdapter(farmsAdapter)
        farmsLoginDropdown.setOnItemClickListener { _, _, position, _ -> selectedFarm = farmsLoginDropdown.adapter.getItem(position).toString()}

        findViewById<View>(R.id.syncButtonFarm).setOnClickListener {ApiUtils.getAndSetFarmsDropdown(this, farmsLoginDropdown)}

        findViewById<View>(R.id.buttonLogin).setOnClickListener {
            if (selectedFarm.isNotBlank() && selectedFarm != "Sin fincas") {
                val intent = Intent(this, LoginSecActivity::class.java)
                intent.putExtra("selectedFarm", selectedFarm)
                startActivity(intent)
            }
            else {
                showToastCustom(this, getString(R.string.login_toast_no_found_farms))
            }
        }
    }

    private fun showToastCustom(context: Context, msg: String) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val toast = Toast(context)
        val view: View = inflater.inflate(R.layout.custom_toast, null)
        val txtMessage = view.findViewById<TextView>(R.id.txtMensajeToast1)

        txtMessage.text = msg
        toast.setGravity(Gravity.CENTER_VERTICAL or Gravity.BOTTOM, 0, 200)
        toast.duration = Toast.LENGTH_LONG
        toast.view = view
        toast.show()
    }
}
