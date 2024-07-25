package com.example.videoresolution.insta360.activity

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.arashivision.sdkcamera.camera.InstaCameraManager
import com.arashivision.sdkcamera.camera.callback.ICaptureStatusListener
import com.example.videoresolution.R
import com.example.videoresolution.insta360.util.TimeFormat
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.FileCallback
import com.lzy.okgo.model.Response
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class CaptureActivity : BaseObserveCameraActivity(), ICaptureStatusListener {



    private var mTvCaptureStatus: TextView? = null
    private var mTvCaptureTime: TextView? = null
    private var mTvCaptureCount: TextView? = null
    private var mBtnPlayLocalFile: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)
        setTitle(R.string.capture_toolbar_title)

        bindViews()

        if (InstaCameraManager.getInstance().cameraConnectedType == InstaCameraManager.CONNECT_TYPE_NONE) {
            finish()
            return
        }

        findViewById<View>(R.id.btn_normal_record_start).setOnClickListener { _ ->
            if (checkSdCardEnabled()) {InstaCameraManager.getInstance().startNormalRecord()}
        }

        findViewById<View>(R.id.btn_normal_record_stop).setOnClickListener { _ ->
            InstaCameraManager.getInstance().stopNormalRecord()
        }

        InstaCameraManager.getInstance().setCaptureStatusListener(this)
    }

    private fun bindViews() {
        mTvCaptureStatus = findViewById(R.id.tv_capture_status)
        mTvCaptureTime = findViewById(R.id.tv_capture_time)
        mTvCaptureCount = findViewById(R.id.tv_capture_count)
        mBtnPlayLocalFile = findViewById(R.id.btn_play_local_file)

    }


    private fun checkSdCardEnabled(): Boolean {
        if (!InstaCameraManager.getInstance().isSdCardEnabled) {
            Toast.makeText(this, R.string.capture_toast_sd_card_error, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    override fun onCameraStatusChanged(enabled: Boolean) {
        super.onCameraStatusChanged(enabled)
        if (!enabled) {
            finish()
        }
    }

    override fun onCaptureStarting() {
        mTvCaptureStatus?.setText(R.string.capture_capture_starting)
        mBtnPlayLocalFile?.visibility = View.GONE
    }

    override fun onCaptureWorking() {
        mTvCaptureStatus?.setText(R.string.capture_capture_working)
    }

    override fun onCaptureStopping() {
        mTvCaptureStatus?.setText(R.string.capture_capture_stopping)
    }

    override fun onCaptureFinish(filePaths: Array<String>) {
        Log.i("CaptureActivity", "onCaptureFinish, filePaths = ${filePaths?.contentToString() ?: "null"}")

        mTvCaptureStatus?.setText(R.string.capture_capture_finished)
        mTvCaptureTime?.visibility = View.GONE
        mTvCaptureCount?.visibility = View.GONE

        // Si se capturaron archivos
        if (filePaths != null && filePaths.isNotEmpty()) {

            mBtnPlayLocalFile?.visibility = View.VISIBLE

            mBtnPlayLocalFile?.setOnClickListener{
                downloadFilesAndPlay(filePaths)
            }

        } else { // Si no se capturaron archivos

            mBtnPlayLocalFile?.visibility = View.GONE

            mBtnPlayLocalFile?.setOnClickListener(null)
        }
    }

    override fun onCaptureTimeChanged(captureTime: Long) {
        mTvCaptureTime?.visibility = View.VISIBLE
        mTvCaptureTime?.text =
            getString(R.string.capture_capture_time, TimeFormat.durationFormat(captureTime))
    }

    override fun onCaptureCountChanged(captureCount: Int) {
        mTvCaptureCount?.visibility = View.VISIBLE
        mTvCaptureCount?.text = getString(R.string.capture_capture_count, captureCount)
    }


    private fun downloadFilesAndPlay(urls: Array<String>?) {

        if (urls.isNullOrEmpty()) {
            return
        }

        val localFolder = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)}/SDK_DEMO_CAPTURE"
        val fileNames = Array(urls.size) { "" }
        val localPaths = Array(urls.size) { "" }
        var needDownload = false


        for (i in urls.indices) {
            fileNames[i] = urls[i].substring(urls[i].lastIndexOf("/") + 1)
            localPaths[i] = "$localFolder/${fileNames[i]}"
            if (!File(localPaths[i]).exists()) {
                needDownload = true
            }
        }


        if (!needDownload) {
            //PlayAndExportActivity.launchActivity(this, localPaths)
            //return
        }

        val dialog = MaterialDialog(this).show {
            title(R.string.osc_dialog_title_downloading)
            message(text = getString(R.string.osc_dialog_msg_downloading, urls.size, 0, 0))
            cancelable(false)
            cancelOnTouchOutside(false)
        }

        val successfulCount = AtomicInteger(0)
        val errorCount = AtomicInteger(0)


        for (i in localPaths.indices) {
            val url = urls[i]
            OkGo.get<File>(url).execute(object : FileCallback(localFolder, fileNames[i]) {

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
                        dialog.message(text = getString(
                                R.string.osc_dialog_msg_downloading,
                                urls.size,
                                successfulCount.toInt(),
                                errorCount.toInt()
                            ))
                        if (successfulCount.toInt() + errorCount.toInt() >= urls.size) {
                            //PlayAndExportActivity.launchActivity(this@CaptureActivity, localPaths)
                            dialog.dismiss()
                        }
                    }
                })
        }

    }


}
