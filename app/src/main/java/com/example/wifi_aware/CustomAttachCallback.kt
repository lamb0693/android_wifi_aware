package com.example.wifi_aware

import android.content.Context
import android.net.wifi.aware.AttachCallback
import android.net.wifi.aware.WifiAwareSession
import android.util.Log

class CustomAttachCallback(private val activity: MainActivity) : AttachCallback() {
    override fun onAttachFailed() {
        super.onAttachFailed()
        Log.i(">>>>", "onAttachFailed")
    }

    override fun onAttached(session: WifiAwareSession?) {
        super.onAttached(session)
        Log.i(">>>>", "onAttached")
        session?.let{
            Log.i(">>>>", "onAttached session : $session")
        }
        activity.removeCurrentWifiAwareSession()
        activity.setWifiAwareSession(session)
    }

    override fun onAwareSessionTerminated() {
        super.onAwareSessionTerminated()
        Log.i(">>>>", "onAwareSessionTerminated")
    }
}