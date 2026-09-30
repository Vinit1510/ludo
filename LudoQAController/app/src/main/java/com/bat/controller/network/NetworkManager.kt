package com.bat.controller.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

class NetworkManager {

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null

    suspend fun connect(ip: String, port: Int, onLog: (String) -> Unit): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                disconnect()
                socket = Socket()
                socket?.connect(InetSocketAddress(ip, port), 5000)
                writer = PrintWriter(socket!!.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                onLog("Connected to TCP target $ip:$port")
                true
            } catch (e: Exception) {
                onLog("TCP Connection error: ${e.message}")
                false
            }
        }
    }

    suspend fun sendCommand(command: String, onLog: (String) -> Unit): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                writer?.println(command)
                onLog("-> TCP Sent: $command")
                true
            } catch (e: Exception) {
                onLog("TCP Send error: ${e.message}")
                false
            }
        }
    }

    fun disconnect() {
        try {
            socket?.close()
            socket = null
            writer = null
            reader = null
        } catch (e: Exception) {
            // ignore
        }
    }
}
