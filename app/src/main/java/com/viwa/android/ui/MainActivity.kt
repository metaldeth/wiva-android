package com.viwa.android.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.viwa.android.ui.screens.customer.ViwaCustomerUiTokens
import com.viwa.android.ui.theme.GLOBAL_UI_SCALE
import com.viwa.android.ui.theme.LocalCustomerPrimaryButtonColor
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.viwa.android.hardware.scanner.ScannerManager
import com.viwa.android.ui.navigation.ViwaNavGraph
import com.viwa.android.ui.navigation.Routes
import com.viwa.android.ui.screens.service.ServiceScreenLaunch
import com.viwa.android.ui.screens.idle.IdleVideoOverlay
import com.viwa.android.ui.screens.idle.IdleVideoViewModel
import com.viwa.android.services.telemetry.EmployeeKeyServiceMenuCoordinator
import com.viwa.android.services.telemetry.LoyaltyCardScanCoordinator
import com.viwa.android.services.telemetry.TelemetryRegistrationScannerCoordinator
import com.viwa.android.domain.repository.NanoKassaRepository
import com.viwa.android.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val LightScheme =
    lightColorScheme(
        primary = Color(0xFF008CF0),
        background = Color(0xFFF3F3F4),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFEEEEF0),
        onBackground = Color(0xFF1A1C1E),
        onSurface = Color(0xFF1A1C1E),
        onSurfaceVariant = Color(0xFF43474E),
        outline = Color(0xFFD0D0D4),
        outlineVariant = Color(0xFFE8E8EC),
        inverseSurface = Color(0xFF2D2D2D),
        inverseOnSurface = Color(0xFFF0F0F0),
    )

private val DarkScheme =
    darkColorScheme(
        primary = Color(0xFF8E24AA),
        background = Color(0xFF1C1C1E),
        surface = Color(0xFF2C2C2E),
        surfaceVariant = Color(0xFF2C2C2E),
        onBackground = Color(0xFFE0E0E0),
        onSurface = Color(0xFFE0E0E0),
        onSurfaceVariant = Color(0xFF8A8A8E),
        outline = Color(0xFF3A3A3C),
        outlineVariant = Color(0xFF3A3A3C),
        inverseSurface = Color(0xFFE0E0E0),
        inverseOnSurface = Color(0xFF1C1C1E),
    )

@UnstableApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
 /** Глобальная подписка на сканер карты лояльности (инициализация @Singleton). */
    @Inject
    lateinit var loyaltyCardScanCoordinator: LoyaltyCardScanCoordinator

 /** Сканер должен читать всегда, пока жив kiosk activity, а не только из сервисной вкладки. */
    @Inject
    lateinit var scannerManager: ScannerManager

 /** KEY-* → authCodeRequestExport → навигация в сервис (. */
    @Inject
    lateinit var employeeKeyServiceMenuCoordinator: EmployeeKeyServiceMenuCoordinator

    /** QR/REG → поля регистрации телеметрии (инициализация @Singleton). */
    @Inject
    lateinit var telemetryRegistrationScannerCoordinator: TelemetryRegistrationScannerCoordinator

    @Inject
    lateinit var nanoKassaRepository: NanoKassaRepository

    private val themeViewModel: ThemeViewModel by viewModels()
    private val idleVideoViewModel: IdleVideoViewModel by viewModels()

 /** Выставляется в [setContent] для [onNewIntent] (открыть сервис с дашбордом). */
    private var navigateToServiceLambda: (() -> Unit)? = null

    override fun onUserInteraction() {
        super.onUserInteraction()
        idleVideoViewModel.resetTimer()
    }

    private val requestLocationPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _: Map<String, Boolean> -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
 // Back navigation blocked in kiosk mode
                }
            },
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val needCoarse =
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED
            if (needCoarse) {
                requestLocationPermissions.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    ),
                )
            }
        }
        hideSystemBars()
        setupImmersiveInsetsListener()
        scannerManager.startReading()
        telemetryRegistrationScannerCoordinator
        setContent {
            val isDark by themeViewModel.isDark.collectAsStateWithLifecycle()
            val customerPrimaryLightArgb by themeViewModel.customerPrimaryLightArgb.collectAsStateWithLifecycle(
                initialValue = ViwaCustomerUiTokens.DefaultBrandPrimaryArgb,
            )
            val customerPrimaryDarkArgb by themeViewModel.customerPrimaryDarkArgb.collectAsStateWithLifecycle(
                initialValue = ViwaCustomerUiTokens.DefaultBrandPrimaryArgbDark,
            )
            val brandPrimaryArgb = if (isDark) customerPrimaryDarkArgb else customerPrimaryLightArgb
            val brandPrimaryColor = Color(brandPrimaryArgb)
            val colorScheme =
                (if (isDark) DarkScheme else LightScheme).copy(primary = brandPrimaryColor)
            val isIdleVisible by idleVideoViewModel.isVisible.collectAsStateWithLifecycle()
            val enabledVideoIds by idleVideoViewModel.enabledVideoIds.collectAsStateWithLifecycle()
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides
                    Density(
                        density = baseDensity.density * GLOBAL_UI_SCALE,
                        fontScale = baseDensity.fontScale,
                    ),
            ) {
                MaterialTheme(colorScheme = colorScheme) {
                    CompositionLocalProvider(LocalCustomerPrimaryButtonColor provides brandPrimaryColor) {
                        val navController = rememberNavController()
                        val backStackEntry by navController.currentBackStackEntryAsState()

                        LaunchedEffect(navController) {
                            navigateToServiceLambda = {
                                navController.navigate(Routes.Service) {
                                    launchSingleTop = true
                                }
                            }
                            if (intent.getBooleanExtra("open_service_dashboard", false)) {
                                intent.removeExtra("open_service_dashboard")
                                ServiceScreenLaunch.selectDashboardOnOpen = true
                                navigateToServiceLambda?.invoke()
                            }
                        }

                        LaunchedEffect(navController) {
                            employeeKeyServiceMenuCoordinator.openServiceMenuRequests.collect {
                                if (navController.currentDestination?.route != Routes.Home) return@collect
                                idleVideoViewModel.resetTimer()
                                navController.navigate(Routes.Service)
                            }
                        }

                        LaunchedEffect(Unit) {
                            withContext(Dispatchers.IO) {
                                runCatching { nanoKassaRepository.verifyIntegration() }
                            }
                        }

                        // Idle-таймер работает только на экране выбора напитков
                        LaunchedEffect(backStackEntry) {
                            val route = backStackEntry?.destination?.route
                            idleVideoViewModel.setActive(route == Routes.Home)
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            if (isIdleVisible) {
                                IdleVideoOverlay(
                                    enabledVideoIds = enabledVideoIds,
                                    onDismiss = { idleVideoViewModel.resetTimer() },
                                )
                            } else {
                                ViwaNavGraph(navController = navController)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("open_service_dashboard", false)) {
            intent.removeExtra("open_service_dashboard")
            ServiceScreenLaunch.selectDashboardOnOpen = true
            navigateToServiceLambda?.invoke()
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onDestroy() {
        scannerManager.stop()
        super.onDestroy()
    }

    private val hideRunnable = Runnable { hideSystemBars() }

    private fun setupImmersiveInsetsListener() {
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            val compat = WindowInsetsCompat.toWindowInsetsCompat(insets, view)
            val imeVisible = compat.isVisible(WindowInsetsCompat.Type.ime())
            val navVisible = compat.isVisible(WindowInsetsCompat.Type.navigationBars())
            if (navVisible && !imeVisible) {
                view.removeCallbacks(hideRunnable)
                view.postDelayed(hideRunnable, 150)
            }
            view.onApplyWindowInsets(insets)
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                )
        }
    }
}
