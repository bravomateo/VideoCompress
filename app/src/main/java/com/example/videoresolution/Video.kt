package com.example.videoresolution

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Video(
    @PrimaryKey(autoGenerate = true) val uid: Int? = null,
    @ColumnInfo(name = "name_video") val nameVideo: String?,
    @ColumnInfo(name = "resolution_video") val resolutionVideo: String?,
    @ColumnInfo(name = "output_file_path") val outputFilePath: String?,
    @ColumnInfo(name = "original_path") val originalPath: String?,
    @ColumnInfo(name = "start_time") val startTime: Int?,
    @ColumnInfo(name = "end_time") val endTime: Int?,
    @ColumnInfo(name = "width") val width: String?,
    @ColumnInfo(name = "height") val height: String?,
    @ColumnInfo(name = "fps") val fps: String?,
    @ColumnInfo(name = "farm") val farm: String?,
    @ColumnInfo(name = "block") val block: String?,
    @ColumnInfo(name = "bed") val bed: String?


)