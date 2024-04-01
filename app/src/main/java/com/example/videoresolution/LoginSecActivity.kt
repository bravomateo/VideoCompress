package com.example.videoresolution
import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
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

    class MyDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity?.let {
                val builder = AlertDialog.Builder(it)
                val inflater = requireActivity().layoutInflater
                val view = inflater.inflate(R.layout.dialog_block_status, null)

                val imageViewStatus = view.findViewById<ImageView>(R.id.imageViewStatus)

                builder.setView(view)
                    .setPositiveButton("Aceptar") { dialog, _ ->
                        dialog.dismiss()
                    }

                builder.create()
            } ?: throw IllegalStateException("Activity cannot be null")
        }

        override fun onResume() {
            super.onResume()
            updateImage(ApiUtils.BlockRequestStatus.LOADING)
        }

        fun updateImage(status: ApiUtils.BlockRequestStatus) {
            val imageViewStatus = dialog?.findViewById<ImageView>(R.id.imageViewStatus)
            imageViewStatus?.setImageResource(
                when (status) {
                    ApiUtils.BlockRequestStatus.LOADING -> R.drawable.load
                    ApiUtils.BlockRequestStatus.SUCCESS -> R.drawable.check
                    ApiUtils.BlockRequestStatus.ERROR -> R.drawable.error
                }
            )
        }
    }

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

        val fabSyncButton: FloatingActionButton = findViewById(R.id.floatingActionButtonSync)
        fabSyncButton.setOnClickListener {

            val dialogFragment = MyDialogFragment()
            dialogFragment.show(supportFragmentManager, "MyDialogFragment")

            getBlocks(this) { blockNumbers, status ->
                when (status) {
                    ApiUtils.BlockRequestStatus.LOADING -> {
                        Log.d("FabSyncButton", "Estado: CARGANDO")
                        dialogFragment.updateImage(status)
                    }

                    ApiUtils.BlockRequestStatus.SUCCESS -> {
                        Log.d("FabSyncButton", "Estado: ÉXITO")
                        if (blockNumbers != null) {
                            blocksList.addAll(blockNumbers)
                        }
                        dialogFragment.updateImage(status)
                    }

                    ApiUtils.BlockRequestStatus.ERROR -> {
                        Log.d("FabSyncButton", "Estado: ERROR")
                        dialogFragment.updateImage(status)
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
                showToast("Sincronizar para añadir un video")
            }
        }

    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}