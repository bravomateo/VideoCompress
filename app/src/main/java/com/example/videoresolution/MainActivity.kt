package com.example.videoresolution

import android.Manifest
import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.arthenica.mobileffmpeg.FFmpeg
import com.google.gson.annotations.SerializedName
import java.util.Calendar
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.File
import okhttp3.OkHttpClient
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch



class MainActivity : AppCompatActivity() {

    private val REQUEST_PERMISSION_CODE = 123
    private val REQUEST_VIDEO_CODE = 456


    private lateinit var blockDropdown: AutoCompleteTextView
    private lateinit var selectedBlock: String

    private lateinit var selectedFarm: String
    private var selectedOrientationDegrees = 0f



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val selectFileButton: Button = findViewById(R.id.selectFileButton)
        selectFileButton.setOnClickListener {
            checkPermissionsAndOpenFilePicker()
        }

        blockDropdown = findViewById(R.id.dropdown_field_blocks)
        val blocksItems = arrayOf("No Block")
        val blocksAdapter = ArrayAdapter(this, R.layout.list_item, blocksItems)
        blockDropdown.setAdapter(blocksAdapter)

        blockDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedBlock = blockDropdown.adapter.getItem(position).toString()
        }

        selectedFarm = "BC"


        val blocksList = intent.getStringArrayExtra("blocksList")?.mapNotNull { it }?.toTypedArray() ?: arrayOf()
        ApiUtils.setBlocksDropdown(this, blockDropdown, blocksList)

        val listVideosButton: ImageButton = findViewById(R.id.ListVideos)
        listVideosButton.setOnClickListener {
            val intent = Intent(this, LoginSecActivity::class.java)
            startActivity(intent)
        }
    }
    private fun checkPermissionsAndOpenFilePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openFilePicker()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_PERMISSION_CODE
                )
            }
        } else {
            openFilePicker()
        }
    }
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_VIDEO_CODE)
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openFilePicker()
            showToast(this,"Video seleccionado.")
        } else {
            showToast(this,"Permiso denegado. No se puede seleccionar el video.")
        }
    }

    private var currentRotationDegrees = 0f

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_VIDEO_CODE && resultCode == RESULT_OK && data != null) {
            val block = selectedBlock
            val selectedVideoUri: Uri = data.data!!
            val width = findViewById<EditText>(R.id.editTextWidth).text.toString()
            val height = findViewById<EditText>(R.id.editTextHeight).text.toString()
            val fps = findViewById<EditText>(R.id.editTextFPS).text.toString()
            val bed = findViewById<EditText>(R.id.editTextBed).text.toString()

            if (width.isNotEmpty() && height.isNotEmpty() && fps.isNotEmpty()) {

                val originalPath = getRealPathFromUri(selectedVideoUri)
                val outputDirectory =
                    File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                        "ReducedResolution"
                    )
                outputDirectory.mkdirs()


                val currentDateTime = obtenerFechaYHoraActual()
                val outputFilePath = File(outputDirectory, "${currentDateTime}.mp4").absolutePath
                showFirstFramePreview(selectedVideoUri, originalPath, outputFilePath, width, height, fps, selectedFarm, block, bed)

            } else {
                showToast(this,"Ingrese valores válidos para la resolución.")
            }

        } else {
            showToast(this,"No se seleccionó ningún video.")
        }
    }

    private fun showFirstFramePreview(videoUri: Uri, originalPath: String, outputFilePath: String, width: String, height: String, fps: String, farm: String, block: String, bed: String) {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, videoUri)
        val timeMicros = 0L
        val firstFrameBitmap = retriever.getFrameAtTime(timeMicros)


        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_preview, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.previewImageView)
        val rotateButton = dialogView.findViewById<Button>(R.id.rotateButton)
        val acceptButton = dialogView.findViewById<Button>(R.id.acceptButton)

        imageView.setImageBitmap(firstFrameBitmap)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        rotateButton.setOnClickListener {
            firstFrameBitmap?.let { bitmap ->
                selectedOrientationDegrees = (selectedOrientationDegrees + 90) % 360
                val rotatedBitmap = rotateBitmap(bitmap, selectedOrientationDegrees)
                imageView.setImageBitmap(rotatedBitmap)
            }
        }

        acceptButton.setOnClickListener {
            dialog.dismiss()
            Log.e("SelectedOrientation", "Valor de selectedOrientationDegrees: $selectedOrientationDegrees")
            showTrimVideoDialog(videoUri, originalPath, outputFilePath, width, height, fps, farm, block, bed)
        }

        dialog.show()
    }
    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun showTrimVideoDialog(videoUri: Uri, originalPath: String, outputFilePath: String, width: String, height: String, fps: String, farm: String, block: String, bed: String) {
        val dialogView = layoutInflater.inflate(R.layout.trip_video, null)
        val acceptButtonTrim = dialogView.findViewById<Button>(R.id.acceptButtonTrim)

        val trimSeekBar = dialogView.findViewById<SeekBar>(R.id.seekBar)
        val progressTextView = dialogView.findViewById<TextView>(R.id.progressTextView) // Obtener referencia al TextView
        val videoView = dialogView.findViewById<VideoView>(R.id.videoView) // Obtener referencia al VideoView

        val trimSeekEndBar = dialogView.findViewById<SeekBar>(R.id.seekEndBar)
        val progressEndTextView = dialogView.findViewById<TextView>(R.id.progressEndTextView) // Obtener referencia al TextView


        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, videoUri)
        val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val durationTime = durationString?.toInt() ?: 0 // Duración total del video en milisegundos

        videoView.setVideoURI(videoUri)
        videoView.setOnPreparedListener { mp ->
            mp.start()
        }

        trimSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                progressTextView.text = "${progress} seg"
                val valorbuscadoinicial = progress * 1000
                videoView.seekTo(valorbuscadoinicial)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                videoView.pause()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                videoView.start()
            }
        })

        trimSeekEndBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Actualizar el texto del TextView con el valor actual del progreso de la barra de búsqueda
                progressEndTextView.text = "${progress} seg"
                val valorbuscado = durationTime - progress * 1000
                // Imprimir el valor de endTime en el registro (log)
                Log.d("valorbuscado", "Valor buscado final: $valorbuscado")
                // Mover el video al segundo correspondiente al progreso de la barra de búsqueda
                videoView.seekTo(valorbuscado) // El progreso se da en segundos, por lo que se multiplica por 1000 para convertirlo a milisegundos

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                videoView.pause()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                videoView.start()
            }
        })

        acceptButtonTrim.setOnClickListener {
            dialog.dismiss()

            val startTime = trimSeekBar.progress
            val endTime = durationTime/1000 -trimSeekEndBar.progress

            Log.d("EndTime", "Valor de endTime: $endTime")

            videoView.stopPlayback()

            showToast(this, "Video guardado exitosamente.")

            selectedOrientationDegrees = 0f



            val resolution = "$width x $height"
            val videoName = obtenerFechaYHoraActual()
            updateInfoROOM( videoName,
                            resolution,
                            outputFilePath,
                            originalPath,
                            startTime,
                            endTime,
                            width,
                            height,
                            fps,
                            farm,
                            block,
                            bed
            )

        }

        dialog.show()
    }

    private fun obtenerFechaYHoraActual(): String {
        val calendario = Calendar.getInstance()
        val mes = calendario.get(Calendar.MONTH) + 1
        val dia = calendario.get(Calendar.DAY_OF_MONTH)
        val anio = calendario.get(Calendar.YEAR)
        val hora = calendario.get(Calendar.HOUR_OF_DAY)
        val minuto = calendario.get(Calendar.MINUTE)
        val segundo = calendario.get(Calendar.SECOND)

        val mesFormateado = if (mes < 10) "0$mes" else mes.toString()
        val diaFormateado = if (dia < 10) "0$dia" else dia.toString()
        val horaFormateada = if (hora < 10) "0$hora" else hora.toString()
        val minutoFormateado = if (minuto < 10) "0$minuto" else minuto.toString()
        val segundoFormateado = if (segundo < 10) "0$segundo" else segundo.toString()

        return "$diaFormateado-$mesFormateado-$anio-$horaFormateada-$minutoFormateado-$segundoFormateado"
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

    private fun getRealPathFromUri(uri: Uri): String {
        val contentResolver: ContentResolver = contentResolver
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        val columnIndex = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor?.moveToFirst()
        val path = columnIndex?.let { cursor.getString(it) } ?: ""
        cursor?.close()
        return path
    }

    private fun updateInfoROOM(
        videoName: String,
        resolution: String,
        outputFilePath: String,
        originalPath: String,
        startTime: Int,
        endTime: Int,
        width: String,
        height: String,
        fps: String,
        farm: String,
        block: String,
        bed: String
    )
    {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        ).build()

        val videoDao = db.videoDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val video = Video(  null, videoName, resolution, outputFilePath, originalPath, startTime, endTime, width, height, fps, farm, block, bed)
            videoDao.insertAll(video)
        }
    }


    data class YourResponseModelVideo(
        @SerializedName("error") val error: Boolean,
        @SerializedName("message") val message: MessageVideo
    )
    data class MessageVideo(
        @SerializedName("filename") val filename: String
    )
    data class YourResponseModel(
        @SerializedName("error") val error: Boolean,
        @SerializedName("message") val messages: List<Message>
    )
    data class Message(
        @SerializedName("farm") val farm: String,
        @SerializedName("block") val block: String,
        @SerializedName("bed") val bed: String,
        @SerializedName("video_name") val videoName: String,
        @SerializedName("resolution") val resolution: String,
        @SerializedName("FPS") val fps: String,
        @SerializedName("ID") val id: String
    )
    object RetrofitClient {
        // Samsung WIFI

        //private const val BASE_URL_UPLOAD = "http://192.168.206.45:8000"

        // Home WIFI
        private const val BASE_URL_UPLOAD = "http://192.168.58.104:8000"

        private const val BASE_URL_GET = "http://10.1.2.22:544"

        private val okHttpClient = OkHttpClient.Builder()
            .build()

        val instance: VideoService by lazy {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL_UPLOAD)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            retrofit.create(VideoService::class.java)
        }

        val instanceForGet: VideoService by lazy {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL_GET)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            retrofit.create(VideoService::class.java)
        }
    }

}
