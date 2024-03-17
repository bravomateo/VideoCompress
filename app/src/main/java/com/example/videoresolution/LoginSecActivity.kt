package com.example.videoresolution
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton


class LoginSecActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var itemAdapter: ItemAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_sec)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val items = getListOfItemsFromDatabase() // Obtén la lista de items desde tu base de datos
        itemAdapter = ItemAdapter(items)
        recyclerView.adapter = itemAdapter


        // Configurar el botón flotante para abrir MainActivity sin enviar datos adicionales
        val fab: FloatingActionButton = findViewById(R.id.floatingActionButton)
        fab.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    // Método de ejemplo para obtener datos desde la base de datos
    private fun getListOfItemsFromDatabase(): List<Item> {
        // Aquí deberías usar ROOM para obtener la lista de items desde la base de datos
        // Por ejemplo, consultando la base de datos y obteniendo los datos a través de un LiveData o un Coroutine
        return listOf(
            Item(1, "Example 1: Title", "Description 1"),
            Item(2, "Example 2: Title", "Description 2"),
            Item(3, "Example 3: Title", "Description 3")
        )
    }
}

