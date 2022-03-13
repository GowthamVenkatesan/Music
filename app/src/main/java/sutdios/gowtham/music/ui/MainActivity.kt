package sutdios.gowtham.music.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.navigation.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.ismaeldivita.chipnavigation.ChipNavigationBar
import sutdios.gowtham.music.R
import sutdios.gowtham.music.services.PlaybackService

class MainActivity : AppCompatActivity(),
    ConfigurationFragment.Callbacks,
    MusicBrowserFragment.Callbacks {

    val LOG_TAG = "MainActivity"

    var mCoordinator: CoordinatorLayout? = null
    var mNavRail: ChipNavigationBar? = null
    var mToolbar: MaterialToolbar? = null

    lateinit var mService: PlaybackService
    var mBound: Boolean = false
    
    val connection = object : ServiceConnection {

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(LOG_TAG, "onServiceDisconnected()")
            mBound = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i(LOG_TAG, "onServiceConnected()")
            val binder = service as PlaybackService.PlaybackServiceBinder
            mService = binder.getService()
            mBound = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(LOG_TAG, "onCreate()")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mToolbar = findViewById(R.id.appBar)
        setSupportActionBar(mToolbar)
        mCoordinator = findViewById(R.id.coordinator)
        mNavRail = findViewById(R.id.nav_rail)
        mNavRail?.apply {
            setItemSelected(R.id.nav_rail_configure)
            setOnItemSelectedListener {
                if (it == R.id.nav_rail_configure) {
                    findNavController(R.id.nav_host_fragment).navigate(R.id.configurationFragment)
                }
                else if (it == R.id.nav_rail_music) {
                    findNavController(R.id.nav_host_fragment).navigate(R.id.musicBrowserFragment)
                }
            }
        }
        findNavController(R.id.nav_host_fragment).navigate(R.id.configurationFragment)

        Intent(this, PlaybackService::class.java).also {
            startService(it)
            bindService(it, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onDestroy() {
        Log.i(LOG_TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun onItemClick(position: Int) {
    }
}
