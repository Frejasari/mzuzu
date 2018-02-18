package de.sari.mzuzu

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.jakewharton.threetenabp.AndroidThreeTen
import io.fabric.sdk.android.Fabric

class MzuzuApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
        AndroidThreeTen.init(this)
    }
}