package de.sari.mzuzu

import android.util.Log
import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator


class MeditationJobCreator : JobCreator {

    override fun create(tag: String): Job? {
        return when (tag) {
            StartMusicJob.TAG -> {
                Log.i("sync", "MeditationJobCreater StartMusicJob called")
                StartMusicJob()
            }
            else -> null
        }
    }
}
