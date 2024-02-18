package com.example.videoresolution

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class LoginSecActivity : AppCompatActivity() {
    private lateinit var selectedFarm: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_sec)

        // Get value passed from LoginActivity
        selectedFarm = intent.getStringExtra("selectedFarm") ?: ""

        val fab: FloatingActionButton = findViewById(R.id.floatingActionButton)
        fab.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("selectedFarm", selectedFarm)
            startActivity(intent)
        }
    }
}
