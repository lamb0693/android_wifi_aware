package com.example.wifi_aware

import android.content.Context
import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

class ClientSocketThread(val context: Context, private val host : InetSocketAddress) : Thread() {
    private lateinit var socket : Socket
    private var outputStream: OutputStream? = null
    private var inputStream : InputStream? = null
    private var isRunning = true
    override fun run() {
        Log.i(">>>>", "Client Thread Started")

        try {
            socket = Socket()
            socket.connect(host, 10000)
            Log.i(">>>>" , "client socket ; connected to server = $socket")

            outputStream = socket.getOutputStream()
            inputStream = socket.getInputStream()
            // Send message to the server (group owner)
            sendMessage("hello from client")
            //sendMessage("quit")

            while(isRunning){
                val buffer = ByteArray(1024)
                val bytesRead = inputStream?.read(buffer)
                if (bytesRead != null && bytesRead > 0) {
                    val receivedMessage = String(buffer, 0, bytesRead)
                    // Handle the received message
                    Log.d(">>>>",  "ReceivedMessage : $receivedMessage")
                    if(receivedMessage == "quit") isRunning = false
                }
            }

            outputStream?.close()
            inputStream?.close()
            socket.close()
        } catch (e: SocketTimeoutException) {
            // Handle timeout exception
            e.printStackTrace()
        } catch (e: Exception) {
            // Handle other exceptions
            e.printStackTrace()
        }

        Log.i(">>>>", "Client Thread terminating...")
    }

    fun sendMessage(message: String): Unit {
        val strMessage : String  = "" + message
        outputStream?.write(strMessage.toByteArray())
    }

}