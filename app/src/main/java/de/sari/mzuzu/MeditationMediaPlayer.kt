package de.sari.mzuzu

import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer

class MeditationMediaPlayer(musicFile: AssetFileDescriptor) : MediaPlayer() {

    init {
        setDataSource(musicFile.fileDescriptor, musicFile.startOffset, musicFile.length)
        prepareAsync()
    }

    override fun pause() {
        if (isPlaying) super.pause()
    }

    override fun start() {
        seekTo(0)
        super.start()
    }


}