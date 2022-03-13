package sutdios.gowtham.music.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import sutdios.gowtham.music.R
import sutdios.gowtham.music.run.Player
import sutdios.gowtham.music.ui.MainActivity

class PlaybackService : Service(), Player.Callbacks {

    val NOTIFICATION_ID: Int = 37

    val mBinder = PlaybackServiceBinder()

    inner class PlaybackServiceBinder: Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(getString(R.string.channel_name), name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun updateNotification(state: Player.Callbacks.PlaybackState?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        val notificattion = NotificationCompat.Builder(this, "Channel").run {
            setSmallIcon(R.drawable.ic_launcher_foreground)
            if (state != null) setContentTitle(state.song) else setContentTitle("Playback")
            setContentIntent(pendingIntent)
            build()
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notificattion)
    }

    override fun onPlaybackStateChanged(state: Player.Callbacks.PlaybackState) {
        updateNotification(state)
    }
}
