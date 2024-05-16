package com.example.videoresolution

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class MyViewModel : ViewModel() {
    val loading = MutableLiveData<Boolean>()
    val loaded = MutableLiveData<Boolean>()

}