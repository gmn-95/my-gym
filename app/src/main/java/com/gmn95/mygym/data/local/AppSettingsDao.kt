package com.gmn95.mygym.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 0")
    fun observe(): Flow<AppSettingsEntity?>

    @Upsert
    suspend fun upsert(settings: AppSettingsEntity)
}
