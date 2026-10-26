package com.gmn95.mygym.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AppSettingsEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MyGymDatabase : RoomDatabase() {
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        private const val DATABASE_NAME = "my-gym.db"

        fun create(context: Context): MyGymDatabase =
            Room.databaseBuilder(
                context,
                MyGymDatabase::class.java,
                DATABASE_NAME,
            ).build()
    }
}
