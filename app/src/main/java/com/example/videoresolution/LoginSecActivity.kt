package com.example.videoresolution
import ItemAdapter
import android.app.Dialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
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

    private val viewModel = ViewModelProvider(this)[VideoViewModel::class.java]

    val videoStates: MutableMap<Int, VideoState> = mutableMapOf()

    class MyDialogFragment : DialogFragment() {

        private lateinit var imageViewStatus: ImageView
        private lateinit var progressBar: ProgressBar

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity?.let {
                val builder = AlertDialog.Builder(it)
                val inflater = requireActivity().layoutInflater
                val view = inflater.inflate(R.layout.dialog_block_status, null)

                imageViewStatus = view.findViewById(R.id.imageViewStatus)
                progressBar = view.findViewById(R.id.progressBar)

                builder.setView(view)
                    .setPositiveButton("Aceptar") { dialog, _ ->
                        dialog.dismiss()
                    }

                builder.create()
            } ?: throw IllegalStateException("Activity cannot be null")
        }

        override fun onResume() {
            super.onResume()
            updateStatus(ApiUtils.BlockRequestStatus.LOADING)
        }

        fun updateStatus(status: ApiUtils.BlockRequestStatus) {
            val imageViewStatus = dialog?.findViewById<ImageView>(R.id.imageViewStatus)
            imageViewStatus?.setImageResource(
                when (status) {
                    ApiUtils.BlockRequestStatus.LOADING -> {
                        imageViewStatus.visibility = View.INVISIBLE // Ocultar la imagen
                        progressBar.visibility = View.VISIBLE // Mostrar la barra de progreso
                        R.drawable.load // No importa qué imagen se establece aquí, ya que se oculta
                    }
                    ApiUtils.BlockRequestStatus.SUCCESS -> {
                        imageViewStatus.visibility = View.VISIBLE // Mostrar la imagen
                        progressBar.visibility = View.INVISIBLE // Ocultar la barra de progreso
                        R.drawable.check
                    }
                    ApiUtils.BlockRequestStatus.ERROR -> {
                        imageViewStatus.visibility = View.VISIBLE // Mostrar la imagen
                        progressBar.visibility = View.INVISIBLE // Ocultar la barra de progreso
                        R.drawable.error
                    }
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
            val videoStates: MutableMap<Int, VideoState> = mutableMapOf()
            val videos = videoDao.getAll()

            runOnUiThread {
                itemAdapter = ItemAdapter(this@LoginSecActivity, videoStates, viewModel, videos) { position ->
                    val clickedItem = videos[position]
                    val outputFilePath: String = clickedItem.outputFilePath!!
                    val startTime: Int = clickedItem.startTime!!
                    val endTime: Int = clickedItem.endTime!!
                    val originalPath: String = clickedItem.originalPath!!
                    val width: String = clickedItem.width!!
                    val height: String = clickedItem.height!!
                    val fps: String = clickedItem.fps!!

                    Log.d("MainActivityVideos", "The position selected is $position")

                    showToast(this@LoginSecActivity, "Subiendo el video: ${clickedItem.nameVideo}.")

                    itemAdapter.uploadVideo(position, applicationContext, outputFilePath, startTime, endTime, originalPath, width, height, fps, viewModel)

                }
                recyclerView.adapter = itemAdapter
            }
        }


        val blocksList = mutableListOf<String>()

        val fabSyncButton: FloatingActionButton = findViewById(R.id.floatingActionButtonSync)
        fabSyncButton.setOnClickListener {

            val dialogFragment = MyDialogFragment()
            dialogFragment.show(supportFragmentManager, "MyDialogFragment")

            getBlocks(this) { blockNumbers, status ->
                when (status) {
                    ApiUtils.BlockRequestStatus.LOADING -> {
                        Log.d("FabSyncButton", "Estado: CARGANDO")
                        dialogFragment.updateStatus(status)
                    }

                    ApiUtils.BlockRequestStatus.SUCCESS -> {
                        Log.d("FabSyncButton", "Estado: ÉXITO")
                        if (blockNumbers != null) {
                            blocksList.addAll(blockNumbers)
                        }
                        dialogFragment.updateStatus(status)
                    }

                    ApiUtils.BlockRequestStatus.ERROR -> {
                        Log.d("FabSyncButton", "Estado: ERROR")
                        dialogFragment.updateStatus(status)
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
                showToast(this, "Sincronizar para añadir un video.")
            }
        }
    }

    private fun showToast(context: Context, msg: String?) {
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