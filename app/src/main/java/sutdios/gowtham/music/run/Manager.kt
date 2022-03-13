package sutdios.gowtham.music.run

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.*
import sutdios.gowtham.music.network.*
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.ServerSocket
import java.util.ArrayList

object Manager: LifecycleObserver {

    const val LOG_TAG = "Manager"
    const val INTERVAL_BETWEEN_READS = 250L // in millis

    var mContext: WeakReference<Context>? = null
    var mType: Type? = null

    val mScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    fun initialize(type: Type, listener: Discovery.Callbacks, context: Context) = mScope.launch {
        Log.i(LOG_TAG, "initialize()")

        mContext = WeakReference(context)
        mType = type

        Player.initialize(context)
        Discovery.initialize(mContext!!.get()!!, listener)
        Discovery.registerService(
            Discovery.createService(type, Discovery.initializeServerSocket()),
            context
        )
        Discovery.startDiscovery(mContext!!.get()!!)
        startListening()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreated() {
        Log.i(LOG_TAG, "onCreated()")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        Log.i(LOG_TAG, "onDestroy()")
        release()
    }

    fun release() {
        Log.i(LOG_TAG, "release()")

        Discovery.release()
        Player.release()
        mScope.cancel()
        mContext = null
    }

    fun startListening() = mScope.launch {
        Log.i(LOG_TAG, "startListening()")

        launch {
            keepListening()
        }
        launch {
            keepListeningForMessages(Discovery.connectedDevices)
        }
    }

    suspend fun keepListening() {
        Log.i(LOG_TAG, "keepListening()")

        while(Discovery.broadcastState == Discovery.BroadcastState.REGISTERED) {
            acceptAConnection(Discovery.serverSocket)
        }
    }

    suspend fun acceptAConnection(serverSocket: ServerSocket) {
        Log.i(LOG_TAG, "acceptAConnection()")

        try {
            val thisSocket = serverSocket.accept()
            Log.i(LOG_TAG, "Got a connection: $thisSocket")

            // read his hello
            val hisHello = Message.receive(thisSocket) as Control
            Log.i(LOG_TAG, "hisHello: $hisHello")
            if (hisHello.action == Control.Action.HELLO) {
                val thisService = hisHello.service
                thisService!!.socket = thisSocket
                Discovery.connectedDevices.add(thisService)
                Log.i(LOG_TAG, "added to connected devices: $thisService")

                // send him our hello
                Control.hello(Discovery.us!!, thisService)
                Log.i(LOG_TAG, "sent him our hello")
            }
            else {
                throw IOException("He didn't say hello! $thisSocket")
            }
        }
        catch (e: IOException) {
            Log.e(LOG_TAG, "Error accepting connection!", e)
        }
    }

    suspend fun keepListeningForMessages(connectedServices: ArrayList<Service>) {
        Log.i(LOG_TAG, "keepListeningForMessages()")

        while (Discovery.broadcastState == Discovery.BroadcastState.REGISTERED) {
            queryMessage(connectedServices)
            delay(INTERVAL_BETWEEN_READS)
        }
    }

    suspend fun queryMessage(connectedServices: ArrayList<Service>) {
        Log.i(LOG_TAG, "queryMessage()")

        // cycle through all the services and read any messages if available
        connectedServices.forEach {
            if (it.socket!!.getInputStream().available() > 0) {
                // handle this message
                handleMessage(Message.receive(it.socket!!)!!, it)
            }
        }
    }

    suspend fun handleMessage(message: Message, service: Service) {
        Log.i(LOG_TAG, "handleMessage() $message $service")

        when (message.type) {
            Message.Type.CONTROL -> handleControlMessage(message as Control, service)
            Message.Type.STREAM -> handleStreamMessage(message as Stream, service)
            Message.Type.SYNC -> handleSyncMessage(message as Sync, service)
        }
    }

    suspend fun handleControlMessage(message: Control, service: Service) {
        Log.i(LOG_TAG, "handleControlMessage() $message $service")

        when (message.action) {
            Control.Action.PLAY -> if (message.time == -1L) Player.play() else { /* TODO: Later */ }
            Control.Action.PREPARE -> Player.prepare(SongsCache.getSong(message.additionalInfo)!!)
            Control.Action.PAUSE -> Player.pause()
            Control.Action.STOP -> Player.stop()
            Control.Action.HELLO -> {} // Hello messages are handled during connection itself, they wont reach here
            Control.Action.DISCONNECT -> service.disconnect()
            Control.Action.REQ_SHARE_SONG -> TODO()
            Control.Action.MESSAGE_BROADCAST -> TODO()
            Control.Action.QUERY_SONG -> {
                if (SongsCache.getSong(message.additionalInfo) != null) Control.queryResult(service, Control.Companion.QueryResult.PRESENT)
                else Control.queryResult(service, Control.Companion.QueryResult.NOT_PRESENT)
            }
            Control.Action.QUERY_RESULT -> {
                if (Control.Companion.QueryResult.valueOf(message.additionalInfo) == Control.Companion.QueryResult.NOT_PRESENT) {} // TODO: inform someone so that we can send him that song!
                else {} // do nothing
            }
        }
    }

    suspend fun handleStreamMessage(message: Stream, service: Service) {
        Log.i(LOG_TAG, "handleStreamMessage() $message $service")

        SongsCache.hold(SongsCache.Song(
            message.songName,
            message.audioBuffer
        ))
    }

    suspend fun handleSyncMessage(message: Sync, service: Service) {
        Log.i(LOG_TAG, "handleSyncMessage() $message $service")
    }
}