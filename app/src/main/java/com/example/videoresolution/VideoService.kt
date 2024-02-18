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
    ): Call<MainActivity.YourResponseModelVideo>

    @FormUrlEncoded
    @POST("upload_info/")
    fun uploadInfo(
        @Field("farm") farm: String,
        @Field("block") block: String,
        @Field("bed") bed: String,
        @Field("video_name") video_name: String,
        @Field("resolution") resolution: String,
        @Field("FPS") FPS: String,
        @Field("ID") ID: String,
    ): Call<MainActivity.YourResponseModel>
    
    @GET("/mapper/manageBlocks/VF/")
    fun getBlocks(): Call<List<BlockItem>>

    @GET("/users/farms/")
    fun getFarms(): Call<FarmResponse>
}

class BlockItem(
    @SerializedName("BLOCK_NUMBER") val blockNumber: String,
    @SerializedName("IS_ACTIVE") val isActive: Boolean,
    @SerializedName("AREA") val area: Double
)
data class FarmResponse(
    @SerializedName("Farms") val farms: List<FarmItem>
)

data class FarmItem(
    @SerializedName("name") val name: String
)