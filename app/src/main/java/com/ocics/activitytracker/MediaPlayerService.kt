package com.ocics.activitytracker

import android.app.Service
import android.content.Intent
import android.database.Cursor
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaPlayer
import android.media.MediaPlayer.*
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.provider.MediaStore
import android.util.Log


class MediaPlayerService : Service(), OnCompletionListener,
    OnPreparedListener, OnErrorListener, OnSeekCompleteListener, OnInfoListener,
    OnBufferingUpdateListener, OnAudioFocusChangeListener {
    // Binder given to clients
    private val iBinder: IBinder = LocalBinder()
    override fun onBind(intent: Intent): IBinder? {
        return iBinder
    }

    private var mediaPlayer: MediaPlayer? = null
    private val playlist = ArrayList<String>()
    private var songIndex = 0

    override fun onCreate() {
        super.onCreate()
        getPlaylist()
        initMediaPlayer()
    }

    override fun onCompletion(mp: MediaPlayer) {
        mediaPlayer!!.reset()
        playMusic()
    }

    //Handle errors
    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        //Invoked when there has been an error during an asynchronous operation.
        when (what) {
            MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK -> Log.d(
                "MediaPlayer Error",
                "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK $extra"
            )
            MEDIA_ERROR_SERVER_DIED -> Log.d(
                "MediaPlayer Error",
                "MEDIA ERROR SERVER DIED $extra"
            )
            MEDIA_ERROR_UNKNOWN -> Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN $extra")
        }
        return false
    }

    override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        //Invoked to communicate some info.
        return false
    }

    override fun onPrepared(mp: MediaPlayer) {
        playMedia()
    }

    override fun onAudioFocusChange(focusState: Int) {
    }

    override fun onSeekComplete(p0: MediaPlayer?) {
    }

    override fun onBufferingUpdate(mp: MediaPlayer, percent: Int) {
    }

    inner class LocalBinder : Binder() {
        val service: MediaPlayerService
            get() = this@MediaPlayerService
    }

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        }
        //Set up MediaPlayer event listeners
        mediaPlayer!!.setOnCompletionListener(this)
        mediaPlayer!!.setOnErrorListener(this)
        mediaPlayer!!.setOnPreparedListener(this)
        mediaPlayer!!.setOnBufferingUpdateListener(this)
        mediaPlayer!!.setOnSeekCompleteListener(this)
        mediaPlayer!!.setOnInfoListener(this)
        //Reset so that the MediaPlayer is not pointing to another data source
        mediaPlayer!!.reset()
    }

    private fun playMusic() {
        if (playlist.isEmpty()) return
        val dataStream = playlist[songIndex]

        // Move index to next song
        songIndex += 1
        if (songIndex >= playlist.count())
            songIndex = 0

        try {
            mediaPlayer!!.setDataSource(dataStream)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            stopSelf()
        }
        mediaPlayer!!.prepareAsync()
    }

    fun playMedia() {
        if (!mediaPlayer!!.isPlaying) {
            mediaPlayer!!.start()
        }
    }

    fun pauseMedia() {
        if (mediaPlayer!!.isPlaying) {
            mediaPlayer!!.pause()
        }
    }

    // search music files in /music, support mp3 and flac files
    private fun getPlaylist() {
        val args = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.MediaColumns.DATA
        )
        val membersUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val songCursor: Cursor? = contentResolver.query(membersUri, args, null, null, null)
        val songIndex = 0 // PLAYING FROM THE FIRST SONG
        if (songCursor != null) {
            songCursor.moveToPosition(songIndex)
            while (!songCursor.isAfterLast) {
                if (songCursor.getString(3) == "audio/mpeg" || songCursor.getString(3) == "audio/flac")
                    playlist.add(songCursor.getString(4))

                songCursor.moveToNext()
            }

            songCursor.close()
        }

        Log.d("MediaPlayerService", playlist.count().toString())
    }

}