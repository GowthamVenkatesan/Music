package sutdios.gowtham.music.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

import sutdios.gowtham.music.R
import sutdios.gowtham.music.model.MediaItem
import sutdios.gowtham.music.model.MusicLibrary
import java.util.ArrayList
import java.util.stream.Collectors

class GroupedMusicView : Fragment() {

    val LOG_TAG = "GroupedMusicView"

    val GROUPING = "grouping"
    lateinit var mGrouping: MusicGroup
    val GROUPING_KEY = "groupingKey"
    var mGroupingKey: String? = null

    lateinit var mRecycler: RecyclerView
    private lateinit var mAdapter: SongsListAdapter
    lateinit var mLayoutManager: LinearLayoutManager

    lateinit var musicCollection: MusicLibrary.Collection
    lateinit var mDataset: List<MediaItem>

    private var mListener: Callbacks? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(LOG_TAG, "onCreate()")
        super.onCreate(savedInstanceState)

        Log.i(LOG_TAG, "loading music")
        lifecycle.coroutineScope.launch {
            musicCollection = MusicLibrary.getCollection(activity!!.applicationContext)
        }
        arguments?.let {
            mGrouping = MusicGroup.valueOf(it.getString(GROUPING)!!)
            mGroupingKey = it.getString(GROUPING_KEY)
            when(mGrouping) {
                MusicGroup.SONGS -> mDataset = musicCollection.songs
                MusicGroup.ALBUMS -> mDataset = musicCollection.albums
                MusicGroup.ARTISTS -> mDataset = musicCollection.artists
                MusicGroup.THIS_ALBUM -> mDataset = musicCollection.songs.filter { it.album == mGroupingKey }
                MusicGroup.THIS_ARTIST -> mDataset = musicCollection.songs.filter { it.artist == mGroupingKey }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(LOG_TAG, "onCreateView()")

        val view = inflater.inflate(R.layout.fragment_grouped_music_view, container, false)
        mRecycler = view.findViewById(R.id.grouped_music_view_fragment_recycler)
        mAdapter = SongsListAdapter(mDataset)
        mRecycler.adapter = mAdapter
        mLayoutManager = LinearLayoutManager(context)
        mRecycler.layoutManager = mLayoutManager
        return view
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
        fun onMediaItemClick(item: MediaItem)
    }

    companion object {
        @JvmStatic
        fun newInstance(grouping: String, key: String?) =
            GroupedMusicView().apply {
                arguments = Bundle().apply {
                    putString(GROUPING, grouping)
                    putString(GROUPING_KEY, key)
                }
            }
    }

    enum class MusicGroup {
        SONGS, ALBUMS, ARTISTS,
        THIS_ALBUM, THIS_ARTIST
    }

    private inner class SongsViewHolder internal constructor(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): RecyclerView.ViewHolder(
        inflater.inflate(
            R.layout.service_item_generic,
            parent,
            false
        )
    ) {
        internal val track: TextView = itemView.findViewById(R.id.song_item_generic_track)
        internal val title: TextView = itemView.findViewById(R.id.song_item_generic_song_name)
        internal val album: TextView = itemView.findViewById(R.id.song_item_generic_song_album)
        internal val year: TextView = itemView.findViewById(R.id.song_item_generic_song_year)

        init {
            itemView.setOnClickListener {
                mListener?.onMediaItemClick(mDataset[adapterPosition])
            }
        }
    }

    private inner class SongsListAdapter internal constructor(
        private val songs: List<MediaItem>
    ): RecyclerView.Adapter<SongsViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongsViewHolder {
            return SongsViewHolder(LayoutInflater.from(parent.context), parent)
        }

        override fun getItemCount(): Int {
            return songs.size
        }

        override fun onBindViewHolder(holder: SongsViewHolder, position: Int) {
            holder.track.text = songs[position].track.toString()
            holder.title.text = songs[position].title
            holder.album.text = songs[position].album
            holder.year.text = songs[position].year.toString()
        }
    }
}
