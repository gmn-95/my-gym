package com.gmn95.mygym

import android.app.Application
import com.gmn95.mygym.data.local.MyGymDatabase
import com.gmn95.mygym.data.backup.WorkoutBackupRepository
import com.gmn95.mygym.data.repository.RoomWorkoutRepository
import com.gmn95.mygym.data.repository.RoomSessionRepository
import com.gmn95.mygym.data.repository.SessionRepository
import com.gmn95.mygym.data.repository.WorkoutRepository
import com.gmn95.mygym.domain.session.AndroidSessionClock
import com.gmn95.mygym.domain.session.SessionClock

class MyGymApplication : Application() {
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
}
