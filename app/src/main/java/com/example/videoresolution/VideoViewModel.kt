package com.example.videoresolution

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

enum class UploadStatus {
    NOT_UPLOADED,
    UPLOADING,
    UPLOADED
}

class VideoViewModel : ViewModel() {
    private val _uploadStatus = MutableLiveData<UploadStatus>()
    val uploadStatus: LiveData<UploadStatus> = _uploadStatus

    fun uploadVideoToServer(videoFile: File) {
        // Aquí realiza la lógica para cargar el video

        // Cuando la carga está completa, actualiza el estado del LiveData
        _uploadStatus.value = UploadStatus.UPLOADED
    }
}
