package com.example.videoresolution.insta360.activity

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.arashivision.insta360.basecamera.camera.CameraType
import com.arashivision.sdkcamera.camera.InstaCameraManager
import com.arashivision.sdkcamera.camera.callback.ICameraOperateCallback
import com.arashivision.sdkcamera.camera.callback.ICaptureStatusListener
import com.example.videoresolution.R
import com.example.videoresolution.insta360.util.TimeFormat
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.FileCallback
import com.lzy.okgo.model.Response
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class CaptureActivity : BaseObserveCameraActivity(), ICaptureStatusListener {

    private companion object {const val TAG = "CaptureActivity_" }

    private var mTvCaptureStatus: TextView? = null

    private var mTvCaptureTime: TextView? = null

    private var mTvCaptureCount: TextView? = null

    //private var mBtnPlayCameraFile: Button? = null

    private var mBtnPlayLocalFile: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)
        setTitle(R.string.capture_toolbar_title)

        // Relaciona la vinculación de elementos de la interfaz de usuario con variables en la clase de actividad,
        bindViews()


        // Verifica si la camara está conectada, si no está conectada la actividad finaliza
        if (InstaCameraManager.getInstance().cameraConnectedType == InstaCameraManager.CONNECT_TYPE_NONE) {
            finish()
            return
        }

        // Inicializa y asigna una instancia de SwitchSensorCallback, la cual se utiliza para gestionar eventos y estados relacionados con el cambio de sensor
        val switchSensorCallback = SwitchSensorCallback(this)


        // Dinámicamente muestra u oculta un layout dependiendo del tipo de cámara conectada isOneX3
        val layoutSwitchSensor = findViewById<LinearLayout>(R.id.layout_switch_sensor)
        layoutSwitchSensor.visibility = if (isOneX3()) View.VISIBLE else View.GONE

        // Configura el botón para cambiar el modo frontal
        findViewById<View>(R.id.btn_switch_front_sensor).setOnClickListener { _ ->
            switchSensorCallback.onStart()
            InstaCameraManager.getInstance().switchCameraMode(InstaCameraManager.CAMERA_MODE_SINGLE_FRONT, InstaCameraManager.FOCUS_SENSOR_FRONT, switchSensorCallback)
        }

        // Configura el botón para iniciar la grabación de video normal
        findViewById<View>(R.id.btn_normal_record_start).setOnClickListener { _ ->
            if (checkSdCardEnabled()) {InstaCameraManager.getInstance().startNormalRecord()}
            InstaCameraManager.getInstance().startNormalRecord()
        }

        // Configura el botón para detener la grabación de video normal
        findViewById<View>(R.id.btn_normal_record_stop).setOnClickListener { _ -> InstaCameraManager.getInstance().stopNormalRecord()}

        // Capture Status Callback
        InstaCameraManager.getInstance().setCaptureStatusListener(this)
    }

    private fun bindViews() {
        mTvCaptureStatus   = findViewById(R.id.tv_capture_status)         // show capture status
        mTvCaptureTime     = findViewById(R.id.tv_capture_time)           // show elapsed time during a capture
        mTvCaptureCount    = findViewById(R.id.tv_capture_count)          // show the number of captures made.
        //mBtnPlayCameraFile = findViewById(R.id.btn_play_camera_file)      // play captured files directly from the camera
        mBtnPlayLocalFile  = findViewById(R.id.btn_play_local_file)       // play captured files that have been saved locally on the device

    }

    // Verifica si la camara es OneX3
    private fun isOneX3(): Boolean {
        return CameraType.getForType(InstaCameraManager.getInstance().cameraType) == CameraType.X3
    }

    // Verifica que la tarjeta SD esté disponible
    private fun checkSdCardEnabled(): Boolean {
        if (!InstaCameraManager.getInstance().isSdCardEnabled) {
            Toast.makeText(this, R.string.capture_toast_sd_card_error, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }


    // Asegura que CaptureActivity se cierre automáticamente si la cámara se desconecta
    override fun onCameraStatusChanged(enabled: Boolean) {
        super.onCameraStatusChanged(enabled)
        if (!enabled) {
            finish()
        }
    }


    // FUNCIONES DE MANEJO DE INTERFAZ

    // Actualiza la interfaz de usuario para reflejar que una captura está comenzando
    override fun onCaptureStarting() {
        // Mostrar mensaje indicando que la captura está comenzando
        mTvCaptureStatus?.setText(R.string.capture_capture_starting)
        // Oculta los dos botones
        //mBtnPlayCameraFile?.visibility = View.GONE
        mBtnPlayLocalFile?.visibility = View.GONE
    }

    // Actualiza la interfaz de usuario para reflejar que una captura está trabajando
    override fun onCaptureWorking() {
        mTvCaptureStatus?.setText(R.string.capture_capture_working)
    }

    // Actualiza la interfaz de usuario para reflejar que una captura se está deteniendo
    override fun onCaptureStopping() {
        mTvCaptureStatus?.setText(R.string.capture_capture_stopping)
    }

    // Maneja el evento cuando la captura de la cámara ha finalizado
    override fun onCaptureFinish(filePaths: Array<String>) {
        // Imprime  información acerca de los archivos capturados
        Log.i(TAG, "onCaptureFinish, filePaths = ${filePaths?.contentToString() ?: "null"}")

        // Mostrar mensaje indicando que la captura a finalizado
        mTvCaptureStatus?.setText(R.string.capture_capture_finished)
        // Muestren información como el tiempo de captura y el número de capturas
        mTvCaptureTime?.visibility = View.GONE
        mTvCaptureCount?.visibility = View.GONE


        // Si filePaths no es nulo y tiene elementos,
        if (filePaths != null && filePaths.isNotEmpty()) {
            // Hace visible el boton Play Camera File
            //mBtnPlayCameraFile?.visibility = View.VISIBLE

            /*
            // Se configura su click listener para lanzar la actividad
            mBtnPlayCameraFile?.setOnClickListener {
                // TODO PlayAndExportActivity.launchActivity(this, filePaths)
            }*/

            // Hace visible el boton Play Local File
            mBtnPlayLocalFile?.visibility = View.VISIBLE

            // Se configura su click listener para descargar y reproducir los archivos.
            mBtnPlayLocalFile?.setOnClickListener {
                downloadFilesAndPlay(filePaths)
            }
        } else {
            // Los botones se hacen invisibles y se eliminia cualquier listener
            //mBtnPlayCameraFile?.visibility = View.GONE
            //mBtnPlayCameraFile?.setOnClickListener(null)
            mBtnPlayLocalFile?.visibility = View.GONE
            mBtnPlayLocalFile?.setOnClickListener(null)
        }
    }

    // Actualiza el tiempo transcurrido durante una captura de cámara
    override fun onCaptureTimeChanged(captureTime: Long) {
        mTvCaptureTime?.visibility = View.VISIBLE
        mTvCaptureTime?.text = getString(R.string.capture_capture_time, TimeFormat.durationFormat(captureTime))
    }

    // Actualizar visualmente el número de capturas realizadas durante una sesión
    override fun onCaptureCountChanged(captureCount: Int) {
        mTvCaptureCount?.visibility = View.VISIBLE
        mTvCaptureCount?.text = getString(R.string.capture_capture_count, captureCount)
    }



    // Maneja la descarga de archivos desde una lista de URLs y
    // luego inicia una actividad para reproducir o exportar los archivos descargados.
    private fun downloadFilesAndPlay(urls: Array<String>?) {
        if (urls.isNullOrEmpty()) {
            return
        }

        // Preparación de Rutas Locales
        val localFolder = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)}/SDK_DEMO_CAPTURE"
        val fileNames = Array(urls.size) { "" }
        val localPaths = Array(urls.size) { "" }
        var needDownload = false


        // Iteración sobre URLs y Verificación de Existencia Local:
        for (i in urls.indices) {
            fileNames[i] = urls[i].substring(urls[i].lastIndexOf("/") + 1)
            localPaths[i] = "$localFolder/${fileNames[i]}"
            if (!File(localPaths[i]).exists()) {
                needDownload = true
            }
        }


        // Manejo de Descarga o Lanzamiento Directo:
        if (!needDownload) {
            PlayAndExportActivity.launchActivity(this, localPaths)
            return
        }


        // Inicio de Descarga con Diálogo de Progreso:
        val dialog = MaterialDialog(this).show {
            title(R.string.osc_dialog_title_downloading)
            message(text = getString(R.string.osc_dialog_msg_downloading, urls.size, 0, 0))
            cancelable(false)
            cancelOnTouchOutside(false)
        }

        // Contadores de Éxito y Error:
        val successfulCount = AtomicInteger(0)
        val errorCount = AtomicInteger(0)


        // Bucle de Descarga de Archivos:
        for (i in localPaths.indices) {
            val url = urls[i]
            OkGo.get<File>(url)
                .execute(object : FileCallback(localFolder, fileNames[i]) {
                    override fun onError(response: Response<File>) {
                        super.onError(response)
                        errorCount.incrementAndGet()
                        checkDownloadCount()
                    }

                    override fun onSuccess(response: Response<File>) {
                        successfulCount.incrementAndGet()
                        checkDownloadCount()
                    }

                    private fun checkDownloadCount() {
                        dialog.message(text = getString(R.string.osc_dialog_msg_downloading, urls.size, successfulCount.toInt(), errorCount.toInt()))

                        if (successfulCount.toInt() + errorCount.toInt() >= urls.size) {
                            PlayAndExportActivity.launchActivity(this@CaptureActivity, localPaths)
                            dialog.dismiss()
                        }
                    }
                })
        }

    }

    private class SwitchSensorCallback(private val context: Context) : ICameraOperateCallback {

        private var dialog: MaterialDialog? = null

        fun onStart() {
            dialog = MaterialDialog(context).show {
                message(res = R.string.capture_switch_sensor_ing)
                cancelable(false)
                cancelOnTouchOutside(false)
            }
        }

        override fun onSuccessful() {
            dialog?.dismiss()
            dialog = null
            Toast.makeText(context, R.string.capture_switch_sensor_success, Toast.LENGTH_SHORT).show()
        }

        override fun onFailed() {
            dialog?.dismiss()
            dialog = null
            Toast.makeText(context, R.string.capture_switch_sensor_failed, Toast.LENGTH_SHORT).show()
        }

        override fun onCameraConnectError() {
            dialog?.dismiss()
            dialog = null
            Toast.makeText(context, R.string.capture_switch_sensor_camera_connect_error, Toast.LENGTH_SHORT).show()
        }
    }

}
