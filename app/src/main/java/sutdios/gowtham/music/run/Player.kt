package sutdios.gowtham.music.run

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

object Player: MediaPlayer.OnErrorListener {

    const val LOG_TAG = "Player"

    var mContext: WeakReference<Context>? = null

    var mPlayer: MediaPlayer? = null
    var mState = State.RESET
    var mAudioFocusState: Int = AudioManager.AUDIOFOCUS_LOSS

    val mScope = CoroutineScope(newSingleThreadContext("PlayerCoroutine"))

    fun initialize(context: Context) = mScope.launch {
        Log.i(LOG_TAG, "initialize()")

        mPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        }
        getAudioFocus()
    }

    fun prepare(song: SongsCache.Song) = mScope.launch {
        Log.i(LOG_TAG, "prepare() $song")

        if (mState != State.RESET) {
            if (mState == State.PLAYING) {
                mPlayer!!.stop()
            }
            mPlayer!!.reset()
        }

        mPlayer!!.setDataSource(song)
        mPlayer!!.prepare() // We'll be running on a separate thread
        mState = State.PREPARED
    }

    fun play() = mScope.launch {
        Log.i(LOG_TAG, "play()")

        if (mState in arrayOf(State.PREPARED, State.PAUSED)) {
            if (mAudioFocusState != AudioManager.AUDIOFOCUS_GAIN) {
                Log.i(LOG_TAG, "Starting playback with 0 volume, we do not have audioFocus")
                mPlayer!!.setVolume(0.0f, 0.0f)
            }
            mPlayer!!.start()
            mState = State.PLAYING
        }
        else {
            Log.e(LOG_TAG, "Attempting to prepare at state: $mState")
        }
    }

    fun pause() = mScope.launch {
        Log.i(LOG_TAG, "pause()")
        if (mState == State.PLAYING) {
            mPlayer!!.pause()
            mState = State.PAUSED
        }
    }

    fun stop() = mScope.launch {
        Log.i(LOG_TAG, "stop()")
        if (mState == State.RESET) {
            mPlayer!!.reset()
            mState = State.RESET
        }
    }

    fun release() = mScope.launch {
        Log.i(LOG_TAG, "release()")

        if (mPlayer != null) {
            if (mState == State.PLAYING) {
                mPlayer!!.stop()
            }
            mPlayer!!.release()
        }

        runBlocking {
            mScope.cancel()
        }
    }

    fun setVolume(left: Float, right: Float) {
        mPlayer!!.setVolume(left, right)
    }

    fun duck() {
        mPlayer!!.setVolume(0.3f, 0.3f)
    }

    fun restoreVolume() {
        mPlayer!!.setVolume(1.0f, 1.0f)
    }

    internal fun getAudioFocus() {
        val audioManager = mContext!!.get()!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
            setAudioAttributes(
                AudioAttributes.Builder().run {
                    setUsage(AudioAttributes.USAGE_MEDIA)
                    setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    build()
                }
            )
            setAcceptsDelayedFocusGain(true)
            setOnAudioFocusChangeListener {
                mAudioFocusState = it
                when(it) {
                    AudioManager.AUDIOFOCUS_LOSS -> mScope.launch { stop() }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> mScope.launch { stop() }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> mScope.launch { duck() }
                    AudioManager.AUDIOFOCUS_GAIN -> mScope.launch { restoreVolume() }
                }
            }
        }
    }

    enum class State {
        RESET, PREPARED, PLAYING, PAUSED
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        Log.e(LOG_TAG, "MediaPlayer error! what: $what extra: $extra")
        return false
    }

    interface Callbacks {

        fun onPlaybackStateChanged(state: PlaybackState)

        data class PlaybackState(
            val song: String,
            val state: State
        )
    }
}