package com.gmn95.mygym.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "theme_mode")
    val themeMode: String = "SYSTEM",
    @ColumnInfo(name = "language_mode")
    val languageMode: String = "SYSTEM",
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
