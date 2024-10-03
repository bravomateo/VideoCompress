package com.example.videoresolution.insta360.activity


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment

import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.constraintlayout.widget.Group

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton

import com.arashivision.sdkmedia.export.ExportUtils
import com.arashivision.sdkmedia.export.ExportVideoParamsBuilder
import com.arashivision.sdkmedia.export.IExportCallback

import com.arashivision.sdkmedia.work.WorkWrapper
import com.example.videoresolution.R

import java.util.Locale


class PlayAndExportActivity : BaseObserveCameraActivity(), IExportCallback {

    companion object {
        private const val WORK_URLS = "CAMERA_FILE_PATH"
        private val EXPORT_DIR_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/SDK_DEMO_EXPORT/"

        fun launchActivity(context: Context, urls: Array<String>) {
            val intent = Intent(context, PlayAndExportActivity::class.java)
            intent.putExtra(WORK_URLS, urls)
            context.startActivity(intent)
        }
    }

    //private lateinit var mVideoPlayerView: InstaVideoPlayerView
    private lateinit var mRbNormal: RadioButton
    private lateinit var mGroupProgress: Group
    private lateinit var mTvCurrent: TextView
    private lateinit var mTvTotal: TextView
    private lateinit var mSeekBar: SeekBar
    private lateinit var mWorkWrapper: WorkWrapper


    private var mExportDialog: MaterialDialog? = null
    private var mCurrentExportId: Int = -1



    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play_and_export)
        setTitle(R.string.play_toolbar_title)

        val urls = intent.getStringArrayExtra(WORK_URLS)
        if (urls == null) {
            finish()
            Toast.makeText(this, R.string.play_toast_empty_path, Toast.LENGTH_SHORT).show()
            return
        }

        mWorkWrapper = WorkWrapper(urls)
        bindViews()

        findViewById<View>(R.id.btn_export_original).setOnClickListener {
            if(mWorkWrapper.isVideo) {
                exportVideoOriginal()
            }
            showExportDialog()
        }
    }

    private fun bindViews() {
        mGroupProgress = findViewById(R.id.group_progress)
        mTvCurrent = findViewById(R.id.tv_current)
        mTvTotal = findViewById(R.id.tv_total)
        mSeekBar = findViewById(R.id.seek_bar)

        mSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                //mVideoPlayerView.pause()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // mVideoPlayerView?.seekTo(seekBar.progress.toLong())
            }
        })

        mRbNormal = findViewById(R.id.rb_normal)

        findViewById<RadioGroup>(R.id.rg_image_mode).setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rb_normal) {
                //playVideo(false)
                //mVideoPlayerView.switchNormalMode()
            }
        }

    }


    // Función encargada de  reproducir el video
    /*
    private fun playVideo(isPlaneMode: Boolean) {
        mGroupProgress.visibility = View.VISIBLE

        mVideoPlayerView.visibility = View.VISIBLE


        mVideoPlayerView.setPlayerViewListener(object : PlayerViewListener {
            override fun onLoadingStatusChanged(isLoading: Boolean) {}

            override fun onLoadingFinish() {
                Toast.makeText(this@PlayAndExportActivity, R.string.play_toast_load_finish, Toast.LENGTH_SHORT).show()
            }

            override fun onFail(errorCode: Int, errorMsg: String) {
                val toast = getString(R.string.play_toast_fail_desc, errorCode, errorMsg)
                Toast.makeText(this@PlayAndExportActivity, toast, Toast.LENGTH_LONG).show()
            }
        })


        mVideoPlayerView.setVideoStatusListener(object : VideoStatusListener {
            override fun onProgressChanged(position: Long, length: Long) {
                mSeekBar.max = length.toInt()
                mSeekBar.progress = position.toInt()
                mTvCurrent.text = TimeFormat.durationFormat(position)
                mTvTotal.text = TimeFormat.durationFormat(length)
            }

            override fun onPlayStateChanged(isPlaying: Boolean) {}

            override fun onSeekComplete() {mVideoPlayerView.resume()}

            override fun onCompletion() {}

        })

        mVideoPlayerView.setGestureListener(object : PlayerGestureListener {
            override fun onTap(e: MotionEvent): Boolean {
                if (mVideoPlayerView.isPlaying) {
                    mVideoPlayerView.pause()
                } else if (!mVideoPlayerView.isLoading && !mVideoPlayerView.isSeeking) {
                    mVideoPlayerView.resume()
                }
                return false
            }
        })


        val builder = VideoParamsBuilder().setWithSwitchingAnimation(true)

        if (isPlaneMode) {
            builder.renderModelType = VideoParamsBuilder.RENDER_MODE_PLANE_STITCH
            builder.setScreenRatio(2, 1)
        }

        mVideoPlayerView.prepare(mWorkWrapper, builder)
        mVideoPlayerView.play()
    }
    */

    private fun exportVideoOriginal() {
        val builder = ExportVideoParamsBuilder()
            .setExportMode(ExportUtils.ExportMode.PANORAMA)
            .setTargetPath("$EXPORT_DIR_PATH${System.currentTimeMillis()}.mp4")
            .setWidth(2048)
            .setHeight(1024)
        mCurrentExportId = ExportUtils.exportVideo(mWorkWrapper, builder, this)
    }

    private fun stopExport() {
        if (mCurrentExportId != -1) {
            ExportUtils.stopExport(mCurrentExportId)
            mCurrentExportId = -1
        }
    }
    private fun showExportDialog() {
        if (mExportDialog == null) {
            mExportDialog = MaterialDialog(this).apply {
                cancelable(false)
                cancelOnTouchOutside(false)
                positiveButton(R.string.export_dialog_ok) { }
                neutralButton(R.string.export_dialog_stop) {
                    stopExport()
                }
            }
        }
        mExportDialog?.apply {
            message(R.string.export_dialog_msg_exporting)
            getActionButton(WhichButton.POSITIVE).visibility = View.GONE
            show()
        }
    }

    override fun onSuccess() {
        mExportDialog?.apply {
            message(text = getString(R.string.export_dialog_msg_export_success, EXPORT_DIR_PATH))
            getActionButton(WhichButton.POSITIVE).visibility = View.VISIBLE
            getActionButton(WhichButton.NEUTRAL).visibility = View.GONE
        }
        mCurrentExportId = -1
    }

    override fun onFail(errorCode: Int, errorMsg: String) {
        mExportDialog?.apply {
            message(text = getString(R.string.export_dialog_msg_export_failed, errorCode))
            getActionButton(WhichButton.POSITIVE).visibility = View.VISIBLE
            getActionButton(WhichButton.NEUTRAL).visibility = View.GONE
        }
        mCurrentExportId = -1
    }

    override fun onCancel() {
        mExportDialog?.apply {
            message(R.string.export_dialog_msg_export_stopped)
            getActionButton(WhichButton.POSITIVE).visibility = View.VISIBLE
            getActionButton(WhichButton.NEUTRAL).visibility = View.GONE
        }
        mCurrentExportId = -1
    }


    override fun onProgress(progress: Float) {
        mExportDialog?.apply {
            message(text = getString(R.string.export_dialog_msg_export_progress, String.format(Locale.CHINA, "%.1f", progress * 100) + "%"))
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        //mVideoPlayerView.destroy()
    }
}