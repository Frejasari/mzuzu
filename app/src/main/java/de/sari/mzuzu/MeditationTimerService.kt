package de.sari.mzuzu

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder


class MeditationTimerService : Service() {

    inner class MeditationTimerBinder : Binder() {
        fun getService() = this@MeditationTimerService
    }

    override fun onBind(intent: Intent?): IBinder {
        return MeditationTimerBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}