package dev.nerzouille.koan.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Whether the phone has a network, and when its networks last changed (Wi-Fi ↔ mobile, a VPN
 * reconnecting). For the error pages' grace and auto-reload only; no request of its own.
 *
 * Online = a real (non-VPN) network with internet: a VPN stays up and "validated" with nothing
 * under it (airplane mode), so the default network alone can't say. The default network is
 * watched too, for switches (a VPN reconnecting over the same Wi-Fi).
 */
class NetworkWatch(context: Context) {
    /** [changedAt]: `SystemClock.elapsedRealtime()` of the last change, 0 = none seen. */
    data class Net(val online: Boolean, val changedAt: Long)

    private val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    private val _state = MutableStateFlow(Net(online = initialOnline(), changedAt = 0L))
    val state: StateFlow<Net> = _state

    private val lock = Any()
    /** Real networks with internet now. */
    private val real = HashSet<Network>()
    private var default: Network? = null
    /** When [start] ran: networks reported in the first moments were already there. */
    private var startedAt = 0L

    /** Milliseconds since the networks last changed (large when never). */
    fun sinceChangeMs(): Long = _state.value.changedAt.let { if (it == 0L) Long.MAX_VALUE else SystemClock.elapsedRealtime() - it }

    fun start() {
        startedAt = SystemClock.elapsedRealtime()
        runCatching {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                .build()
            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = synchronized(lock) {
                    // The first callbacks only report what is already there.
                    val changed = SystemClock.elapsedRealtime() - startedAt > SETTLE_MS
                    real += network
                    publish(changed)
                }

                override fun onLost(network: Network) = synchronized(lock) {
                    real -= network
                    publish(changed = true)
                }
            })
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = synchronized(lock) {
                    val changed = default != null && default != network
                    default = network
                    if (changed) publish(changed = true)
                }

                override fun onLost(network: Network) = synchronized(lock) {
                    if (network == default) default = null
                    publish(changed = true)
                }
            })
        }
    }

    private fun publish(changed: Boolean) {
        val prev = _state.value
        _state.value = Net(real.isNotEmpty(), if (changed) SystemClock.elapsedRealtime() else prev.changedAt)
    }

    private companion object {
        const val SETTLE_MS = 1000L
    }

    private fun initialOnline(): Boolean = runCatching {
        cm.allNetworks.any { n ->
            cm.getNetworkCapabilities(n)?.let {
                it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && it.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            } == true
        }
    }.getOrDefault(true)
}
