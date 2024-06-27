package com.example.videoresolution.insta360.activity


import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.arashivision.sdkcamera.camera.InstaCameraManager
import com.example.videoresolution.R
import com.example.videoresolution.insta360.util.CameraBindNetworkManager
import com.example.videoresolution.insta360.util.NetworkManager
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.runtime.Permission

class MainActivityInsta360 : BaseObserveCameraActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_insta360)
        setTitle(R.string.main_toolbar_title)


        checkStoragePermission()

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

    }


    private fun checkStoragePermission() {
        AndPermission.with(this)
            .runtime()
            .permission(Permission.READ_EXTERNAL_STORAGE, Permission.ACCESS_FINE_LOCATION)
            .onDenied { permissions ->
                if (AndPermission.hasAlwaysDeniedPermission(this, permissions)) {
                    AndPermission.with(this)
                        .runtime()
                        .setting()
                        .start(1000)
                } else {
                    finish()
                }
            }
            .start()
    }

    override fun onCameraStatusChanged(enabled: Boolean) {
        super.onCameraStatusChanged(enabled)
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
        Toast.makeText(
            this,
            resources.getString(R.string.main_toast_camera_connect_error, errorCode),
            Toast.LENGTH_SHORT
        ).show()
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