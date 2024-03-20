package com.example.wifi_aware

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.aware.DiscoverySessionCallback
import android.net.wifi.aware.PeerHandle
import android.net.wifi.aware.PublishConfig
import android.net.wifi.aware.PublishDiscoverySession
import android.net.wifi.aware.SubscribeConfig
import android.net.wifi.aware.SubscribeDiscoverySession
import android.net.wifi.aware.WifiAwareManager
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
import java.lang.Exception


class MainActivity : AppCompatActivity() {
    private lateinit var bindMain : ActivityMainBinding

    lateinit var wifiAwareManager : WifiAwareManager
    private var wifiAwareReceiver: WifiAwareBroadcastReceiver? = null
    private lateinit var intentFilter : IntentFilter
    private lateinit var customAttachCallback: CustomAttachCallback

    lateinit var viewModel : MainViewModel

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

        checkPermission()
        initListener()

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
                .build()
            viewModel.wifiAwareSession.value?.publish(config, object : DiscoverySessionCallback() {
                override fun onPublishStarted(session: PublishDiscoverySession) {
                    Log.i(">>>>", "onPublishStarted... $session")
                    viewModel.setPublishDiscoverySession(session)
                }
                override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
                    val receivedMessage = String(message, Charsets.UTF_8)
                    Log.i(">>>>", "onMessageReceived...$peerHandle, $receivedMessage")
                    viewModel.setPeerHandle(peerHandle)
                    Toast.makeText(this@MainActivity, receivedMessage, Toast.LENGTH_SHORT).show()
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
                .setTtlSec(20)
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
                override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
                    val receivedMessage = String(message, Charsets.UTF_8)
                    Log.i(">>>>", "onMessageReceived...$peerHandle, $receivedMessage")
                    Toast.makeText(this@MainActivity, receivedMessage, Toast.LENGTH_SHORT).show()
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
