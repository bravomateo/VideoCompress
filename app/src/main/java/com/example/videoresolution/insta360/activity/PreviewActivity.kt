package com.arashivision.sdk.demo.activity

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.ToggleButton
import androidx.annotation.Nullable
import com.arashivision.insta360.basecamera.camera.BaseCamera
import com.arashivision.insta360.basecamera.camera.CameraType
import com.arashivision.insta360.basemedia.model.offset.OffsetData

import com.arashivision.sdkcamera.camera.InstaCameraManager
import com.arashivision.sdkcamera.camera.callback.IPreviewStatusListener
import com.arashivision.sdkcamera.camera.resolution.PreviewStreamResolution
import com.arashivision.sdkmedia.player.capture.CaptureParamsBuilder
import com.arashivision.sdkmedia.player.capture.InstaCapturePlayerView
import com.arashivision.sdkmedia.player.config.InstaStabType
import com.arashivision.sdkmedia.player.listener.PlayerViewListener
import com.example.videoresolution.R
import com.example.videoresolution.insta360.activity.BaseObserveCameraActivity
import com.example.videoresolution.insta360.util.PreviewParamsUtil

class PreviewActivity : BaseObserveCameraActivity(), IPreviewStatusListener {

    private lateinit var mLayoutContent: ViewGroup
    private lateinit var mCapturePlayerView: InstaCapturePlayerView
    private lateinit var mBtnSwitch: ToggleButton
    private lateinit var mRbNormal: RadioButton
    private lateinit var mRbFisheye: RadioButton
    private lateinit var mRbPerspective: RadioButton
    private lateinit var mRbPlane: RadioButton
    private lateinit var mSpinnerResolution: Spinner
    private lateinit var mSpinnerStabType: Spinner

    private var mCurrentResolution: PreviewStreamResolution? = null

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        title = getString(R.string.preview_toolbar_title)
        bindViews()

        InstaCameraManager.getInstance().setPreviewStatusChangedListener(this)
        // mSpinnerResolution -> onItemSelected() Will automatically trigger to open the preview, so comment it out here
        // InstaCameraManager.getInstance().startPreviewStream()
    }

    private fun bindViews() {
        mLayoutContent = findViewById(R.id.layout_content)
        mCapturePlayerView = findViewById(R.id.player_capture)
        // mCapturePlayerView.lifecycle = this.lifecycle


        mBtnSwitch = findViewById(R.id.btn_switch)
        mBtnSwitch.setOnClickListener {
            if (mBtnSwitch.isChecked) {
                mCurrentResolution?.let {
                    InstaCameraManager.getInstance().startPreviewStream(it)
                } ?: InstaCameraManager.getInstance().startPreviewStream()
            } else {
                InstaCameraManager.getInstance().closePreviewStream()
            }
        }

        mRbNormal = findViewById(R.id.rb_normal)
        mRbFisheye = findViewById(R.id.rb_fisheye)
        mRbPerspective = findViewById(R.id.rb_perspective)
        mRbPlane = findViewById(R.id.rb_plane)
        val radioGroup: RadioGroup = findViewById(R.id.rg_preview_mode)
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_plane -> {
                    InstaCameraManager.getInstance().closePreviewStream()
                    mCurrentResolution?.let {
                        InstaCameraManager.getInstance().startPreviewStream(it)
                    } ?: InstaCameraManager.getInstance().startPreviewStream()
                    mRbFisheye.isEnabled = false
                    mRbPerspective.isEnabled = false
                }

                R.id.rb_normal -> {
                    if (!mRbFisheye.isEnabled || !mRbPerspective.isEnabled) {
                        InstaCameraManager.getInstance().closePreviewStream()
                        mCurrentResolution?.let {
                            InstaCameraManager.getInstance().startPreviewStream(it)
                        } ?: InstaCameraManager.getInstance().startPreviewStream()
                        mRbFisheye.isEnabled = true
                        mRbPerspective.isEnabled = true
                    } else {
                        mCapturePlayerView.switchNormalMode()
                    }
                }

                R.id.rb_fisheye -> mCapturePlayerView.switchFisheyeMode()
                R.id.rb_perspective -> mCapturePlayerView.switchPerspectiveMode()
            }
        }

        mSpinnerResolution = findViewById(R.id.spinner_resolution)
        val adapter1 = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            InstaCameraManager.getInstance()
                .getSupportedPreviewStreamResolution(InstaCameraManager.PREVIEW_TYPE_NORMAL)
        )
        mSpinnerResolution.adapter = adapter1
        mSpinnerResolution.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                mCurrentResolution = adapter1.getItem(position)
                InstaCameraManager.getInstance().closePreviewStream()
                InstaCameraManager.getInstance().startPreviewStream(mCurrentResolution)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        mSpinnerStabType = findViewById(R.id.spinner_stab_type)
        val adapter2 = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf(
                getString(R.string.stab_type_auto),
                getString(R.string.stab_type_panorama),
                getString(R.string.stab_type_calibrate_horizon),
                getString(R.string.stab_type_footage_motion_smooth),
                getString(R.string.stab_type_off)
            )
        )
        mSpinnerStabType.adapter = adapter2
        mSpinnerStabType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                if (position == 4 && mCapturePlayerView.isStabEnabled() || position != 4 && !mCapturePlayerView.isStabEnabled()) {
                    InstaCameraManager.getInstance().closePreviewStream()
                    mCurrentResolution?.let {
                        InstaCameraManager.getInstance().startPreviewStream(it)
                    } ?: InstaCameraManager.getInstance().startPreviewStream()
                } else {
                    mCapturePlayerView.setStabType(getStabType())
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        val isNanoS =
            TextUtils.equals(InstaCameraManager.getInstance().cameraType, CameraType.NANOS.type)
        mSpinnerStabType.visibility = if (isNanoS) View.GONE else View.VISIBLE
    }

    private fun getStabType(): Int {
        return when (mSpinnerStabType.selectedItemPosition) {
            1 -> InstaStabType.STAB_TYPE_PANORAMA
            2 -> InstaStabType.STAB_TYPE_CALIBRATE_HORIZON
            3 -> InstaStabType.STAB_TYPE_FOOTAGE_MOTION_SMOOTH
            else -> InstaStabType.STAB_TYPE_AUTO
        }
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            InstaCameraManager.getInstance().setPreviewStatusChangedListener(null)
            InstaCameraManager.getInstance().closePreviewStream()
            mCapturePlayerView.destroy()
        }
    }

    override fun onOpening() {
        mBtnSwitch.isChecked = true
    }

    override fun onOpened() {
        InstaCameraManager.getInstance().setStreamEncode()
        mCapturePlayerView.setPlayerViewListener(object : PlayerViewListener {
            override fun onLoadingFinish() {
                InstaCameraManager.getInstance().setPipeline(mCapturePlayerView.pipeline)
            }

            override fun onReleaseCameraPipeline() {
                InstaCameraManager.getInstance().setPipeline(null)
            }
        })
        mCapturePlayerView.prepare(createParams())
        mCapturePlayerView.play()
        mCapturePlayerView.keepScreenOn = true
    }

    private fun createParams(): CaptureParamsBuilder {
        val builder = PreviewParamsUtil.getCaptureParamsBuilder()
            .setStabType(getStabType())
            .setStabEnabled(mSpinnerStabType.selectedItemPosition != 4)
        mCurrentResolution?.let {
            builder.setResolutionParams(it.width, it.height, it.fps)
        }
        if (mRbPlane.isChecked) {
            builder.setRenderModelType(CaptureParamsBuilder.RENDER_MODE_PLANE_STITCH)
                .setScreenRatio(2, 1)
        } else {
            builder.setRenderModelType(CaptureParamsBuilder.RENDER_MODE_AUTO)
        }
        return builder
    }

    override fun onIdle() {
        mCapturePlayerView.destroy()
        mCapturePlayerView.keepScreenOn = false
    }

    override fun onError() {
        mBtnSwitch.isChecked = false
    }

    override fun onCameraPreviewStreamParamsChanged(
        baseCamera: BaseCamera,
        isPreviewStreamParamsChanged: Boolean
    ) {
        Log.d(TAG, "liveStreamParams isPreviewStreamParamsChanged: $isPreviewStreamParamsChanged")
        if (!isPreviewStreamParamsChanged) {
            Log.d(TAG, "liveStreamParams has nothing changed, ignored")
            return
        }

        val curWindowCropInfo = mCapturePlayerView.windowCropInfo
        val cameraWindowCropInfo =
            PreviewParamsUtil.windowCropInfoConversion(baseCamera.windowCropInfo)

        if (mCapturePlayerView.isPlaying && curWindowCropInfo != null && cameraWindowCropInfo != null) {
            if (curWindowCropInfo.srcWidth != cameraWindowCropInfo.srcWidth ||
                curWindowCropInfo.srcHeight != cameraWindowCropInfo.srcHeight ||
                curWindowCropInfo.desWidth != cameraWindowCropInfo.desWidth ||
                curWindowCropInfo.desHeight != cameraWindowCropInfo.desHeight ||
                curWindowCropInfo.offsetX != cameraWindowCropInfo.offsetX ||
                curWindowCropInfo.offsetY != cameraWindowCropInfo.offsetY
            ) {

                Log.d(TAG, "liveStreamParams changed windowCropInfo: ${baseCamera.windowCropInfo}")
                mCapturePlayerView.setWindowCropInfo(cameraWindowCropInfo)
                mCapturePlayerView.setOffset(
                    OffsetData(
                        baseCamera.mediaOffset,
                        baseCamera.mediaOffsetV2,
                        baseCamera.mediaOffsetV3
                    )
                )
            }
        }
    }
}