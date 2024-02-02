package com.example.videoresolution

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface VideoService {
    @Multipart
    @POST("upload_video/")
    fun uploadVideo(
        @Part video: MultipartBody.Part
    ): Call<MainActivity.YourResponseModel>

    @FormUrlEncoded
    @POST("upload_info/")
    fun uploadInfo(
        @Field("bed") bed: String,
        @Field("farm") farm: String,
        @Field("block") block: String
    ): Call<MainActivity.YourResponseModel>

    @GET("/mapper/manageBlocks/VF/")
    fun getBlocks(): Call<List<BlockItem>>
}

class BlockItem(
    @SerializedName("BLOCK_NUMBER") val blockNumber: String,
    @SerializedName("IS_ACTIVE") val isActive: Boolean,
    @SerializedName("AREA") val area: Double
)