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
import android.widget.VideoView
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

class MainActivity : AppCompatActivity() {

    private val REQUEST_PERMISSION_CODE = 123
    private val REQUEST_VIDEO_CODE = 456
    private lateinit var progressDialog: ProgressDialog
    private lateinit var dropdown: AutoCompleteTextView
    private var selectedBlock: String = "Ningun bloque"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val selectFileButton: Button = findViewById(R.id.selectFileButton)
        selectFileButton.setOnClickListener {
            checkPermissionsAndOpenFilePicker()
        }

        // Obtener el AutoCompleteTextView
        dropdown = findViewById(R.id.dropdown_field)
        val items = arrayOf("Ningun bloque")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)
        dropdown.setAdapter(adapter)

        val syncButton: Button = findViewById(R.id.syncButton)
        syncButton.setOnClickListener {
            val call = RetrofitClient.instanceForGet.getBlocks()

            call.enqueue(object : Callback<List<BlockItem>> {
                override fun onResponse(call: Call<List<BlockItem>>, response: Response<List<BlockItem>>) {
                    if (response.isSuccessful) {
                        val blocksList = response.body()

                        if (blocksList != null) {
                            showToast("Bloques obtenidos.")
                            val blockNumbers = blocksList.map { block -> block.blockNumber }.toTypedArray()
                            val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, blockNumbers)
                            dropdown.setAdapter(adapter)
                        }
                    } else {
                        showToast("Error al obtener bloques del servidor.")
                    }
                }

                override fun onFailure(call: Call<List<BlockItem>>, t: Throwable) {
                    showToast("Error en la solicitud al servidor: ${t.message}")
                    Log.e("GetBlocks", "Error en la solicitud al servidor: ${t.message}", t)
                }

            })
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

            val selectedVideoUri: Uri = data.data!!

            val width = findViewById<EditText>(R.id.editTextWidth).text.toString()
            val height = findViewById<EditText>(R.id.editTextHeight).text.toString()
            val fps = findViewById<EditText>(R.id.editTextFPS).text.toString()

            if (width.isNotEmpty() && height.isNotEmpty() && fps.isNotEmpty()) {

                val originalPath = getRealPathFromUri(selectedVideoUri)
                val outputDirectory =
                    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "ReducedResolution")
                outputDirectory.mkdirs()

                val currentDateTime = obtenerFechaYHoraActual()
                val outputFilePath = File(outputDirectory, "${currentDateTime}.mp4").absolutePath

                progressDialog = ProgressDialog.show(this, "Procesando", "Convirtiendo video...", true, false)

                VideoConversionTask(outputFilePath).execute(originalPath, outputFilePath, width, height, fps)


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

                // Cargar la información al servidor
                val bed = findViewById<EditText>(R.id.editTextBed).text.toString()
                val farm = findViewById<EditText>(R.id.editTextFarm).text.toString()
                val block = findViewById<EditText>(R.id.editTextBlock).text.toString()

                uploadInfoToServer(bed, farm, block)

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

    private fun uploadInfoToServer(bed: String, farm: String, block: String) {
        val videoService = RetrofitClient.instance

        val call = videoService.uploadInfo(bed, farm, block)

        call.enqueue(object : Callback<YourResponseModel> {
            override fun onResponse(call: Call<YourResponseModel>, response: Response<YourResponseModel>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    showToast("Información cargada exitosamente.")
                } else {
                    showToast("Error cargando información.")
                }
            }

            override fun onFailure(call: Call<YourResponseModel>, t: Throwable) {
                showToast("Error en la solicitud del servidor: ${t.message}")
                Log.e("UploadInfo", "Error en la solicitud del servidor: ${t.message}", t)
            }
        })
    }


    private fun uploadVideoToServer(videoFile: File) {
        val videoService = RetrofitClient.instance

        val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), videoFile)

        val videoPart = MultipartBody.Part.createFormData("video", videoFile.name, requestFile)

        val call = videoService.uploadVideo(videoPart)

        call.enqueue(object : Callback<YourResponseModel> {
            override fun onResponse(call: Call<YourResponseModel>, response: Response<YourResponseModel>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    showToast("Video subido exitosamente al servidor.")
                } else {
                    showToast("Error al subir el video al servidor.")
                }
            }

            override fun onFailure(call: Call<YourResponseModel>, t: Throwable) {
                showToast("Error en la solicitud al servidor: ${t.message}")
                Log.e("UploadVideo", "Error en la solicitud al servidor: ${t.message}", t)
            }
        }
        )
    }

    data class YourResponseModel(
        @SerializedName("error") val error: Boolean,
        @SerializedName("message") val message: Message
    )
    data class Message(
        @SerializedName("filename") val filename: String
    )
    object RetrofitClient {
        private const val BASE_URL_UPLOAD = "http://192.168.161.45:8000"
        private const val BASE_URL_GET = "http://10.1.2.22:544"

        private val okHttpClient = OkHttpClient.Builder()
            .build()

        val instance: VideoService by lazy {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL_UPLOAD)  // Puedes cambiarlo según el servidor al que deseas hacer el POST
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            retrofit.create(VideoService::class.java)
        }

        val instanceForGet: VideoService by lazy {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL_GET)  // Cambia esto según el servidor del que deseas obtener la lista de bloques
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

    private fun obtenerFechaYHoraActual(): String {
        val calendario = Calendar.getInstance()
        val dia = calendario.get(Calendar.DAY_OF_MONTH)
        val mes = calendario.get(Calendar.MONTH) + 1  // Se suma 1 porque los meses empiezan desde 0
        val anio = calendario.get(Calendar.YEAR)
        val hora = calendario.get(Calendar.HOUR_OF_DAY)
        val minuto = calendario.get(Calendar.MINUTE)
        val segundo = calendario.get(Calendar.SECOND)

        // Formatear el mes con dos dígitos
        val mesFormateado = if (mes < 10) "0$mes" else mes.toString()

        return "$dia-$mesFormateado-$anio-$hora-$minuto-$segundo"
    }

}