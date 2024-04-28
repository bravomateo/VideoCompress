package com.example.videoresolution

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
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
                showToast("Sincronizar y seleccionar una finca válida")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
