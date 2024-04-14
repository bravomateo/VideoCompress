package com.example.videoresolution

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import com.arthenica.mobileffmpeg.FFmpeg
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.Calendar
import java.util.UUID

object VideoUtils {

    class VideoConversionTaskClass(
        private val context: Context,
        private val outputPath: String,
        private val startTime: Int,
        private val endTime: Int
    ) : AsyncTask<String, Void, Int>() {

        override fun doInBackground(vararg params: String?): Int {
            val inputPath = params[0] ?: ""
            val width = params[2] ?: ""
            val height = params[3] ?: ""
            val fps = params[4] ?: ""

            val tempOutputPath = getTempFilePath(context)
            val frameReductionCommand = arrayOf(
                "-i", inputPath,
                "-ss", startTime.toString(),
                "-t", (endTime - startTime).toString(),
                "-r", fps,
                tempOutputPath
            )

            val frameReductionResult = FFmpeg.execute(frameReductionCommand)
            if (frameReductionResult != 0) {
                return frameReductionResult
            }
            val orientationDegrees = 0.0f

            val rotatedTempOutputPath = getTempFilePath(context)
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
                "-i", rotatedTempOutputPath,
                "-vf", "scale=$width:$height",
                "-b:v", "10K",
                "-an",
                outputPath
            )
            return FFmpeg.execute(resolutionReductionCommand)
        }
        override fun onPostExecute(result: Int) {
            //progressDialog.dismiss()

            if (result == 0) {
                //showToast(context,"Video procesado exitosamente")

                val bed = "02"
                val width = "720"
                val height = "480"
                val resolution = "$width x $height"
                val fps = "15"

                val id = UUID.randomUUID().toString()
                val videoName = obtenerFechaYHoraActual()

                val selectedBlock = "03"
                val selectedFarm = "BC"

                uploadInfoToServer(context,selectedFarm, selectedBlock, bed, videoName, resolution, fps, id)

                val processedVideoFile = File(outputPath)
                uploadVideoToServer(processedVideoFile)


            } else {
                showToast(context,"Error al procesar el video")
            }
        }
        private fun getTempFilePath(context: Context): String {
            val outputDirectory = File(context.cacheDir, "Temp")
            outputDirectory.mkdirs()
            val currentDateTime = obtenerFechaYHoraActual()
            return File(outputDirectory, "${currentDateTime}_temp.mp4").absolutePath
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
        private fun showToast(context: Context, message: String) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        private fun uploadInfoToServer(
            context: Context,
            farm: String,
            block: String,
            bed: String,
            videoName: String,
            resolution: String,
            fps: String,
            id: String
        ) {
            val videoService = MainActivity.RetrofitClient.instance

            val call = videoService.uploadInfo(farm, block, bed, videoName, resolution, fps, id)

            call.enqueue(object : Callback<MainActivity.YourResponseModel> {
                override fun onResponse(
                    call: Call<MainActivity.YourResponseModel>,
                    response: Response<MainActivity.YourResponseModel>
                ) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        if (responseBody != null) {
                            //showToast(context,"Información cargada exitosamente.")

                        } else {
                            showToast(context,"Error cargando información.")
                        }
                    }
                }

                override fun onFailure(call: Call<MainActivity.YourResponseModel>, t: Throwable) {
                    showToast(context,"Error en la solicitud del servidor: ${t.message}")
                    Log.e("UploadInfo", "Error en la solicitud del servidor: ${t.message}", t)
                }

            }
            )
        }
        private fun uploadVideoToServer(videoFile: File) {
            val videoService = MainActivity.RetrofitClient.instance

            val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), videoFile)

            val videoPart = MultipartBody.Part.createFormData("video", videoFile.name, requestFile)

            val call = videoService.uploadVideo(videoPart)

            call.enqueue(object : Callback<MainActivity.YourResponseModelVideo> {
                override fun onResponse(call: Call<MainActivity.YourResponseModelVideo>, response: Response<MainActivity.YourResponseModelVideo>) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        showToast(context,"Video subido exitosamente al servidor.")
                    } else {
                        showToast(context,"Error al subir el video al servidor.")
                    }
                }

                override fun onFailure(call: Call<MainActivity.YourResponseModelVideo>, t: Throwable) {
                    showToast(context,"Error en la solicitud al servidor: ${t.message}")
                    Log.e("UploadVideo", "Error en la solicitud al servidor: ${t.message}", t)
                }
            }
            )
        }

    }

}