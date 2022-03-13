package sutdios.gowtham.music.model

import java.util.ArrayList

data class MediaItem(
    val title: String,
    val track: Int,
    val year: Int,
    val album: String,
    val artist: String,
    val type: Type,
    val children: ArrayList<MediaItem>?
) {
    enum class Type {
        SONG, ALBUM, ARTIST, GENRE, PLAYLIST
    }
}
