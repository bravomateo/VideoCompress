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
    lateinit var itemAdapter: ItemAdapter


    class MyDialogFragment : DialogFragment() {

        private lateinit var imageViewStatus: ImageView
        private lateinit var progressBar: ProgressBar

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity?.let {

                val builder = AlertDialog.Builder(it, R.style.CustomDialogTheme)
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
                        imageViewStatus.visibility = View.INVISIBLE
                        progressBar.visibility = View.VISIBLE
                        R.drawable.load
                    }
                    ApiUtils.BlockRequestStatus.SUCCESS -> {
                        imageViewStatus.visibility = View.VISIBLE
                        progressBar.visibility = View.INVISIBLE
                        R.drawable.check
                    }
                    ApiUtils.BlockRequestStatus.ERROR -> {
                        imageViewStatus.visibility = View.VISIBLE
                        progressBar.visibility = View.INVISIBLE
                        R.drawable.error
                    }
                }
            )
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_sec)

        val blocksListGet = intent.getStringArrayExtra("blocksList")?.mapNotNull { it }?.toTypedArray() ?: arrayOf()


        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val fabSyncButton: FloatingActionButton = findViewById(R.id.floatingActionButtonSync)
        val fab: FloatingActionButton = findViewById(R.id.floatingActionButtonAddVideos)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        ).build()

        val videoDao = db.videoDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val videos = videoDao.getAll()

            runOnUiThread {
                itemAdapter = ItemAdapter(this@LoginSecActivity, videos) { position ->
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

                    itemAdapter.uploadVideo(this@LoginSecActivity, position, this@LoginSecActivity, outputFilePath, startTime, endTime, originalPath, width, height, fps)

                }
                recyclerView.adapter = itemAdapter


                if (itemAdapter.isUploading()) {
                    fabSyncButton.isEnabled = false
                    fab.isEnabled = false
                }

            }
        }


        val blocksList = blocksListGet?.toMutableList() ?: mutableListOf()



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