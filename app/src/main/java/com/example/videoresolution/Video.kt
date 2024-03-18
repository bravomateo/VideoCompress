package com.example.videoresolution

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Video(
    @PrimaryKey(autoGenerate = true) val uid: Int? = null,
    @ColumnInfo(name = "name_video") val nameVideo: String?,
    @ColumnInfo(name = "resolution_video") val resolutionVideo: String?
)