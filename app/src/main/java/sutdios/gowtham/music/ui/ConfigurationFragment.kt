package sutdios.gowtham.music.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar

import sutdios.gowtham.music.R
import sutdios.gowtham.music.network.Discovery
import sutdios.gowtham.music.network.Service
import java.util.ArrayList
import java.util.HashSet

class ConfigurationFragment : Fragment(), Discovery.Callbacks {

    val LOG_TAG = "ConfigurationFragment"

    var mListener: Callbacks? = null

    var mRoot: View? = null
    var mRecycler: RecyclerView? = null
    var mServiceName: TextView? = null
    var mServiceState: TextView? = null
    var mServiceStateButton: Button? = null

    private var mAdapter: ServiceListAdapter? = null
    private var mLayoutManger: LinearLayoutManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(LOG_TAG, "onCreateView()")
        val view = inflater.inflate(R.layout.fragment_configuration, container, false)
        mRoot = view
        mRecycler = view.findViewById(R.id.config_frag_services_recycler)
        mServiceName = view.findViewById(R.id.config_frag_service_name)
        mServiceState = view.findViewById(R.id.config_frag_service_state)
        mServiceStateButton = view.findViewById(R.id.config_frag_service_state_button)

        mServiceStateButton?.setOnClickListener {
            if (Discovery.broadcastState in arrayOf(Discovery.BroadcastState.UNREGISTERED, Discovery.BroadcastState.UNREGISTERING)) {
                Discovery.startDiscovery(activity!!.applicationContext)
                mServiceStateButton?.text = "stop"
            }
            else {
                Discovery.stopDiscovery(activity!!.applicationContext)
                mServiceStateButton?.text = "start"
            }
        }

        mAdapter = ServiceListAdapter(Discovery.availableServices)
        mRecycler?.adapter = mAdapter
        mLayoutManger = LinearLayoutManager(context)
        mRecycler?.layoutManager = mLayoutManger
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
        fun onItemClick(position: Int)
    }

    private inner class GenericViewHolder internal constructor(
        inflater: LayoutInflater,
        parent: ViewGroup
    ) : RecyclerView.ViewHolder(
        inflater.inflate(
            R.layout.service_item_generic,
            parent,
            false
        )
    ) {
        internal val service: TextView = itemView.findViewById(R.id.service_item_generic_service_name)
        internal val serviceType: TextView = itemView.findViewById(R.id.service_item_generic_service_type)
        internal val connectionState: TextView = itemView.findViewById(R.id.service_item_generic_connection_state)

        init {
            itemView.setOnClickListener {
                mListener?.onItemClick(adapterPosition)
            }
        }
    }

    private inner class ServiceListAdapter internal constructor(
        private val services: ArrayList<Service>
    ) :
        RecyclerView.Adapter<GenericViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericViewHolder {
            return GenericViewHolder(LayoutInflater.from(parent.context), parent)
        }

        override fun onBindViewHolder(holder: GenericViewHolder, position: Int) {
            holder.service.text = services[position].name
            holder.serviceType.text = services[position].deviceType.toString()
            holder.connectionState.text = services[position].state.toString()
        }

        override fun getItemCount(): Int {
            return services.size
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ConfigurationFragment().apply {}
    }

    override fun discoveryStarted() {
    }

    override fun discoveryStopped() {
    }

    override fun discoveryStartFailed() {
        mRoot?.let {
            Snackbar.make(it, "Error starting discovery", Snackbar.LENGTH_INDEFINITE)
                .setAction("retry") {
                    // TODO: Restart discovery
                }
                .show()
        }
    }

    override fun discoveryStopFailed() {
        mRoot?.let {
            Snackbar.make(it, "Error stopping discovery", Snackbar.LENGTH_INDEFINITE)
                .show()
        }
    }

    override fun availableServicesChanged(availableServices: ArrayList<Service>) {
        mAdapter?.notifyDataSetChanged()
    }

    override fun serviceRegistered() {
        mServiceState?.text = "stop"
    }

    override fun serviceUnregistered() {
        mServiceState?.text = "start"
    }

    override fun serviceRegistrationFailed() {
        mRoot?.let {
            Snackbar.make(it, "Error registering service", Snackbar.LENGTH_INDEFINITE)
                .setAction("retry") {
                    // TODO: Retry service registration
                }
                .show()
        }
    }

    override fun serviceUnRegistrationFailed() {
        mRoot?.let {
            Snackbar.make(it, "Error un-registering service", Snackbar.LENGTH_INDEFINITE)
                .show()
        }
    }

    override fun serviceConnected(service: Service) {
        mAdapter?.notifyDataSetChanged()
    }

    override fun serviceDisconnected(service: Service) {
        mAdapter?.notifyDataSetChanged()
    }
}
