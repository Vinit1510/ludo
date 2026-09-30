package com.bat.controller.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothController(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _uiState = MutableStateFlow(BluetoothUiState())
    val uiState: StateFlow<BluetoothUiState> = _uiState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var currentSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    // Standard SPP UUID for Serial Bluetooth communication
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    device?.let { d ->
                        val devModel = BluetoothDeviceModel(d.name, d.address, d.bondState == BluetoothDevice.BOND_BONDED)
                        _uiState.update { state ->
                            if (state.scannedDevices.none { it.address == devModel.address }) {
                                state.copy(scannedDevices = state.scannedDevices + devModel)
                            } else state
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _uiState.update { it.copy(isScanning = false) }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    _uiState.update { it.copy(isBluetoothEnabled = state == BluetoothAdapter.STATE_ON) }
                }
            }
        }
    }

    init {
        updateBluetoothEnabled()
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
    }

    fun updateBluetoothEnabled() {
        _uiState.update { it.copy(isBluetoothEnabled = bluetoothAdapter?.isEnabled == true) }
        loadPairedDevices()
    }

    @SuppressLint("MissingPermission")
    fun loadPairedDevices() {
        val paired = bluetoothAdapter?.bondedDevices?.map {
            BluetoothDeviceModel(it.name, it.address, true)
        } ?: emptyList()
        _uiState.update { it.copy(pairedDevices = paired) }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter.cancelDiscovery()
        }
        _uiState.update { it.copy(isScanning = true, scannedDevices = emptyList()) }
        bluetoothAdapter?.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        bluetoothAdapter?.cancelDiscovery()
        _uiState.update { it.copy(isScanning = false) }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceModel: BluetoothDeviceModel, onLog: (String) -> Unit) {
        stopDiscovery()
        _uiState.update { it.copy(connectionState = BluetoothConnectionState.Connecting) }
        scope.launch {
            try {
                val device = bluetoothAdapter?.getRemoteDevice(deviceModel.address)
                currentSocket?.close()
                currentSocket = device?.createRfcommSocketToServiceRecord(sppUuid)
                currentSocket?.connect()

                outputStream = currentSocket?.outputStream
                inputStream = currentSocket?.inputStream

                _uiState.update {
                    it.copy(
                        connectionState = BluetoothConnectionState.Connected(
                            deviceModel.name ?: "Unknown Device",
                            deviceModel.address
                        )
                    )
                }
                onLog("Connected to Bluetooth device: ${deviceModel.name} (${deviceModel.address})")
                listenForIncomingData(onLog)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(connectionState = BluetoothConnectionState.Error(e.message ?: "Connection failed"))
                }
                onLog("Bluetooth connection error: ${e.message}")
            }
        }
    }

    private fun listenForIncomingData(onLog: (String) -> Unit) {
        val buffer = ByteArray(1024)
        while (currentSocket?.isConnected == true) {
            try {
                val bytes = inputStream?.read(buffer) ?: -1
                if (bytes > 0) {
                    val message = String(buffer, 0, bytes).trim()
                    _uiState.update { it.copy(lastReceivedMessage = message) }
                    onLog("<- Received: $message")
                }
            } catch (e: IOException) {
                _uiState.update { it.copy(connectionState = BluetoothConnectionState.Disconnected) }
                onLog("Connection closed: ${e.message}")
                break
            }
        }
    }

    fun sendCommand(command: String, onLog: (String) -> Unit): Boolean {
        return try {
            outputStream?.write((command + "\n").toByteArray())
            outputStream?.flush()
            onLog("-> Sent: $command")
            true
        } catch (e: Exception) {
            onLog("Failed to send command: ${e.message}")
            false
        }
    }

    fun disconnect() {
        try {
            currentSocket?.close()
            currentSocket = null
            outputStream = null
            inputStream = null
            _uiState.update { it.copy(connectionState = BluetoothConnectionState.Disconnected) }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun release() {
        try {
            context.unregisterReceiver(receiver)
            disconnect()
        } catch (e: Exception) {
            // ignore
        }
    }
}
