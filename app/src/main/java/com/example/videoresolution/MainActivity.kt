package com.example.videoresolution

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.arthenica.mobileffmpeg.FFmpeg
import java.io.File
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private val REQUEST_PERMISSION_CODE = 123
    private val REQUEST_VIDEO_CODE = 456

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

                // Llamar a reduceResolution con las dimensiones y FPS personalizados
                reduceResolution(originalPath, outputFilePath, width, height, fps)
            } else {
                showToast("Ingrese valores válidos para la resolución.")
            }

        } else {
            showToast("No se seleccionó ningún video.")
        }
    }

    private fun reduceResolution(inputPath: String, outputPath: String, width: String, height: String, fps: String) {

        val command = arrayOf(
            "-i", inputPath,
            "-vf", "scale=$width:$height",  // Utilizar las dimensiones personalizadas
            "-r", fps,                       // Establecer los FPS deseados
            "-b:v", "500K",                  // Ajustar según sea necesario
            "-c:a", "copy",
            outputPath
        )

        val result = FFmpeg.execute(command)

        if (result == 0) {
            showToast("Video comprimido exitosamente")
        } else {
            showToast("Error al reducir la resolución del video")
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

        return "$dia$mesFormateado$anio$hora$minuto$segundo"
    }

}



















