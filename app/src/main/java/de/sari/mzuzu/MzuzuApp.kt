package de.sari.mzuzu

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen


class MzuzuApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
    }
}