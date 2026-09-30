package com.bat.controller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bat.controller.bluetooth.BluetoothConnectionState
import com.bat.controller.ui.theme.AccentGreen
import com.bat.controller.ui.theme.AccentRed
import com.bat.controller.ui.theme.AccentYellow

@Composable
fun ConnectionStatusCard(
    mode: String,
    onModeChange: (String) -> Unit,
    btState: BluetoothConnectionState,
    onOpenBtDialog: () -> Unit,
    ip: String,
    onIpChange: (String) -> Unit,
    port: String,
    onPortChange: (String) -> Unit,
    onConnectTcp: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Connection Channel",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = mode == "BLUETOOTH",
                        onClick = { onModeChange("BLUETOOTH") },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = { Icon(Icons.Default.Bluetooth, contentDescription = null) }
                    ) {
                        Text("BT")
                    }
                    SegmentedButton(
                        selected = mode == "TCP",
                        onClick = { onModeChange("TCP") },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = { Icon(Icons.Default.Wifi, contentDescription = null) }
                    ) {
                        Text("TCP")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (mode == "BLUETOOTH") {
                val (statusText, statusColor) = when (btState) {
                    is BluetoothConnectionState.Connected -> Pair("Connected: ${btState.deviceName}", AccentGreen)
                    is BluetoothConnectionState.Connecting -> Pair("Connecting...", AccentYellow)
                    is BluetoothConnectionState.Error -> Pair("Error: ${btState.message}", AccentRed)
                    BluetoothConnectionState.Disconnected -> Pair("Disconnected", Color.Gray)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onOpenBtDialog,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Scan / Pair")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = ip,
                        onValueChange = onIpChange,
                        label = { Text("IP") },
                        modifier = Modifier.weight(2f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = port,
                        onValueChange = onPortChange,
                        label = { Text("Port") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = onConnectTcp,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Connect")
                    }
                }
            }
        }
    }
}
