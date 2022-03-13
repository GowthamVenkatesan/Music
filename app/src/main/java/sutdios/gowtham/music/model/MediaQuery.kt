package sutdios.gowtham.music.model

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import java.lang.Exception
import java.util.ArrayList
import java.util.HashMap

const val LOG_TAG = "MediaQuery"

val PROJECTION = arrayOf(
    /* 0 */ MediaStore.Audio.AudioColumns.TITLE,
    /* 1 */ MediaStore.Audio.AudioColumns.TRACK,
    /* 2 */ MediaStore.Audio.AudioColumns.YEAR,
    /* 3 */ MediaStore.Audio.AudioColumns.ALBUM,
    /* 4 */ MediaStore.Audio.AudioColumns.ARTIST
)

fun queryMusic(context: Context): MusicLibrary.Collection? {
    Log.i(LOG_TAG, "queryMusic()")

    var cursor: Cursor? = null
    try {
        cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            PROJECTION,
            null,
            null,
            null
        )
        return processQuery(cursor)
    }
    catch (e: Exception) {
        Log.e(LOG_TAG, "Error querying for music!", e)
    }
    finally {
        cursor?.close()
    }
    return null
}

internal fun processQuery(cursor: Cursor?): MusicLibrary.Collection {
    Log.i(LOG_TAG, "processQuery()")

    val musicLibrary = MusicLibrary.Collection(
        ArrayList(),
        ArrayList(),
        ArrayList()
    )
    val albums = HashMap<String, MediaItem>()
    val artists = HashMap<String, MediaItem>()

    if (cursor != null && cursor.moveToFirst()) {
        do {
            val title = cursor.getString(0)
            val track = cursor.getInt(1)
            val year = cursor.getInt(2)
            val album = cursor.getString(3)
            val artist = cursor.getString(4)

            // create a song
            val thisSong = MediaItem(
                title,
                track,
                year,
                album,
                artist,
                MediaItem.Type.SONG,
                null
            )
            musicLibrary.songs.add(thisSong)

            // push to its album and artist
            if (albums[album] == null) {
                // create it
                val thisAlbum = MediaItem(
                    album,
                    -1,
                    year,
                    album,
                    artist,
                    MediaItem.Type.ALBUM,
                    ArrayList()
                )
                musicLibrary.albums.add(thisAlbum)
                albums[album] = thisAlbum

            }
            albums[album]?.children?.add(thisSong)

            if (artists[artist] == null) {
                // create it
                val thisArtist = MediaItem(
                    artist,
                    -1,
                    year,
                    album,
                    artist,
                    MediaItem.Type.ARTIST,
                    ArrayList()
                )
                musicLibrary.albums.add(thisArtist)
                artists[artist] = thisArtist
            }
            artists[artist]?.children?.add(thisSong)

        } while(cursor.moveToNext())
    }

    return musicLibrary
}
