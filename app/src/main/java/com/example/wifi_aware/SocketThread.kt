package com.example.wifi_aware

import android.content.Context
import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class SocketThread(val context: Context) : Thread() {

    private var outputStream: OutputStream? = null
    private var inputStream : InputStream? = null
    private var isRunning = true

    private lateinit var socket : Socket

    fun setSocket(socket: Socket){
        this.socket = socket
    }
    override fun run() {
        try {
            outputStream = socket.getOutputStream()
            inputStream = socket.getInputStream()

            Log.i(">>>>", "Server Thread starting...")
            sendMessage("Hello")

            while(isRunning){
                val buffer = ByteArray(1024)
                val bytesRead = inputStream?.read(buffer)
                if (bytesRead != null && bytesRead > 0) {
                    val receivedMessage = String(buffer, 0, bytesRead)
                    Log.i(">>>>", "received from socket: $receivedMessage")
                }
            }

            outputStream?.close()
            inputStream?.close()
            socket.close()
        } catch (e: Exception) {
            // Handle other exceptions
            e.printStackTrace()
        }

        Log.i(">>>>", "Server Thread terminating...")
    }

    fun sendMessage(message: String): Unit {
        val strMessage : String  = "" + message
        outputStream?.write(strMessage.toByteArray())
    }
}


