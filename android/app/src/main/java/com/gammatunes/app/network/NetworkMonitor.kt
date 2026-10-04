package com.gammatunes.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tracks whether the device currently has a working internet connection. */
object NetworkMonitor {
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private fun NetworkCapabilities?.hasInternet(): Boolean =
        this != null &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    fun init(context: Context) {
        val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java) ?: return
        _isOnline.value = cm.getNetworkCapabilities(cm.activeNetwork).hasInternet()
        runCatching {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    _isOnline.value = caps.hasInternet()
                }

                override fun onLost(network: Network) {
                    _isOnline.value = false
                }
            })
        }
    }
}
