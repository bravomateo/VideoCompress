package com.example.videoresolution.insta360.util;

import com.arashivision.insta360.basemedia.asset.WindowCropInfo
import com.arashivision.sdkcamera.camera.InstaCameraManager
import com.arashivision.sdkmedia.player.capture.CaptureParamsBuilder

object PreviewParamsUtil {

    fun getCaptureParamsBuilder(): CaptureParamsBuilder {
        return CaptureParamsBuilder()
            .setCameraType(InstaCameraManager.getInstance().cameraType)
            .setMediaOffset(InstaCameraManager.getInstance().mediaOffset)
            .setMediaOffsetV2(InstaCameraManager.getInstance().mediaOffsetV2)
            .setMediaOffsetV3(InstaCameraManager.getInstance().mediaOffsetV3)
            .setCameraSelfie(InstaCameraManager.getInstance().isCameraSelfie)
            .setGyroTimeStamp(InstaCameraManager.getInstance().gyroTimeStamp)
            .setBatteryType(InstaCameraManager.getInstance().batteryType)
            .setWindowCropInfo(windowCropInfoConversion(InstaCameraManager.getInstance().windowCropInfo))
    }

    fun windowCropInfoConversion(cameraWindowCropInfo: com.arashivision.onecamera.camerarequest.WindowCropInfo?): WindowCropInfo? {
        if (cameraWindowCropInfo == null) return null

        return WindowCropInfo().apply {
            desHeight = cameraWindowCropInfo.dstHeight
            desWidth = cameraWindowCropInfo.dstWidth
            srcHeight = cameraWindowCropInfo.srcHeight
            srcWidth = cameraWindowCropInfo.srcWidth
            offsetX = cameraWindowCropInfo.offsetX
            offsetY = cameraWindowCropInfo.offsetY
        }
    }
}
