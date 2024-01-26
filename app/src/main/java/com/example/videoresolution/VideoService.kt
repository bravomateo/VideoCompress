package com.example.videoresolution

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface VideoService {
    @Multipart
    @POST("upload_video/")
    fun uploadVideo(
        @Part video: MultipartBody.Part
    ): Call<MainActivity.YourResponseModel>
}