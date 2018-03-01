package de.sari.mzuzu

import android.support.v4.content.ContextCompat
import com.evernote.android.job.Job
import com.evernote.android.job.JobRequest

const val ACTION_COMPLETED = "action completed, timer completed, job done"
class StartMusicJob : Job() {

    companion object {
        const val TAG = "Start Music Job"
        fun schedule(seconds: Int) {
            JobRequest.Builder(TAG)
                    .setExact(TimeUtils.toMillis(seconds))
                    .setUpdateCurrent(true)
                    .build()
                    .schedule()
        }
    }

    override fun onRunJob(params: Params): Result {
        val intent = getMeditationTimerServiceIntent(context).apply {
            action = ACTION_COMPLETED
        }
        ContextCompat.startForegroundService(context, intent)
        return Result.SUCCESS
    }

}