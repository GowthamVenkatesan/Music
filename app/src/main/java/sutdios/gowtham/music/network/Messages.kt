package sutdios.gowtham.music.network

import android.net.nsd.NsdServiceInfo
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.Socket
import java.nio.ByteBuffer

open class Message(val type: Type) {

    /**
     * payload for a message is calculated once only and cached
     * a message is never intended to be converted and reused
     * always create new messages if needed
     */
    var mHeader: ByteBuffer? = null
    var mPayload: ByteBuffer? = null

    fun getHeader(): ByteBuffer {
        if (mHeader != null) return mHeader as ByteBuffer
        else {
            mHeader = _getHeader()
            return mHeader as ByteBuffer
        }
    }

    // calculated from payload
    internal fun _getHeader(): ByteBuffer {
        Log.i(LOG_TAG, "_getHeader()")

        val header = ByteBuffer.allocate(HEADER_SIZE)
        header.putInt(type.ordinal)
        header.putInt(getPayload().array().size)
        header.rewind()
        return header
    }

    fun getPayload(): ByteBuffer {
        if (mPayload != null) return mPayload as ByteBuffer
        else  {
            mPayload = _getPayload()
            return mPayload as ByteBuffer
        }
    }

    // override this
    internal open fun _getPayload(): ByteBuffer {
        Log.i(LOG_TAG, "_getPayload()")
        return ByteBuffer.allocate(0)
    }

    fun send(service: Service): Boolean {
        Log.i(LOG_TAG, "send()")
        return Companion.send(this, service)
    }

    companion object {

        val LOG_TAG = "Message"

        val HEADER_SIZE = 4 + 4 // type + size

        fun send(message: Message, service: Service): Boolean {
            Log.i(LOG_TAG, "_send()")

            if (service.state != Service.State.CONNECTED) {
                Log.e(LOG_TAG, "Attempting to send message to a service that is not yet connected! $service")
                return false
            }
            try {
                service.socket!!.getOutputStream().write(message.getHeader().array())
                service.socket!!.getOutputStream().write(message.getPayload().array())
                return true
            }
            catch (e: IOException) {
                Log.e(LOG_TAG, "Error sending message: $this", e)
            }
            return false
        }

        fun receive(socket: Socket): Message? {
            Log.i(LOG_TAG, "_receive()")


            try {
                val headerBuf = ByteArray(HEADER_SIZE)
                socket.getInputStream().read(headerBuf)
                val header = ByteBuffer.wrap(headerBuf)
                val type = Type.fromValue(header.getInt())
                val size = header.getInt()
                val payloadBuf = ByteArray(size)
                socket.getInputStream().read(payloadBuf)
                val payload = ByteBuffer.wrap(payloadBuf)

                if (type!! == Type.STREAM) {
                    return Stream.fromByteBuffer(payload)
                }
                else if (type == Type.CONTROL) {
                    return Control.fromByteBuffer(payload)
                }
                else if (type == Type.SYNC) {
                    return Sync.fromByteBuffer(payload)
                }
                else {
                    Log.e(LOG_TAG, "Unknown message type: $type")
                    return null
                }
            }
            catch (e: IOException) {
                Log.e(LOG_TAG, "Error reading message", e)
                return null
            }
        }

        fun receive(service: Service): Message? {
            if (service.state != Service.State.CONNECTED) {
                Log.e(LOG_TAG, "Attempting to receive messages from a service that is not yet connected! $service")
                return null
            }
            return receive(service.socket!!)
        }

        fun readBlock(buffer: ByteBuffer): ByteBuffer {
            Log.i(LOG_TAG, "readBlock()")

            val size = buffer.getInt()
            val block = ByteArray(size)
            buffer.get(block)
            return ByteBuffer.wrap(block)
        }

        fun writeBlock(buffer: ByteBuffer): ByteBuffer {
            Log.i(LOG_TAG, "writeBlock()")

            val payloadSize = buffer.array().size
            val block = ByteBuffer.wrap(ByteArray(4 + payloadSize))
            block.putInt(payloadSize)
            block.put(buffer)
            block.rewind()
            return block
        }
    }

    override fun toString(): String {
        return "Message<${type}>"
    }

    enum class Type(val value: Int) {
        STREAM(0), SYNC(1), CONTROL(2);

        companion object {
            val values = hashMapOf(*values().map { it.value to it }.toTypedArray())

            fun fromValue(value: Int): Type? {
                return values[value]
            }
        }
    }
}

class Stream(
    val songName: String,
    val audioBuffer: ByteBuffer
): Message(Type.STREAM) {

    companion object {

        val LOG_TAG = "Stream"

        fun streamSong(song: String, audioStream: InputStream, service: Service): Boolean {
            val audioBuffer = ByteBuffer.wrap(audioStream.readBytes())
            return Stream(song, audioBuffer).send(service)
        }

        fun toByteBuffer(message: Stream): ByteBuffer {
            Log.i(LOG_TAG, "toByteBuffer()")

            val songNameBlock = writeBlock(ByteBuffer.wrap(message.songName.toByteArray()))
            message.audioBuffer.rewind() // the full buffer only contains the song data is not reused
            val audioBufferBlock = writeBlock(message.audioBuffer)

            val payloadSize = songNameBlock.array().size + audioBufferBlock.array().size
            val payload = ByteBuffer.allocate(payloadSize)
            payload.put(songNameBlock)
            payload.put(audioBufferBlock)
            payload.rewind()
            return payload
        }

        fun fromByteBuffer(buffer: ByteBuffer): Stream {
            Log.i(LOG_TAG, "fromByteBuffer()")

            val songNameBlock = readBlock(buffer)
            val songName = String(songNameBlock.array())
            val audioBuffer = readBlock(buffer)

            return Stream(
                songName,
                audioBuffer
            )
        }
    }

    override fun _getPayload(): ByteBuffer {
        Log.i(LOG_TAG, "_getPayload()")
        return toByteBuffer(this)
    }
}

class Sync(): Message(Type.SYNC) {

    companion object {

        val LOG_TAG = "Sync"

        fun toByteBuffer(message: Sync): ByteBuffer {
            Log.i(LOG_TAG, "toByteBuffer()")
            return ByteBuffer.allocate(0)
        }

        fun fromByteBuffer(buffer: ByteBuffer): Sync {
            Log.i(LOG_TAG, "fromByteBuffer()")
            return Sync()
        }
    }

    override fun _getPayload(): ByteBuffer {
        return toByteBuffer(this)
    }
}

class Control(val action: Action, val time: Long, val service: Service?, val additionalInfo: String): Message(Type.CONTROL) {

    enum class Action(val value: String) {
        PREPARE("prepare"), PLAY("prepare"), PAUSE("pause"), STOP("stop"),
        HELLO("hello"), DISCONNECT("disconnect"),
        REQ_SHARE_SONG("reqShareSong"),
        QUERY_SONG("querySong"), QUERY_RESULT("querySongResult"),
        MESSAGE_BROADCAST("messgaeBroadcast");

        override fun toString(): String {
            return value
        }

        companion object {

            val values = hashMapOf(*values().map { it.value to it }.toTypedArray())

            fun fromValue(value: String): Action? {
                return values[value]
            }
        }
    }

    override fun _getPayload(): ByteBuffer {
        return toByteBuffer(this)
    }

    companion object {

        val LOG_TAG = "Control"

        fun hello(us: Service, toService: Service): Boolean {
            return Control(Action.HELLO, -1, us, "").send(toService)
        }

        fun disconnect(service: Service): Boolean {
            return Control(Action.DISCONNECT, -1, null, "").send(service)
        }

        fun prepare(service: Service, song: String): Boolean {
            return Control(Action.PREPARE, -1L, null, song).send(service)
        }

        fun play(service: Service, time: Long): Boolean {
            return Control(Action.PLAY, time, null, "").send(service)
        }

        fun pause(service: Service): Boolean {
            return Control(Action.PAUSE, -1, null, "").send(service)
        }

        fun stop(service: Service): Boolean {
            return Control(Action.STOP, -1, null, "").send(service)
        }

        fun requestShareSongTo(srcService: Service, destService: Service, song: String): Boolean {
            return Control(Action.REQ_SHARE_SONG, -1, destService, song).send(srcService)
        }

        fun querySong(service: Service, song: String): Boolean {
            return Control(Action.QUERY_SONG, -1, null, song).send(service)
        }

        fun queryResult(service: Service, result: QueryResult): Boolean {
            return Control(Action.QUERY_RESULT, -1, null, result.name).send(service)
        }

        fun toByteBuffer(message: Control): ByteBuffer {
            Log.i(LOG_TAG, "toByteBuffer()")
            // name::deviceType::ipAddr::port
            var serviceInfo = ""
            if (message.service != null) {
                val thisService = message.service
                serviceInfo = arrayOf(thisService.name, thisService.deviceType.toString(), thisService.serviceInfo.host?.toString(), thisService.serviceInfo.port.toString()).joinToString("::")
            }
            val payloadSize = (4 + message.action.value.toByteArray().size) + 8 + (4 + serviceInfo.toByteArray().size) + (4 + message.additionalInfo.toByteArray().size)
            val payload = ByteBuffer.allocate(payloadSize)
            payload.put(writeBlock(ByteBuffer.wrap(message.action.value.toByteArray())))
            payload.putLong(message.time)
            payload.put(writeBlock(ByteBuffer.wrap(serviceInfo.toByteArray())))
            payload.put(writeBlock(ByteBuffer.wrap(message.additionalInfo.toByteArray())))
            payload.rewind()
            return payload
        }

        fun fromByteBuffer(buffer: ByteBuffer): Control {
            Log.i(LOG_TAG, "fromByteBuffer()")

            val actionBlock = readBlock(buffer)
            val action = Action.fromValue(String(actionBlock.array()))
            val time = buffer.getLong()
            val serviceInfoString = String(readBlock(buffer).array())
            var service: Service? = null
            if (serviceInfoString.isNotEmpty()) {
                val info = serviceInfoString.split("::")
                val nsdServiceInfo = NsdServiceInfo().apply {
                    host = InetAddress.getByName(info[2])
                    port = info[3].toInt()
                }
                service = Service(info[0], sutdios.gowtham.music.network.Type.valueOf(info[1]), nsdServiceInfo, null, Service.State.DISCONNECTED)

            }
            val additionalInfo = String(readBlock(buffer).array())
            return Control(action!!, time, service, additionalInfo)
        }

        enum class QueryResult {
            PRESENT, NOT_PRESENT
        }
    }
}
