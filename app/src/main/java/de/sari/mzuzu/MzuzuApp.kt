package de.sari.mzuzu

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.jakewharton.threetenabp.AndroidThreeTen
import io.fabric.sdk.android.Fabric
import com.evernote.android.job.JobManager



class MzuzuApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
        AndroidThreeTen.init(this)

        JobManager.create(this).addJobCreator(MeditationJobCreator())
    }
}