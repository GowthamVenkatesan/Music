package sutdios.gowtham.music.run

import android.media.MediaDataSource
import java.lang.Integer.max
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

object SongsCache {

    const val LOG_TAG = "SongsCache"

    const val size = 30 * 1024 * 1024 // 30 MB max
    var currentSize = 0

    val queue = ConcurrentLinkedDeque<Song>()
    val map = ConcurrentHashMap<String, Song>()

    fun hold(song: Song) {
        val thisSongSize = song.audio.array().size
        if (thisSongSize > size) {
            throw RuntimeException("Can't store song!")
        }

        if (map.contains(song.name)) {
            // just ignore!
        }

        while (song.audio.array().size > (size - currentSize)) {
            val removedSong = queue.pop()
            map.remove(removedSong.name)
            currentSize -= removedSong.audio.array().size
        }
        queue.push(song)
        map.put(song.name, song)
    }

    fun getSong(name: String): Song? {
        return map[name]
    }

    class Song(
        val name: String,
        val audio: ByteBuffer
    ): MediaDataSource() {

        val mSize = audio.array().size

        override fun readAt(position: Long, buffer: ByteArray?, offset: Int, size: Int): Int {

            if (position >= mSize) {
                return -1
            }

            val readLength = max(size, (mSize - position.toInt()))
            System.arraycopy(
                audio,
                position.toInt(),
                buffer,
                offset,
                readLength
            )
            return readLength
        }

        override fun getSize(): Long {
            return mSize.toLong()
        }

        override fun close() {
            // do nothing
        }
    }
}