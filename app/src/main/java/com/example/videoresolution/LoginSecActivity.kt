package com.example.videoresolution
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.videoresolution.ApiUtils.getBlocks
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginSecActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var itemAdapter: ItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_sec)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        ).build()

        val videoDao = db.videoDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val videos = videoDao.getAll()
            Log.d("MainActivityVideos", "Videos: ${videos.toString()}")
            itemAdapter = ItemAdapter(videos)
            recyclerView.adapter = itemAdapter
        }


        // Define una variable de lista fuera de las funciones
        val blocksList = mutableListOf<String>()

        // Llamada a getBlocks desde fabSyncButton y almacenamiento de los bloques en la lista
        val fabSyncButton: FloatingActionButton = findViewById(R.id.floatingActionButtonSync)
        fabSyncButton.setOnClickListener {
            getBlocks(this) { blockNumbers, error ->
                if (error == null) {
                    if (blockNumbers != null) {
                        blocksList.addAll(blockNumbers)
                    }
                }
            }
        }

        val fab: FloatingActionButton = findViewById(R.id.floatingActionButtonAddVideos)
        fab.setOnClickListener {
            if (blocksList.isNotEmpty()) {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("blocksList", blocksList.toTypedArray())
                startActivity(intent)
            } else {
                showToast("Sincronizars para adicionar un video")
            }
        }

    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}