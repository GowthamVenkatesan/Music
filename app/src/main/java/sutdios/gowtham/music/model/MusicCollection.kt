package sutdios.gowtham.music.model

import android.content.Context
import kotlinx.coroutines.*
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicBoolean

object MusicLibrary {

    lateinit var mCollection: Collection

    var mStartedLoading: AtomicBoolean = AtomicBoolean(false)
    var mLoaded = false

    var mScope = CoroutineScope(Dispatchers.IO)
    var mLoadJob: Job? = null

    suspend fun checkAndLoadIfNeeded(context: Context) {
        if (!mLoaded) {
            if (mStartedLoading.compareAndSet(false, true)) {
                load(context)
                mLoaded = true
            }
            else {
                // Someone is already loading it for us, wait for him to complete
                mLoadJob?.join()
            }
        }
    }

    suspend fun load(context: Context) {
        mCollection = queryMusic(context)!!
    }

    suspend fun getCollection(context: Context): Collection {
        val m = mScope.async {
            checkAndLoadIfNeeded(context)
        }
        m.await()
        return mCollection
    }

    data class Collection(
        val songs: ArrayList<MediaItem>,
        val albums: ArrayList<MediaItem>,
        val artists: ArrayList<MediaItem>
    )
}
