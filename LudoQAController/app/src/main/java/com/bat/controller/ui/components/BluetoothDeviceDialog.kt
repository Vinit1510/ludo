package com.bat.controller.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bat.controller.bluetooth.BluetoothDeviceModel
import com.bat.controller.bluetooth.BluetoothUiState

@Composable
fun BluetoothDeviceDialog(
    state: BluetoothUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onSelectDevice: (BluetoothDeviceModel) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Bluetooth Devices")
                if (state.isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onStartScan,
                        enabled = !state.isScanning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Scan")
                    }
                    OutlinedButton(
                        onClick = onStopScan,
                        enabled = state.isScanning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Stop")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (state.pairedDevices.isNotEmpty()) {
                        item {
                            Text(
                                "Paired Devices",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(state.pairedDevices) { dev ->
                            DeviceRow(dev) { onSelectDevice(dev) }
                        }
                    }

                    item {
                        Text(
                            "Discovered Devices",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    if (state.scannedDevices.isEmpty()) {
                        item {
                            Text(
                                if (state.isScanning) "Searching for nearby devices..." else "No devices found.",
                                color = Color.Gray
                            )
                        }
                    } else {
                        items(state.scannedDevices) { dev ->
                            DeviceRow(dev) { onSelectDevice(dev) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun DeviceRow(device: BluetoothDeviceModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = device.name ?: "Unknown Device",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = device.address,
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
