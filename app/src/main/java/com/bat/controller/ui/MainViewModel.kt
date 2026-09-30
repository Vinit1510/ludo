package com.bat.controller.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bat.controller.bluetooth.BluetoothController
import com.bat.controller.bluetooth.BluetoothDeviceModel
import com.bat.controller.network.NetworkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val bluetoothController = BluetoothController(application.applicationContext)
    private val networkManager = NetworkManager()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _selectedMode = MutableStateFlow("BLUETOOTH") // "BLUETOOTH" or "TCP"
    val selectedMode: StateFlow<String> = _selectedMode.asStateFlow()

    private val _targetIp = MutableStateFlow("192.168.1.100")
    val targetIp: StateFlow<String> = _targetIp.asStateFlow()

    private val _targetPort = MutableStateFlow("8080")
    val targetPort: StateFlow<String> = _targetPort.asStateFlow()

    private val _roomCode = MutableStateFlow("")
    val roomCode: StateFlow<String> = _roomCode.asStateFlow()

    private val _lastRollValue = MutableStateFlow<Int?>(null)
    val lastRollValue: StateFlow<Int?> = _lastRollValue.asStateFlow()

    init {
        log("Ludo QA Controller initialized.")
    }

    fun setMode(mode: String) {
        _selectedMode.value = mode
        log("Switched mode to $mode")
    }

    fun setIp(ip: String) { _targetIp.value = ip }
    fun setPort(port: String) { _targetPort.value = port }
    fun setRoomCode(code: String) { _roomCode.value = code }

    fun log(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _logs.update { (listOf("[$timestamp] $message") + it).take(100) }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun sendDiceCommand(diceValue: Int) {
        _lastRollValue.value = diceValue
        val cmd = "{\"action\":\"FORCE_DICE\",\"value\":$diceValue,\"room\":\"${roomCode.value}\"}"
        log("Triggering Dice Override: $diceValue")
        executeCommand(cmd)
    }

    fun sendCustomCommand(actionName: String) {
        val cmd = "{\"action\":\"$actionName\",\"room\":\"${roomCode.value}\"}"
        log("Executing action: $actionName")
        executeCommand(cmd)
    }

    private fun executeCommand(command: String) {
        viewModelScope.launch {
            if (selectedMode.value == "BLUETOOTH") {
                bluetoothController.sendCommand(command) { log(it) }
            } else {
                networkManager.sendCommand(command) { log(it) }
            }
        }
    }

    fun connectTcp() {
        viewModelScope.launch {
            val portNum = targetPort.value.toIntOrNull() ?: 8080
            networkManager.connect(targetIp.value, portNum) { log(it) }
        }
    }

    fun connectBluetoothDevice(device: BluetoothDeviceModel) {
        bluetoothController.connectToDevice(device) { log(it) }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.release()
        networkManager.disconnect()
    }
}
