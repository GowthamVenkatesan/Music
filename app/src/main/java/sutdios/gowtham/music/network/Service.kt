package sutdios.gowtham.music.network

import android.net.nsd.NsdServiceInfo
import android.util.Log
import sutdios.gowtham.music.util.Open
import java.io.IOException
import java.net.Socket

@Open
class Service(
    val name: String,
    val deviceType: Type,
    val serviceInfo: NsdServiceInfo,
    var socket: Socket?,
    var state: State
) {

    val LOG_TAG = "Service"

    enum class State {
        CONNECTED, DISCONNECTED
    }

    companion object {
        const val PROTOCOL = "_tcp"
        val serviceTypes = Type.values().map { getServiceType(it) }.toSet()

        fun getServiceType(type: Type): String {
            return "${type}_music.$PROTOCOL"
        }

        fun parse(serviceInfo: NsdServiceInfo): Service {
            val type = serviceInfo.serviceType.split(".")[0].split("_music")[0]
            return Service(
                serviceInfo.serviceName,
                Type.valueOf(type),
                serviceInfo,
                null,
                State.DISCONNECTED
            )
        }
    }

    /**
     * We connect and send an hello
     * Then we get the other guy's hello
     */
    fun connect(): Boolean {
        try {
            socket = Socket(serviceInfo.host, serviceInfo.port)

            if (!Control.hello(Discovery.us!!, this)) {
                throw IOException("Error saying hi to $this")
            }
            val hiHello = Message.receive(this) as Control  // we don't care, we already know him

            state = State.CONNECTED
            Discovery.mListener?.serviceConnected(this)
            return true
        }
        catch (e: IOException) {
            Log.e(LOG_TAG, "Error opening socket to peer!", e)
        }
        return false
    }

    fun disconnect(): Boolean {
        return try {
            if (state == State.CONNECTED) {
                if (!Control.disconnect(this)) {
                    throw IOException("Error sending disconnect message! $this")
                }
            }
            socket!!.close()
            Discovery.mListener?.serviceDisconnected(this)
            true
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Error disconnecting! $this", e)
            false
        }
    }

    fun sync() {

    }

    override fun toString(): String {
        return "Service<$name, $deviceType, $state, $socket>"
    }
}

enum class Type {
    SOURCE, SINK, BOTH
}