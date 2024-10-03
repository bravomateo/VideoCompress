package com.example.videoresolution.videoEdit.util

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Video::class], version = 1, )
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
}