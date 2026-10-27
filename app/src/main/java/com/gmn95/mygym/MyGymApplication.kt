package com.gmn95.mygym

import android.app.Application
import android.content.Context
import com.gmn95.mygym.data.local.MyGymDatabase
import com.gmn95.mygym.data.remote.ExerciseCatalogRepository
import com.gmn95.mygym.data.remote.WgerCatalogRepository
import com.gmn95.mygym.data.backup.WorkoutBackupRepository
import com.gmn95.mygym.data.repository.RoomWorkoutRepository
import com.gmn95.mygym.data.repository.RoomSessionRepository
import com.gmn95.mygym.data.repository.SessionRepository
import com.gmn95.mygym.data.repository.WorkoutRepository
import com.gmn95.mygym.data.repository.AppSettingsRepository
import com.gmn95.mygym.domain.session.AndroidSessionClock
import com.gmn95.mygym.domain.session.SessionClock
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.CachePolicy

class MyGymApplication : Application(), SingletonImageLoader.Factory {
    val database: MyGymDatabase by lazy {
        MyGymDatabase.create(applicationContext)
    }

    val workoutRepository: WorkoutRepository by lazy {
        RoomWorkoutRepository(database)
    }

    val sessionClock: SessionClock by lazy {
        AndroidSessionClock(applicationContext)
    }

    val sessionRepository: SessionRepository by lazy {
        RoomSessionRepository(database, sessionClock)
    }

    val workoutBackupRepository: WorkoutBackupRepository by lazy {
        WorkoutBackupRepository(database, sessionClock)
    }

    val appSettingsRepository: AppSettingsRepository by lazy {
        AppSettingsRepository(database.appSettingsDao())
    }

    val exerciseCatalogRepository: ExerciseCatalogRepository by lazy {
        WgerCatalogRepository()
    }

    override fun newImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
        // A mídia do catálogo remoto não deve ser persistida pelo aplicativo.
        .diskCachePolicy(CachePolicy.DISABLED)
        .build()
}
