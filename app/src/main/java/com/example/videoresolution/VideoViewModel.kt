package com.example.videoresolution

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File



class VideoViewModel : ViewModel() {
    private val _videoStates = MutableLiveData<Map<Int, VideoState>> ()
    val videoStates: LiveData<Map<Int, VideoState>>
        get() = _videoStates

    fun setVideoState(position: Int, state: VideoState) {
        val currentStates = _videoStates.value?.toMutableMap() ?: mutableMapOf()
        currentStates[position] = state
        _videoStates.value = currentStates
    }
}

