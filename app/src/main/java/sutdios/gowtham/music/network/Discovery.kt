package sutdios.gowtham.music.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import java.lang.ref.WeakReference
import java.net.ServerSocket
import java.util.ArrayList
import java.util.HashSet

object Discovery {

    const val LOG_TAG = "Discovery"

    var mContext: WeakReference<Context>? = null
    var mListener: Callbacks? = null // listener will be UI

    val serverSocket = initializeServerSocket()
    var broadcastState: BroadcastState = BroadcastState.UNREGISTERED
    var us: Service? = null
    var broadcastService: NsdServiceInfo? = null
    var discoveryState = DiscoveryState.STOPPED
    val availableServices = ArrayList<Service>()

    val connectedDevices = ArrayList<Service>()

    // reinits is UI restarts ?
    fun initialize(context: Context, callbacks: Callbacks) {
        mContext = WeakReference(context)
        mListener = callbacks
    }

    fun release() {
        stopDiscovery(mContext!!.get()!!)
        unregisterService(mContext!!.get()!!)
        connectedDevices.forEach { it.disconnect() }
    }

    fun startDiscovery(context: Context) {
        discoveryState = DiscoveryState.STARTING
        (context.getSystemService(Context.NSD_SERVICE) as NsdManager).apply {
            discoverServices(Service.getServiceType(Type.SOURCE), NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        }
    }

    fun stopDiscovery(context: Context) {
        discoveryState = DiscoveryState.STOPPING
        (context.getSystemService(Context.NSD_SERVICE) as NsdManager).apply {
            stopServiceDiscovery(discoveryListener)
        }
    }

    fun createService(type: Type, serverSocket: ServerSocket): NsdServiceInfo {
        val nsdServiceInfo = NsdServiceInfo().apply {
            serviceName = "Music"
            serviceType = Service.getServiceType(type)
            port = serverSocket.localPort
        }
        return nsdServiceInfo
    }

    fun registerService(serviceInfo: NsdServiceInfo, context: Context): Boolean {
        if (broadcastState != BroadcastState.UNREGISTERED) {
            Log.e(LOG_TAG, "Service already registered! $broadcastService")
            return false
        }
        broadcastState = BroadcastState.REGISTERING
        broadcastService = serviceInfo
        val nsdManager = (context.getSystemService(Context.NSD_SERVICE) as NsdManager).apply {
            registerService(broadcastService, NsdManager.PROTOCOL_DNS_SD, nsdRegistrationListener)
        }
        return true
    }

    fun unregisterService(context: Context): Boolean {
        if (broadcastState in arrayOf(BroadcastState.REGISTERING, BroadcastState.REGISTERED)) {
            broadcastState = BroadcastState.UNREGISTERING
            val nsdManager = (context.getSystemService(Context.NSD_SERVICE) as NsdManager).apply {
                unregisterService(nsdRegistrationListener)
            }
            return true
        }
        else {
            Log.e(LOG_TAG, "No service registered!")
            return false
        }
    }

    interface Callbacks {

        fun discoveryStarted()
        fun discoveryStopped()
        fun discoveryStartFailed()
        fun discoveryStopFailed()

        fun availableServicesChanged(availableServices: ArrayList<Service>)

        fun serviceRegistered()
        fun serviceUnregistered()
        fun serviceRegistrationFailed()
        fun serviceUnRegistrationFailed()

        fun serviceConnected(service: Service)
        fun serviceDisconnected(service: Service)
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {

        override fun onDiscoveryStarted(regType: String) {
            Log.i(LOG_TAG, "Service discovery started")
            discoveryState = DiscoveryState.STARTED
            mListener?.discoveryStarted()
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            Log.d(LOG_TAG, "Service discovery success$service")
            if (Service.serviceTypes.contains(service.serviceType)) {
                Log.i(LOG_TAG, "Found service $service")
                if (service.serviceName == broadcastService?.serviceName) {
                    Log.i(LOG_TAG, "Our service!")
                }
                else {
                    Log.i(LOG_TAG, "Adding found service")
                    availableServices.add(Service.parse(service))
                    mListener?.availableServicesChanged(availableServices)
                }
            }
            else {
                Log.i(LOG_TAG, "Unknown service $service")
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            Log.e(LOG_TAG, "service lost: $service")
            availableServices.remove(Service.parse(service))
            mListener?.availableServicesChanged(availableServices)
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i(LOG_TAG, "Discovery stopped: $serviceType")
            discoveryState = DiscoveryState.STOPPED
            mListener?.discoveryStopped()
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(LOG_TAG, "Discovery failed: Error code:$errorCode")
            discoveryState = DiscoveryState.STOPPED
            mListener?.discoveryStartFailed()
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(LOG_TAG, "Discovery failed: Error code:$errorCode")
            discoveryState = DiscoveryState.STOPPING
            mListener?.discoveryStopFailed()
            // TODO: Attempt to stop again!
        }
    }

    internal fun initializeServerSocket(): ServerSocket {
        return ServerSocket(0)
    }

    val nsdRegistrationListener = object: NsdManager.RegistrationListener {

        override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
            Log.i(LOG_TAG, "$serviceInfo registered")
            broadcastState = BroadcastState.REGISTERED
            us = Service.parse(serviceInfo!!)
            mListener?.serviceRegistered()
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            Log.e(LOG_TAG, "$serviceInfo registration failed! $errorCode")
            broadcastState = BroadcastState.UNREGISTERED
            mListener?.serviceRegistrationFailed()
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
            Log.i(LOG_TAG, "$serviceInfo unregistered")
            broadcastState = BroadcastState.UNREGISTERED
            mListener?.serviceUnregistered()
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            Log.e(LOG_TAG, "$serviceInfo unregistration failed! $errorCode")
            broadcastState = BroadcastState.REGISTERED
            mListener?.serviceUnRegistrationFailed()
        }
    }

    enum class BroadcastState {
        REGISTERING, REGISTERED, UNREGISTERING, UNREGISTERED
    }

    enum class DiscoveryState {
        STARTING, STARTED, STOPPING, STOPPED
    }
}