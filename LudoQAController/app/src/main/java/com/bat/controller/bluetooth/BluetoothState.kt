package com.bat.controller.bluetooth

sealed interface BluetoothConnectionState {
    data object Disconnected : BluetoothConnectionState
    data object Connecting : BluetoothConnectionState
    data class Connected(val deviceName: String, val address: String) : BluetoothConnectionState
    data class Error(val message: String) : BluetoothConnectionState
}

data class BluetoothUiState(
    val isBluetoothEnabled: Boolean = false,
    val isScanning: Boolean = false,
    val scannedDevices: List<BluetoothDeviceModel> = emptyList(),
    val pairedDevices: List<BluetoothDeviceModel> = emptyList(),
    val connectionState: BluetoothConnectionState = BluetoothConnectionState.Disconnected,
    val lastReceivedMessage: String? = null
)
