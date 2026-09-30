package com.bat.controller.bluetooth

data class BluetoothDeviceModel(
    val name: String?,
    val address: String,
    val isBonded: Boolean = false
)
