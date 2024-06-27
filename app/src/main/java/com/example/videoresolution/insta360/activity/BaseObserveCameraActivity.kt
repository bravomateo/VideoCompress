package com.example.videoresolution.insta360.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arashivision.sdkcamera.camera.InstaCameraManager
import com.arashivision.sdkcamera.camera.callback.ICameraChangedCallback


abstract class BaseObserveCameraActivity : AppCompatActivity(), ICameraChangedCallback {
    protected val TAG = this.javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InstaCameraManager.getInstance().registerCameraChangedCallback(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        InstaCameraManager.getInstance().unregisterCameraChangedCallback(this)
    }

    override fun onCameraStatusChanged(enabled: Boolean) {}

    override fun onCameraConnectError(errorCode: Int) {}

    override fun onCameraSDCardStateChanged(enabled: Boolean) {}

    override fun onCameraStorageChanged(freeSpace: Long, totalSpace: Long) {}

    override fun onCameraBatteryLow() {}

    override fun onCameraBatteryUpdate(batteryLevel: Int, isCharging: Boolean) {}

    override fun onCameraSensorModeChanged(cameraSensorMode: Int) {}
}
