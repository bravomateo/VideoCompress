package com.example.videoresolution

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MyViewModel : ViewModel() {
    val loading = MutableLiveData<Boolean>()
    val loaded = MutableLiveData<Boolean>()

}
