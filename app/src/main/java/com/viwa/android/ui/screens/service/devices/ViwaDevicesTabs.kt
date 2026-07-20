package com.viwa.android.ui.screens.service.devices

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.viwa.android.ui.screens.service.SettingsColumn
import timber.log.Timber

@Composable
fun ViwaDevicesTab(
    modifier: Modifier = Modifier,
    viewModel: ViwaDevicesViewModel =
        hiltViewModel(LocalContext.current as ComponentActivity),
) {
    Timber.tag("ViwaDevicesTab").i("compose")
    val state by viewModel.devicesController.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.devicesController.refreshDevices()
    }
    SettingsColumn(modifier = modifier) {
        DevicesBlockContent(
            state = state,
            actions = viewModel.devicesController,
            embeddedInScrollParent = true,
            blockLabels = DevicesBlockUiLabels.KioskDefaults,
        )
    }
}

@Composable
fun ViwaDevicesControllerTab(
    modifier: Modifier = Modifier,
    viewModel: ViwaDevicesViewModel =
        hiltViewModel(LocalContext.current as ComponentActivity),
) {
    ControllerPortBlockContent(
        controller = viewModel.controllerPortController,
        modifier = modifier,
    )
}

@Composable
fun ViwaDevicesPortsTab(
    modifier: Modifier = Modifier,
    viewModel: ViwaDevicesViewModel =
        hiltViewModel(LocalContext.current as ComponentActivity),
) {
    ViwaPortsBlockContent(
        controllerPortScanController = viewModel.portScanController,
        modifier = modifier,
    )
}

@Composable
fun ViwaDevicesPaymentTab(
    modifier: Modifier = Modifier,
    viewModel: ViwaDevicesViewModel =
        hiltViewModel(LocalContext.current as ComponentActivity),
) {
    PaymentTerminalBlockContent(
        controller = viewModel.paymentTerminalController,
        modifier = modifier,
    )
}

@Composable
fun ViwaDevicesScannerTab(
    modifier: Modifier = Modifier,
    viewModel: ViwaDevicesViewModel =
        hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val scannerActive by viewModel.scannerActive.collectAsStateWithLifecycle()
    ScannerDebugBlockContent(
        controller = viewModel.scannerDebugController,
        scannerActive = scannerActive,
        onStartScanner = viewModel::startScanner,
        onStopScanner = viewModel::stopScanner,
        modifier = modifier,
    )
}
