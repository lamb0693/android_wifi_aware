package com.example.wifi_aware

import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

class ServerSocketThread(private val context: Context) : Thread() {
    private var serverSocket: ServerSocket? = null

    private var outputStream: OutputStream? = null
    private var inputStream : InputStream? = null
    private var isRunning = true
    override fun run() {
        Log.i(">>>>", "ServerSocketTrhead Thread Started")

        val coroutineScope = CoroutineScope(Dispatchers.IO)

        try {
            serverSocket = ServerSocket(8888)
            serverSocket?.also { serverSocket1 ->
                val clientSocket : Socket? = serverSocket1.accept()
                Log.i(">>>>" , "server socket ; Accepted  clientSocket = $clientSocket")
                clientSocket?.also {
                    inputStream = it.getInputStream()
                    outputStream = it.getOutputStream()

                    sendMessage("hello from server")
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
                    clientSocket.close()

//                    coroutineScope.launch {
//                        try{
//                            while (isRunning){
//                                val buffer = ByteArray(1024)
//                                val bytesRead = inputStream?.read(buffer)
//                                if (bytesRead != null && bytesRead > 0) {
//                                    val receivedMessage = String(buffer, 0, bytesRead)
//                                    // Handle the received message
//                                    Log.d(">>>>",  "server = ReceivedMessage : $receivedMessage}")
//                                    if(receivedMessage == "quit") isRunning= false
//                                    showToastOnMainThread(receivedMessage)
//                                }
//                            }
//                        } catch (e : Exception){
//                            e.printStackTrace()
//                        } finally {
//                            inputStream?.close()
//                            outputStream?.close()
//                            it.close()
//                        }
//                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            serverSocket?.close()
        }
        Log.i(">>>>", "Server Thread terminating...")
    }

    private suspend fun showToastOnMainThread(message: String) {
        // Switch to the main thread using withContext
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun sendMessage(message: String): Unit {
        outputStream?.write(message.toByteArray())
    }
}