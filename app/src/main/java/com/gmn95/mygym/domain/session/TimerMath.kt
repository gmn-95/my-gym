package com.gmn95.mygym.domain.session

object TimerMath {
    fun activeDurationMillis(
        accumulatedMillis: Long,
        anchorElapsedRealtime: Long?,
        anchorBootCount: Int?,
        nowElapsedRealtime: Long,
        currentBootCount: Int,
    ): Long {
        if (anchorElapsedRealtime == null || anchorBootCount != currentBootCount) {
            return accumulatedMillis.coerceAtLeast(0)
        }
        return (accumulatedMillis + nowElapsedRealtime - anchorElapsedRealtime)
            .coerceAtLeast(accumulatedMillis)
    }

    fun remainingMillis(
        remainingAtAnchorMillis: Long,
        anchorElapsedRealtime: Long?,
        anchorBootCount: Int?,
        nowElapsedRealtime: Long,
        currentBootCount: Int,
    ): Long {
        if (anchorElapsedRealtime == null || anchorBootCount != currentBootCount) {
            return remainingAtAnchorMillis.coerceAtLeast(0)
        }
        return (remainingAtAnchorMillis - (nowElapsedRealtime - anchorElapsedRealtime))
            .coerceAtLeast(0)
    }
}
