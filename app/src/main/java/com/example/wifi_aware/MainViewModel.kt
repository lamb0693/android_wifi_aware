package com.example.wifi_aware

import android.net.wifi.aware.PeerHandle
import android.net.wifi.aware.PublishDiscoverySession
import android.net.wifi.aware.SubscribeDiscoverySession
import android.net.wifi.aware.WifiAwareSession
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel(){

    private val _wifiAwareSession = MutableLiveData<WifiAwareSession>()
    val wifiAwareSession : LiveData<WifiAwareSession> get() = _wifiAwareSession

    private val _serviceName = MutableLiveData<String>()
    val serviceName : LiveData<String> get() = _serviceName

    private val _roleAsServer = MutableLiveData<Boolean>()
    val roleAsServer : LiveData<Boolean> get() = _roleAsServer

    private val _peerHandle = MutableLiveData<PeerHandle>()
    val peerHandle : LiveData<PeerHandle> get() = _peerHandle

    private val _publishDiscoverySession = MutableLiveData<PublishDiscoverySession>()
    val publishDiscoverySession : LiveData<PublishDiscoverySession> get()= _publishDiscoverySession

    private val _subscribeDiscoverySession = MutableLiveData<SubscribeDiscoverySession>()
    val subscribeDiscoverySession : LiveData<SubscribeDiscoverySession> get()= _subscribeDiscoverySession

    init {
        _wifiAwareSession.value = null
        _serviceName.value = null
        _roleAsServer.value = null
        _peerHandle.value = null
        _publishDiscoverySession.value = null
        _subscribeDiscoverySession.value = null
    }

    fun setWifiAwareSession(session: WifiAwareSession?){
        _wifiAwareSession.value = session
    }

    fun setServiceName(name : String?){
        _serviceName.value = name
    }

    fun setRoleAsServer(asServer : Boolean){
        _roleAsServer.value = asServer
    }

    fun setPeerHandle(handle: PeerHandle?){
        _peerHandle.value = handle
    }

    fun setPublishDiscoverySession(session: PublishDiscoverySession?){
        _publishDiscoverySession.value = session
    }

    fun setSubscribeDiscoverySession(session: SubscribeDiscoverySession?){
        _subscribeDiscoverySession.value = session
    }

}