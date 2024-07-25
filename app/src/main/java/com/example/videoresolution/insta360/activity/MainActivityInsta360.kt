package com.example.videoresolution.insta360.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.arashivision.sdkcamera.camera.InstaCameraManager
import com.example.videoresolution.R
import com.example.videoresolution.insta360.util.CameraBindNetworkManager
import com.example.videoresolution.insta360.util.NetworkManager


class MainActivityInsta360 : BaseObserveCameraActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_insta360)
        setTitle(R.string.main_toolbar_title)


        if (!checkPermission()) {
            requestPermission()
        }


        if (InstaCameraManager.getInstance().cameraConnectedType != InstaCameraManager.CONNECT_TYPE_NONE) {
            onCameraStatusChanged(true)
        }

        // Button Connect WIFI
        findViewById<View>(R.id.btn_connect_by_wifi).setOnClickListener {
            CameraBindNetworkManager.getInstance().bindNetwork { _ ->
                InstaCameraManager.getInstance().openCamera(InstaCameraManager.CONNECT_TYPE_WIFI)
            }
        }

        // Button Disconnect
        findViewById<View>(R.id.btn_close_camera).setOnClickListener {
            CameraBindNetworkManager.getInstance().unbindNetwork()
            InstaCameraManager.getInstance().closeCamera()
        }

        // Button Capture
        findViewById<View>(R.id.btn_capture).setOnClickListener { _ ->
            startActivity(Intent(this@MainActivityInsta360, CaptureActivity::class.java)
            )
        }

    }

    private fun checkPermission(): Boolean {
        val resultRead = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE)
        val resultWrite = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return resultRead == PackageManager.PERMISSION_GRANTED && resultWrite == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            } else {
            }
        }

    }

    override fun onCameraStatusChanged(enabled: Boolean) {
        super.onCameraStatusChanged(enabled)
        findViewById<Button>(R.id.btn_capture).isEnabled = enabled
        if (enabled) {
            Toast.makeText(this, R.string.main_toast_camera_connected, Toast.LENGTH_SHORT).show()
        } else {
            CameraBindNetworkManager.getInstance().unbindNetwork()
            NetworkManager.getInstance().clearBindProcess()
            Toast.makeText(this, R.string.main_toast_camera_disconnected, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCameraConnectError(errorCode: Int) {
        super.onCameraConnectError(errorCode)
        CameraBindNetworkManager.getInstance().unbindNetwork()
        Toast.makeText(this, resources.getString(R.string.main_toast_camera_connect_error, errorCode), Toast.LENGTH_SHORT).show()
    }

    override fun onCameraSDCardStateChanged(enabled: Boolean) {
        super.onCameraSDCardStateChanged(enabled)
        if (enabled) {
            Toast.makeText(this, R.string.main_toast_sd_enabled, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.main_toast_sd_disabled, Toast.LENGTH_SHORT).show()
        }
    }

}