package sutdios.gowtham.music.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.coroutineScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

import sutdios.gowtham.music.R
import sutdios.gowtham.music.model.MediaItem
import sutdios.gowtham.music.model.MusicLibrary

class MusicBrowserFragment : Fragment(), GroupedMusicView.Callbacks {

    val LOG_TAG = "MusicBrowserFragment"

    var mListener: Callbacks? = null

    lateinit var musicCollection: MusicLibrary.Collection

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(LOG_TAG, "onCreate()")
        super.onCreate(savedInstanceState)

        Log.i(LOG_TAG, "loading music")
        lifecycle.coroutineScope.launch {
            musicCollection = MusicLibrary.getCollection(activity!!.applicationContext)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(LOG_TAG, "onCreateView()")
        return inflater.inflate(R.layout.fragment_music_browser, container, false)
    }

    override fun onAttach(context: Context) {
        Log.i(LOG_TAG, "onAttach()")
        super.onAttach(context)
        if (context is Callbacks) {
            mListener = context
        } else {
            throw RuntimeException("$context must implement Callbacks")
        }
    }

    override fun onDetach() {
        Log.i(LOG_TAG, "onDetach()")
        super.onDetach()
        mListener = null
    }

    interface Callbacks {
    }

    companion object {
        @JvmStatic
        fun newInstance() = ConfigurationFragment().apply {}
    }

    override fun onMediaItemClick(item: MediaItem) {
        if (item.children != null) {
            when(item.type) {
                MediaItem.Type.ALBUM ->
                    MusicBrowserFragmentDirections.actionMusicBrowserFragmentToGroupedMusicView(
                        GroupedMusicView.MusicGroup.THIS_ALBUM.toString(),
                        item.title
                    )
                MediaItem.Type.ARTIST ->
                    MusicBrowserFragmentDirections.actionMusicBrowserFragmentToGroupedMusicView(
                        GroupedMusicView.MusicGroup.THIS_ARTIST.toString(),
                        item.title
                    )
            }
        }
        else {
            // TODO: start playing
        }
    }
}
