package com.example.wifi_aware

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.aware.DiscoverySessionCallback
import android.net.wifi.aware.PeerHandle
import android.net.wifi.aware.PublishConfig
import android.net.wifi.aware.PublishDiscoverySession
import android.net.wifi.aware.SubscribeConfig
import android.net.wifi.aware.SubscribeDiscoverySession
import android.net.wifi.aware.WifiAwareManager
import android.net.wifi.aware.WifiAwareNetworkInfo
import android.net.wifi.aware.WifiAwareNetworkSpecifier
import android.net.wifi.aware.WifiAwareSession
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.wifi_aware.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.Exception


class MainActivity : AppCompatActivity() {
    private lateinit var bindMain : ActivityMainBinding

    lateinit var wifiAwareManager : WifiAwareManager
    private var wifiAwareReceiver: WifiAwareBroadcastReceiver? = null
    private lateinit var intentFilter : IntentFilter
    private lateinit var customAttachCallback: CustomAttachCallback

    lateinit var viewModel : MainViewModel

    lateinit var connectivityManager : ConnectivityManager

    var networkCallback : ConnectivityManager.NetworkCallback? = null

    //private lateinit var socketThread : SocketThread
    //private var socket : Socket? = null

    private var serverSocketThread: ServerSocketThread? = null
    private var clientSocketThread: ClientSocketThread? = null

    private fun initViewModel(){
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        viewModel.wifiAwareSession.observe(this){
            var strDisplay : String = "wifiAwareSession : \n"
            strDisplay += it?.toString() ?: "null"
            bindMain.tvWifiAwareSession.text = strDisplay
        }

        viewModel.roleAsServer.observe(this){
            var strDisplay : String = "AsServer? "
            strDisplay += it?.toString() ?: "null"
            bindMain.tvAsServer.text = strDisplay
        }

        viewModel.serviceName.observe(this){
            var strDisplay : String = "service name? "
            strDisplay += it?.toString() ?: "null"
            bindMain.tvServiceName.text = strDisplay
        }

        viewModel.peerHandle.observe(this){
            val strDisplay : String = it?.toString() ?: " : null"
            bindMain.tvPeerHandle.text = strDisplay
        }

        viewModel.publishDiscoverySession.observe(this){
            val strDisplay: String = if (it != null) {
                "PublishDiscoverySession: $it"
            } else {
                "null"
            }
            bindMain.tvDiscoverySession.text = strDisplay
        }

        viewModel.subscribeDiscoverySession.observe(this){
            val strDisplay: String = if (it != null) {
                "SubscriptDiscoverySession: $it"
            } else {
                "null"
            }
            bindMain.tvDiscoverySession.text = strDisplay
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun initListener(){
        bindMain.btnAttach.setOnClickListener {
            attach()
        }
        bindMain.btnDisconnect.setOnClickListener {
            removeCurrentWifiAwareSession()
        }

        bindMain.rbAsServer.setOnClickListener{viewModel.setRoleAsServer(true) }
        bindMain.rbAsClient.setOnClickListener{viewModel.setRoleAsServer(false)}
        bindMain.editServiceName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.toString()?.let {viewModel.setServiceName(it) }
            }
            override fun afterTextChanged(s: Editable?) {
            }
        })


        bindMain.btnSend.setOnClickListener {
            viewModel.roleAsServer.value?.let { asServer ->
                val strMessage : String = bindMain.etMessage.text.toString()
                if(asServer){
                    viewModel.peerHandle.value?.let {peerHandle->
                        viewModel.publishDiscoverySession.value?.sendMessage(
                            peerHandle,101, strMessage.toByteArray(Charsets.UTF_8)
                        )
                    }
                } else {
                    viewModel.peerHandle.value?.let {peerHandle->
                        viewModel.subscribeDiscoverySession.value?.sendMessage(
                            peerHandle, 101, strMessage.toByteArray(Charsets.UTF_8)
                        )
                    }
                }
            }
        }

        bindMain.btnSendUsingSocket.setOnClickListener{
            sendMessage(bindMain.etMessage.text.toString())
        }
    }

    private fun sendMessage(message : String){
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            viewModel.roleAsServer.value?.let{
                if(it) serverSocketThread?.sendMessage(message)
                else clientSocketThread?.sendMessage(message)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        bindMain = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindMain.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViewModel()

        wifiAwareManager = getSystemService(Context.WIFI_AWARE_SERVICE) as WifiAwareManager
        Log.i(">>>>", "wifiAwareManager : $wifiAwareManager")
        intentFilter = IntentFilter(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED)
        wifiAwareReceiver = WifiAwareBroadcastReceiver(this, wifiAwareManager, viewModel.wifiAwareSession.value )

        customAttachCallback = CustomAttachCallback(this)

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        checkPermission()
        initListener()

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun initServerSocket(){
        if(viewModel.roleAsServer.value != null
            && !viewModel.roleAsServer.value!!) return

        Log.i(">>>>", "init serversocket")
        val ss = ServerSocket(0)
        val port = ss.localPort

        if(viewModel.publishDiscoverySession.value == null
            || viewModel.peerHandle.value == null) return

        val networkSpecifier = WifiAwareNetworkSpecifier.Builder(
            viewModel.publishDiscoverySession.value!!,
            viewModel.peerHandle.value!!)
            .setPskPassphrase("12340987")
            .setPort(port)
            .build()
        Log.i(">>>>", "init serversocket $networkSpecifier")

        val myNetworkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
            .setNetworkSpecifier(networkSpecifier)
            .build()
        Log.i(">>>>", "init serversocket $myNetworkRequest")

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.i(">>>>", "NetworkCallback onAvailable")
                Toast.makeText(this@MainActivity, "Socket network availabe", Toast.LENGTH_LONG).show()

                try{
                    if(serverSocketThread == null) {
                        serverSocketThread = ServerSocketThread(this@MainActivity)
                        serverSocketThread?.start()
                    }
//                    if(socket == null) {
//                        socket = ss.accept()
//                        Log.i(">>>>", "server socket Accepted  created Socket = $socket")
//
//                        socket?.also{
//                            socketThread = SocketThread(this@MainActivity)
//                            socketThread.setSocket(it)
//                            socketThread.start()
//                            socketThread.sendMessage("it's from server")
//                        }
//                    }
                } catch ( e : Exception){
                    Log.e(">>>>", "starting socket thred except : ${e.message}")
                }
            }
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                Log.i(">>>>", "NetworkCallback onCapabilitiesChanged network : $network")
                Log.i(">>>>", "NetworkCapabilities : $networkCapabilities")

//                val peerAwareInfo = networkCapabilities.transportInfo as WifiAwareNetworkInfo
//                val peerIpv6 = peerAwareInfo.peerIpv6Addr
//                val peerPort = peerAwareInfo.port
//                //...
//                try{
//                    if(socket == null) {
//                        socket = network.getSocketFactory().createSocket("localhost", ss.localPort)
//                        Log.i(">>>>", "socket connected to Server, $socket")
//                        socket?.also{
//                            socketThread = SocketThread(this@MainActivity)
//                            socketThread.setSocket(it)
//                            socketThread.start()
//                            //socketThread.sendMessage("it's from client")
//                        }
//                    }
//                } catch ( e : Exception){
//                    Log.e(">>>>", "connection To serversocket : ${e.message}")
//                }
            }
            override fun onLost(network: Network) {
                super.onLost(network)
                Log.i(">>>>", "NetworkCallback onLost")
            }
        }
        connectivityManager.requestNetwork(myNetworkRequest, networkCallback)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun connectToServerSocket(){
        if(viewModel.roleAsServer.value != null
            && viewModel.roleAsServer.value!!) return
        Log.i(">>>>", "starting connecting to server socket")

        if(viewModel.subscribeDiscoverySession.value == null
            || viewModel.peerHandle.value == null) return


        val networkSpecifier = WifiAwareNetworkSpecifier.Builder(
            viewModel.subscribeDiscoverySession.value!!,
            viewModel.peerHandle.value!!
        )
        .setPskPassphrase("12340987")
        .build()

        Log.i(">>>>", "connecting to server socket $networkSpecifier")

        val myNetworkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
            .setNetworkSpecifier(networkSpecifier)
            .build()
        Log.i(">>>>", "connecting to server socket $myNetworkRequest")
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.i(">>>>", "NetworkCallback onAvailable")
                Toast.makeText(this@MainActivity, "Socket network availabe", Toast.LENGTH_LONG)
                    .show()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                Log.i(">>>>", "NetworkCallback onCapabilitiesChanged network : $network")
                Log.i(">>>>", "NetworkCapabilities : $networkCapabilities")

                val peerAwareInfo = networkCapabilities.transportInfo as WifiAwareNetworkInfo
                val peerIpv6 = peerAwareInfo.peerIpv6Addr
                //val peerPort = peerAwareInfo.port

                if(clientSocketThread == null){
                    clientSocketThread = ClientSocketThread(this@MainActivity, InetSocketAddress(peerIpv6, 8888))
                    clientSocketThread?.start()
                }


//                networkCallback?.let{
//                    connectivityManager.unregisterNetworkCallback(it)
//                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.i(">>>>", "NetworkCallback onLost")
            }
        }
        connectivityManager.requestNetwork(myNetworkRequest,
            networkCallback as ConnectivityManager.NetworkCallback
        )
    }

    fun removeCurrentWifiAwareSession(){
        try{
            viewModel.publishDiscoverySession.value?.close()
            viewModel.setPublishDiscoverySession(null)
            viewModel.subscribeDiscoverySession.value?.close()
            viewModel.setSubscribeDiscoverySession(null)
            viewModel.wifiAwareSession.value?.close()
            viewModel.setWifiAwareSession(null)
            viewModel.setPeerHandle(null)
        } catch (e: Exception) {
            Log.e(">>>>", "removeWifiAwareSession : ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun setWifiAwareSession(wifiAwareSession: WifiAwareSession?){
        Log.i(">>>>", "setting wifiAwareSession")
        if(wifiAwareSession == null) Log.i(">>>>", "wifiAwareSession null")
        removeCurrentWifiAwareSession()
        viewModel.setWifiAwareSession(wifiAwareSession)
        //SimpleConfirmDlg(this@MainActivity, "noti", "wifreAwareSession is set $this.wifiAwareSession").showDialog()
        if(viewModel.roleAsServer.value == null || viewModel.serviceName.value == null) {
            // add message
            return
        }

        if(viewModel.roleAsServer.value!!) {
            val config: PublishConfig = PublishConfig.Builder()
                .setServiceName(viewModel.serviceName.value!!)
                .setTtlSec(0)
                .build()
            viewModel.wifiAwareSession.value?.publish(config, object : DiscoverySessionCallback() {
                override fun onPublishStarted(session: PublishDiscoverySession) {
                    Log.i(">>>>", "onPublishStarted... $session")
                    viewModel.setPublishDiscoverySession(session)
                }
                @RequiresApi(Build.VERSION_CODES.Q)
                override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
                    val receivedMessage = String(message, Charsets.UTF_8)
                    Log.i(">>>>", "onMessageReceived...$peerHandle, $receivedMessage")
                    viewModel.setPeerHandle(peerHandle)
                    Toast.makeText(this@MainActivity, receivedMessage, Toast.LENGTH_SHORT).show()
                    initServerSocket()
                    viewModel.publishDiscoverySession.value?.sendMessage(peerHandle, 101, "from server".toByteArray())
                }
                override fun onSessionTerminated() {
                    Log.i(">>>>", "onSessionTerminated")
                    removeCurrentWifiAwareSession()
                    Toast.makeText(this@MainActivity, "fail to connect to server", Toast.LENGTH_SHORT).show()
                    super.onSessionTerminated()
                }
                override fun onServiceLost(peerHandle: PeerHandle, reason: Int) {
                    super.onServiceLost(peerHandle, reason)
                    Log.i(">>>>", "onServiceLost $peerHandle, $reason")
                }
            }, null)
        } else {
            val config: SubscribeConfig = SubscribeConfig.Builder()
                .setServiceName(viewModel.serviceName.value!!)
                .setTtlSec(0)
                .build()
            viewModel.wifiAwareSession.value?.subscribe(config, object : DiscoverySessionCallback() {
                override fun onSubscribeStarted(session: SubscribeDiscoverySession) {
                    Log.i(">>>>", "onSubscribeStarted... $session")
                    viewModel.setSubscribeDiscoverySession(session)
                }
                override fun onServiceDiscovered(
                    peerHandle: PeerHandle,
                    serviceSpecificInfo: ByteArray,
                    matchFilter: List<ByteArray>
                ) {
                    Log.i(">>>>", "onServiceDiscovered... $peerHandle, $serviceSpecificInfo")
                    val messageToSend = "hello"
                    viewModel.setPeerHandle(peerHandle)
                    viewModel.subscribeDiscoverySession.value?.sendMessage(peerHandle,101, messageToSend.toByteArray(Charsets.UTF_8))
                    Toast.makeText(this@MainActivity, "Connected to server", Toast.LENGTH_SHORT).show()
                }
                @RequiresApi(Build.VERSION_CODES.Q)
                override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
                    val receivedMessage = String(message, Charsets.UTF_8)
                    Log.i(">>>>", "onMessageReceived...$peerHandle, $receivedMessage")
                    Toast.makeText(this@MainActivity, receivedMessage, Toast.LENGTH_SHORT).show()
                    connectToServerSocket()
                }
                override fun onSessionTerminated() {
                    removeCurrentWifiAwareSession()
                    Toast.makeText(this@MainActivity, "fail to connect to server", Toast.LENGTH_SHORT).show()
                    super.onSessionTerminated()
                }
                override fun onServiceLost(peerHandle: PeerHandle, reason: Int) {
                    super.onServiceLost(peerHandle, reason)
                    Log.i(">>>>", "onServiceLost $peerHandle, $reason")
                }
            }, null)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun attach(){
        wifiAwareManager.attach(customAttachCallback, null)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onResume() {
        registerReceiver(wifiAwareReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
        super.onResume()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
            permissions -> val granted = permissions.entries.all {
        it.value
    }
        if(granted) Log.i("permission Launcher>>>>", "all permission granted in permission Launcher")
        else {
            Log.e("permission Launcher >>>>", "not all of permission granted in permission Launcher ")
        }
    }

    private fun checkPermission(){
        val statusCoarseLocation = ContextCompat.checkSelfPermission(this,
            "android.permission.ACCESS_COARSE_LOCATION")
        val statusFineLocation = ContextCompat.checkSelfPermission(this,
            "android.permission.ACCESS_FINE_LOCATION")

        val shouldRequestPermission = statusCoarseLocation != PackageManager.PERMISSION_GRANTED
                || statusFineLocation != PackageManager.PERMISSION_GRANTED

        if (shouldRequestPermission) {
            Log.d(">>>>", "One or more Permission Denied, Starting permission Launcher")
            permissionLauncher.launch(
                arrayOf(
                    "android.permission.ACCESS_COARSE_LOCATION",
                    "android.permission.ACCESS_FINE_LOCATION",
                )
            )
        } else {
            Log.d(">>>>", "All Permission Permitted, No need to start permission Launcher")
        }
    }
}
