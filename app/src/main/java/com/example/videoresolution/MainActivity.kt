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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val selectFileButton: Button = findViewById(R.id.selectFileButton)
        selectFileButton.setOnClickListener {
            checkPermissionsAndOpenFilePicker()
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

            // Obtener las dimensiones personalizadas desde los campos de entrada
            val width = findViewById<EditText>(R.id.editTextWidth).text.toString()
            val height = findViewById<EditText>(R.id.editTextHeight).text.toString()
            val fps = findViewById<EditText>(R.id.editTextFPS).text.toString()


            // Validar si se ingresaron valores válidos
            if (width.isNotEmpty() && height.isNotEmpty() && fps.isNotEmpty()) {
                val videoView: VideoView = findViewById(R.id.videoView)
                videoView.setVideoURI(selectedVideoUri)
                videoView.start()

                val originalPath = getRealPathFromUri(selectedVideoUri)
                val outputDirectory =
                    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "ReducedResolution")
                outputDirectory.mkdirs()

                val currentDateTime = obtenerFechaYHoraActual()
                val outputFilePath = File(outputDirectory, "${currentDateTime}.mp4").absolutePath

                // Mostrar el diálogo de progreso
                progressDialog = ProgressDialog.show(this, "Procesando", "Convirtiendo video...", true, false)

                // Llamar a reduceResolution con las dimensiones y FPS personalizados utilizando AsyncTask
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

            val command = arrayOf(
                "-i", inputPath,
                "-vf", "scale=$width:$height",   //  Utilizar las dimensiones personalizadas
                "-r", fps,                       //  Establecer los FPS deseados
                "-b:v", "500K",                  //  Ajustar según sea necesario
                "-c:a", "copy",
                outputPath
            )

            return FFmpeg.execute(command)
        }

        override fun onPostExecute(result: Int) {
            progressDialog.dismiss()

            if (result == 0) {
                showToast("Video comprimido exitosamente")

                // Subir el video procesado al servidor
                val processedVideoFile = File(outputPath)
                uploadVideoToServer(processedVideoFile)
            } else {
                showToast("Error al reducir la resolución del video")
            }
        }
    }

    private fun uploadVideoToServer(videoFile: File) {
        val videoService = RetrofitClient.instance

        // Crear un objeto de tipo RequestBody a partir del archivo
        val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), videoFile)

        // Crear la parte MultipartBody
        val videoPart = MultipartBody.Part.createFormData("video", videoFile.name, requestFile)

        // Hacer la llamada a la API
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
        private const val BASE_URL = "http://192.168.58.103:8000"

        private val okHttpClient = OkHttpClient.Builder()
            .build()

        val instance: VideoService by lazy {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
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