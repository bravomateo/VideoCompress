package com.example.videoresolution.videoEdit.util

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface VideoDao {
    @Query("SELECT * FROM video")
    fun getAll(): List<Video>
    @Insert
    fun insertAll(vararg videos: Video)

}