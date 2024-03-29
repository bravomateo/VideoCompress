package com.example.videoresolution

import android.Manifest
import android.app.ProgressDialog
import android.content.ContentResolver
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
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private val REQUEST_PERMISSION_CODE = 123
    private val REQUEST_VIDEO_CODE = 456
    private lateinit var progressDialog: ProgressDialog

    private lateinit var blockDropdown: AutoCompleteTextView
    private lateinit var selectedBlock: String

    private lateinit var selectedFarm: String
    private var selectedOrientationDegrees = 0f

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)




        val selectFileButton: Button = findViewById(R.id.selectFileButton)
        selectFileButton.setOnClickListener {
            checkPermissionsAndOpenFilePicker()
        }

        blockDropdown = findViewById(R.id.dropdown_field_blocks)
        val blocksItems = arrayOf("No Block")
        val blocksAdapter =
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, blocksItems)
        blockDropdown.setAdapter(blocksAdapter)

        blockDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedBlock = blockDropdown.adapter.getItem(position).toString()
        }

        selectedFarm = intent.getStringExtra("selectedFarm") ?: ""

        val syncButton: Button = findViewById(R.id.syncButton)
        syncButton.setOnClickListener {
            // Show block dropdown
            ApiUtils.getAndSetBlocksDropdown(this, blockDropdown)
        }

        val listVideosButton: Button = findViewById(R.id.ListVideos)
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
            showToast("Video seleccionado.")
        } else {
            showToast("Permiso denegado. No se puede seleccionar el video.")
        }
    }

    private var currentRotationDegrees = 0f // Variable para almacenar el ángulo de rotación actual

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_VIDEO_CODE && resultCode == RESULT_OK && data != null) {
            val block = selectedBlock
            val selectedVideoUri: Uri = data.data!!
            val width = findViewById<EditText>(R.id.editTextWidth).text.toString()
            val height = findViewById<EditText>(R.id.editTextHeight).text.toString()
            val fps = findViewById<EditText>(R.id.editTextFPS).text.toString()

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

                progressDialog = ProgressDialog.show(this, "Procesando", "Convirtiendo video...", true, false)

                // Llama directamente a la selección de orientación
                showFirstFramePreview(selectedVideoUri, originalPath, outputFilePath, width, height, fps)


            } else {
                showToast("Ingrese valores válidos para la resolución.")
            }

        } else {
            showToast("No se seleccionó ningún video.")
        }
    }

    private fun showFirstFramePreview(videoUri: Uri, originalPath: String, outputFilePath: String, width: String, height: String, fps: String) {
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
            dialog.dismiss()  // Cerrar el cuadro de diálogo al hacer clic en "Aceptar"

            // Imprimir el valor de selectedOrientationDegrees
            Log.e("SelectedOrientation", "Valor de selectedOrientationDegrees: $selectedOrientationDegrees")

            showTrimVideoDialog(videoUri, originalPath, outputFilePath, width, height, fps)

        }

        dialog.show()
    }

    private fun showTrimVideoDialog(videoUri: Uri, originalPath: String, outputFilePath: String, width: String, height: String, fps: String) {
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


        // Duracion total del video
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, videoUri)
        val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val durationTime = durationString?.toInt() ?: 0 // Duración total del video en milisegundos

        videoView.setVideoURI(videoUri)
        videoView.setOnPreparedListener { mp ->
            // Iniciar la reproducción del video cuando esté preparado
            mp.start()
        }

        trimSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Actualizar el texto del TextView con el valor actual del progreso de la barra de búsqueda
                progressTextView.text = "${progress} seg"
                val valorbuscadoinicial = progress * 1000
                Log.d("valorbuscado", "Valor buscado inicial: $valorbuscadoinicial")
                // Mover el video al segundo correspondiente al progreso de la barra de búsqueda
                videoView.seekTo(valorbuscadoinicial) // El progreso se da en segundos, por lo que se multiplica por 1000 para convertirlo a milisegundos
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Pausar la reproducción del video cuando se toca la barra de búsqueda
                videoView.pause()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Reanudar la reproducción del video cuando se suelta la barra de búsqueda
                videoView.start()
            }
        })



        // Configurar la segunda barra de búsqueda (final del video)
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
                // Pausar la reproducción del video cuando se toca la barra de búsqueda
                videoView.pause()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Reanudar la reproducción del video cuando se suelta la barra de búsqueda
                videoView.start()
            }
        })

        acceptButtonTrim.setOnClickListener {
            dialog.dismiss()



            // Guardar el valor seleccionado en la barra de progreso como startTime
            val startTime = trimSeekBar.progress
            val endTime = durationTime/1000 -trimSeekEndBar.progress

            // Imprimir el valor de endTime en el registro (log)
            Log.d("EndTime", "Valor de endTime: $endTime")

            // Detener la reproducción del video al cerrar el diálogo
            videoView.stopPlayback()


            // Iniciar el proceso de conversión de video con el nuevo valor de startTime
            VideoConversionTask(outputFilePath, selectedOrientationDegrees, startTime, endTime).execute(
                originalPath,
                outputFilePath,
                width,
                height,
                fps
            )
            selectedOrientationDegrees = 0f
        }

        dialog.show()
    }

    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private inner class VideoConversionTask(
        private val outputPath: String,
        private val orientationDegrees: Float,
        private val startTime: Int,
        private val endTime: Int
    ) : AsyncTask<String, Void, Int>() {

        override fun doInBackground(vararg params: String?): Int {
            val inputPath = params[0] ?: ""
            val width = params[2] ?: ""
            val height = params[3] ?: ""
            val fps = params[4] ?: ""

            val tempOutputPath = getTempFilePath()
            val frameReductionCommand = arrayOf(
                "-i", inputPath,
                "-ss", startTime.toString(), // tiempo inicial en segundos
                "-t", (endTime - startTime).toString(), // duración del recorte en segundos
                "-r", fps,
                tempOutputPath
            )

            val frameReductionResult = FFmpeg.execute(frameReductionCommand)
            if (frameReductionResult != 0) {
                return frameReductionResult
            }


            val rotatedTempOutputPath = getTempFilePath()
            val rotationCommand = when (orientationDegrees) {
                90.0f  -> arrayOf("-i", tempOutputPath, "-vf", "transpose=3", rotatedTempOutputPath)
                180.0f -> arrayOf("-i", tempOutputPath, "-vf", "transpose=2,transpose=2", rotatedTempOutputPath)
                270.0f -> arrayOf("-i", tempOutputPath, "-vf", "transpose=2", rotatedTempOutputPath)
                0.0f   -> arrayOf("-i", tempOutputPath,  rotatedTempOutputPath)
                else   -> arrayOf("-i", tempOutputPath, "-vf", rotatedTempOutputPath)
            }

            val rotationResult = FFmpeg.execute(rotationCommand)
            if (rotationResult != 0) {
                return rotationResult
            }

            val resolutionReductionCommand = arrayOf(
                "-i", rotatedTempOutputPath, // Use the rotated video
                "-vf", "scale=$width:$height",
                "-b:v", "10K",
                "-an",
                outputPath
            )
            return FFmpeg.execute(resolutionReductionCommand)
        }


        override fun onPostExecute(result: Int) {
            progressDialog.dismiss()

            if (result == 0) {
                showToast("Video procesado exitosamente")

                val bed = findViewById<EditText>(R.id.editTextBed).text.toString()
                val width = findViewById<EditText>(R.id.editTextWidth).text.toString()
                val height = findViewById<EditText>(R.id.editTextHeight).text.toString()
                val resolution = "$width x $height"
                val fps = findViewById<EditText>(R.id.editTextFPS).text.toString()

                val id = UUID.randomUUID().toString()
                val videoName = obtenerFechaYHoraActual()

                uploadInfoToServer(selectedFarm, selectedBlock, bed, videoName, resolution, fps, id)

                // Subir el video procesado al servidor
                val processedVideoFile = File(outputPath)
                uploadVideoToServer(processedVideoFile)


            } else {
                showToast("Error al procesar el video")
            }
        }

        private fun getTempFilePath(): String {
            val outputDirectory = File(cacheDir, "Temp")
            outputDirectory.mkdirs()
            val currentDateTime = obtenerFechaYHoraActual()
            return File(outputDirectory, "${currentDateTime}_temp.mp4").absolutePath
        }
    }


    private fun uploadInfoToServer(
        farm: String,
        block: String,
        bed: String,
        videoName: String,
        resolution: String,
        fps: String,
        id: String
    ) {
        val videoService = RetrofitClient.instance

        val call = videoService.uploadInfo(farm, block, bed, videoName, resolution, fps, id)

        call.enqueue(object : Callback<YourResponseModel> {
            override fun onResponse(
                call: Call<YourResponseModel>,
                response: Response<YourResponseModel>
            ) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        showToast("Información cargada exitosamente.")

                        val db = Room.databaseBuilder(
                            applicationContext,
                            AppDatabase::class.java, "database-name"
                        ).build()

                        val videoDao = db.videoDao()


                        // Insertar usuarios en la base de datos
                        lifecycleScope.launch(Dispatchers.IO) {
                            val video = Video(null,videoName, resolution)
                            videoDao.insertAll(video)
                        }

                    } else {
                        showToast("Error cargando información.")
                    }
                }
            }

            override fun onFailure(call: Call<YourResponseModel>, t: Throwable) {
                showToast("Error en la solicitud del servidor: ${t.message}")
                Log.e("UploadInfo", "Error en la solicitud del servidor: ${t.message}", t)
            }

        }
        )
    }

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


    private fun uploadVideoToServer(videoFile: File) {
        val videoService = RetrofitClient.instance

        val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), videoFile)

        val videoPart = MultipartBody.Part.createFormData("video", videoFile.name, requestFile)

        val call = videoService.uploadVideo(videoPart)

        call.enqueue(object : Callback<YourResponseModelVideo> {
            override fun onResponse(call: Call<YourResponseModelVideo>, response: Response<YourResponseModelVideo>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    showToast("Video subido exitosamente al servidor.")
                } else {
                    showToast("Error al subir el video al servidor.")
                }
            }

            override fun onFailure(call: Call<YourResponseModelVideo>, t: Throwable) {
                showToast("Error en la solicitud al servidor: ${t.message}")
                Log.e("UploadVideo", "Error en la solicitud al servidor: ${t.message}", t)
            }
        }
        )
    }

    data class YourResponseModelVideo(
        @SerializedName("error") val error: Boolean,
        @SerializedName("message") val message: MessageVideo
    )
    data class MessageVideo(
        @SerializedName("filename") val filename: String
    )

    object RetrofitClient {
        private const val BASE_URL_UPLOAD = "http://192.168.132.45:8000"
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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}
