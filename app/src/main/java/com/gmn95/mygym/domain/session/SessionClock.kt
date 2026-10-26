package com.gmn95.mygym.domain.session

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

interface SessionClock {
    fun epochMillis(): Long

    fun elapsedRealtimeMillis(): Long

    fun bootCount(): Int
}

class AndroidSessionClock(
    private val context: Context,
) : SessionClock {
    override fun epochMillis(): Long = System.currentTimeMillis()

    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()

    override fun bootCount(): Int =
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)
}
