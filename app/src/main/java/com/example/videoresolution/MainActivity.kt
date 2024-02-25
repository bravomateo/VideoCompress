package com.example.videoresolution

import android.Manifest
import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

class MainActivity : AppCompatActivity() {

    private val REQUEST_PERMISSION_CODE = 123
    private val REQUEST_VIDEO_CODE = 456
    private lateinit var progressDialog: ProgressDialog

    private lateinit var blockDropdown: AutoCompleteTextView
    private lateinit var selectedBlock: String

    private lateinit var selectedFarm: String


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
        val blocksAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, blocksItems)
        blockDropdown.setAdapter(blocksAdapter)

        blockDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedBlock = blockDropdown.adapter.getItem(position).toString()
        }


        /*
        farmsDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedFarm = farmsDropdown.adapter.getItem(position).toString()
        }*/

        selectedFarm = intent.getStringExtra("selectedFarm") ?: ""

        val syncButton: Button = findViewById(R.id.syncButton)
        syncButton.setOnClickListener {
            // Show block dropdown
            ApiUtils.getAndSetBlocksDropdown(this, blockDropdown)
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

                progressDialog =
                    ProgressDialog.show(this, "Procesando", "Convirtiendo video...", true, false)

                VideoConversionTask(outputFilePath).execute(
                    originalPath,
                    outputFilePath,
                    width,
                    height,
                    fps
                )


            } else {
                showToast("Ingrese valores válidos para la resolución.")
            }

        } else {
            showToast("No se seleccionó ningún video.")
        }
    }

    private inner class VideoConversionTask(private val outputPath: String) :
        AsyncTask<String, Void, Int>() {

        override fun doInBackground(vararg params: String?): Int {
            val inputPath = params[0] ?: ""
            val width = params[2] ?: ""
            val height = params[3] ?: ""
            val fps = params[4] ?: ""

            val tempOutputPath = getTempFilePath()
            val frameReductionCommand = arrayOf(
                "-i", inputPath,
                "-r", fps,
                tempOutputPath
            )

            val frameReductionResult = FFmpeg.execute(frameReductionCommand)
            if (frameReductionResult != 0) {
                return frameReductionResult
            }

            val resolutionReductionCommand = arrayOf(
                "-i", tempOutputPath,
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
        private const val BASE_URL_UPLOAD = "http://192.168.29.45:8000"
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
