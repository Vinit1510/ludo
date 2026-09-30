package com.bat.controller

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.bat.controller.ui.MainViewModel
import com.bat.controller.ui.components.*
import com.bat.controller.ui.theme.LudoQAControllerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            viewModel.bluetoothController.updateBluetoothEnabled()
            viewModel.log("All Bluetooth and Location permissions granted.")
        } else {
            Toast.makeText(this, "Bluetooth permissions required for discovery", Toast.LENGTH_LONG).show()
            viewModel.log("Some permissions were denied.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()

        setContent {
            LudoQAControllerTheme {
                val btUiState by viewModel.bluetoothController.uiState.collectAsState()
                val selectedMode by viewModel.selectedMode.collectAsState()
                val targetIp by viewModel.targetIp.collectAsState()
                val targetPort by viewModel.targetPort.collectAsState()
                val roomCode by viewModel.roomCode.collectAsState()
                val lastRollValue by viewModel.lastRollValue.collectAsState()
                val logs by viewModel.logs.collectAsState()

                var showBtDialog by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        "Ludo QA Controller",
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ConnectionStatusCard(
                                mode = selectedMode,
                                onModeChange = { viewModel.setMode(it) },
                                btState = btUiState.connectionState,
                                onOpenBtDialog = { showBtDialog = true },
                                ip = targetIp,
                                onIpChange = { viewModel.setIp(it) },
                                port = targetPort,
                                onPortChange = { viewModel.setPort(it) },
                                onConnectTcp = { viewModel.connectTcp() }
                            )

                            DiceControlSection(
                                roomCode = roomCode,
                                onRoomCodeChange = { viewModel.setRoomCode(it) },
                                lastValue = lastRollValue,
                                onSelectDice = { viewModel.sendDiceCommand(it) },
                                onCustomAction = { viewModel.sendCustomCommand(it) }
                            )

                            LogConsole(
                                logs = logs,
                                onClear = { viewModel.clearLogs() }
                            )
                        }

                        if (showBtDialog) {
                            BluetoothDeviceDialog(
                                state = btUiState,
                                onStartScan = { viewModel.bluetoothController.startDiscovery() },
                                onStopScan = { viewModel.bluetoothController.stopDiscovery() },
                                onSelectDevice = { device ->
                                    viewModel.connectBluetoothDevice(device)
                                    showBtDialog = false
                                },
                                onDismiss = { showBtDialog = false }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }
}
