package com.example.androidxmlbase.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

interface ConnectivityChecker {
    fun isConnected(): Boolean
}

class AndroidConnectivityChecker(private val context: Context) : ConnectivityChecker {
    override fun isConnected(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
