package com.example.videoresolution
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LoginSecActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_sec)


        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        ).build()

        val userDao = db.userDao()

        // Obtener todos los usuarios y mostrarlos en el Logcat
        lifecycleScope.launch(Dispatchers.IO) {
            val users = userDao.getAll()
            users.forEach { user ->
                Log.d("MainActivity", "User: ${user.firstName} ${user.lastName}")
            }
        }


        // Configurar el botón flotante para abrir MainActivity sin enviar datos adicionales
        val fab: FloatingActionButton = findViewById(R.id.floatingActionButton)
        fab.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
