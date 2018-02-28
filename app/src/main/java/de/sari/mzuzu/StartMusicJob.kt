package de.sari.mzuzu

import android.support.v4.content.ContextCompat
import com.evernote.android.job.Job
import com.evernote.android.job.JobRequest


class StartMusicJob : Job() {

    companion object {
        val TAG = "Start Music Job"
        fun schedule(seconds: Int) {
            JobRequest.Builder(TAG)
                    .setExact(TimeUtils.toMillis(seconds))
                    .build()
                    .schedule()
        }
    }

    override fun onRunJob(params: Params): Result {
        val intent = getMeditationTimerServiceIntent(context).apply {
        }
        ContextCompat.startForegroundService(context, intent)
        return Result.SUCCESS
    }

}