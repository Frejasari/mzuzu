package de.sari.mzuzu

import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator


class MeditationJobCreator : JobCreator {
    override fun create(tag: String): Job? {
        return when (tag) {
            StartMusicJob.TAG -> StartMusicJob()
            else -> null
        }
    }
}
