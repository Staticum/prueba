package com.staticum.niagaralauncher

import android.app.Activity
import android.app.AlarmManager
import android.app.WallpaperManager
import android.app.role.RoleManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.staticum.niagaralauncher.data.AppInfo
import com.staticum.niagaralauncher.data.SwipeDirection
import com.staticum.niagaralauncher.ui.home.AmbientLockScreen
import com.staticum.niagaralauncher.ui.home.HomeScreen
import com.staticum.niagaralauncher.ui.home.HomeViewModel
import com.staticum.niagaralauncher.ui.settings.AppPickerScreen
import com.staticum.niagaralauncher.ui.settings.ColorPickerScreen
import com.staticum.niagaralauncher.ui.settings.SettingsScreen
import com.staticum.niagaralauncher.ui.settings.SettingsViewModel
import com.staticum.niagaralauncher.ui.settings.SoundPickerScreen
import com.staticum.niagaralauncher.ui.theme.LauncherTheme
import com.staticum.niagaralauncher.update.UpdateUiState
import com.staticum.niagaralauncher.update.UpdateViewModel
import com.staticum.niagaralauncher.util.CrashLogger
import com.staticum.niagaralauncher.util.SafeModeGuard
import com.staticum.niagaralauncher.util.ViewModelFactory
import com.staticum.niagaralauncher.util.isDefaultLauncher
import com.staticum.niagaralauncher.widget.WidgetHostProvider
import com.staticum.niagaralauncher.widget.WidgetPickerScreen

private enum class Screen {
    HOME, SETTINGS, WIDGET_PICKER, FAVORITES_PICKER, HIDDEN_PICKER, COLOR_PICKER, SOUND_PICKER
}

class MainActivity : ComponentActivity() {

    private val factory by lazy { ViewModelFactory(this) }
    private val homeViewModel: HomeViewModel by viewModels { factory }
    private val settingsViewModel: SettingsViewModel by viewModels { factory }
    private val updateViewModel: UpdateViewModel by viewModels { factory }

    private var onWallpaperPicked: ((Uri?) -> Unit)? = null
    private var pendingWidgetId: Int = -1

    private val isDefaultLauncherState = mutableStateOf(false)
    // The next alarm across ALL apps, not just a clock app of ours - there is no
    // public API to enumerate every alarm on the device (that's a per-app internal
    // detail), but AlarmManager.nextAlarmClock is the one system-wide value every
    // launcher and lock screen already relies on to show "next alarm" without any
    // special permission.
    private val nextAlarmMillis = mutableStateOf<Long?>(null)
    private val screenState = mutableStateOf(Screen.HOME)
    private var widgetsSuppressed = false

    // Bumped on every onStart (app brought back to foreground) so the ambient lock
    // screen, when enabled, re-locks each time the launcher is returned to - not
    // just on the very first launch.
    private val lockTrigger = mutableStateOf(0)

    private val requestHomeRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { isDefaultLauncherState.value = isDefaultLauncher(this) }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri -> onWallpaperPicked?.invoke(uri) }

    /** Result of ACTION_APPWIDGET_CONFIGURE, launched after a successful bind. */
    private val configureWidgetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val id = pendingWidgetId
        pendingWidgetId = -1
        if (result.resultCode == Activity.RESULT_OK && id != -1) {
            settingsViewModel.addWidget(id)
        } else if (id != -1) {
            WidgetHostProvider.get(this).deleteAppWidgetId(id)
        }
        screenState.value = Screen.SETTINGS
    }

    /** Result of ACTION_APPWIDGET_BIND, only needed when bindAppWidgetIdIfAllowed() returns false. */
    private val requestBindLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val id = pendingWidgetId
        if (result.resultCode == Activity.RESULT_OK && id != -1) {
            proceedAfterBind(id)
        } else {
            pendingWidgetId = -1
            if (id != -1) WidgetHostProvider.get(this).deleteAppWidgetId(id)
            screenState.value = Screen.SETTINGS
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // A widget that crashes the process on render could otherwise lock the user
        // out of their own home screen forever (crash on open -> relaunch -> crash
        // again). If the previous launch never settled, skip widgets this run so
        // Settings (which only shows widget labels/icons, never live views) stays
        // reachable to remove the offending one.
        widgetsSuppressed = SafeModeGuard.onLaunchStart(this)

        setContent {
            val screen by screenState
            val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val updateState by updateViewModel.state.collectAsStateWithLifecycle()
            val isDefaultLauncher by isDefaultLauncherState
            val context = LocalContext.current
            val lockTriggerValue by lockTrigger
            // Re-read on every resume (the trigger bumps in onStart), because the
            // user grants Usage access in the system settings and comes back.
            val hasUsageAccess = remember(lockTriggerValue, screen) {
                com.staticum.niagaralauncher.data.UsageRepository.hasUsageAccess(this@MainActivity)
            }

            var isLocked by remember { mutableStateOf(false) }
            LaunchedEffect(homeState.prefs.ambientLockEnabled, lockTriggerValue) {
                if (homeState.prefs.ambientLockEnabled) isLocked = true
            }

            LauncherTheme(palette = homeState.prefs.palette) {
                Surface(color = androidx.compose.ui.graphics.Color.Transparent) {
                    Box(modifier = Modifier) {
                        when (screen) {
                            Screen.HOME -> HomeScreen(
                                state = homeState,
                                isDefaultLauncher = isDefaultLauncher,
                                widgets = if (widgetsSuppressed) emptyList() else settingsState.widgets,
                                widgetsSuppressed = widgetsSuppressed,
                                onQueryChange = homeViewModel::onQueryChange,
                                onLaunchApp = { app -> launchApp(app) },
                                onLongPressApp = { app -> homeViewModel.toggleHidden(app, hidden = true) },
                                onOpenAppInfo = { app -> openAppDetailsSettings(app.packageName) },
                                nextAlarmMillis = nextAlarmMillis.value,
                                onOpenSettings = { screenState.value = Screen.SETTINGS },
                                onSwipe = { direction ->
                                    homeState.favoriteFor(direction)?.let { launchApp(it) }
                                },
                                onSetAsDefaultLauncher = { requestDefaultLauncher() },
                                onPlaceWidget = settingsViewModel::placeWidget,
                                onRemoveWidget = { id ->
                                    WidgetHostProvider.get(context).deleteAppWidgetId(id)
                                    settingsViewModel.removeWidget(id)
                                },
                                onRemoveInvalidWidget = { id ->
                                    WidgetHostProvider.get(context).deleteAppWidgetId(id)
                                    settingsViewModel.removeWidget(id)
                                },
                            )

                            Screen.SETTINGS -> SettingsScreen(
                                state = settingsState,
                                updateState = updateState,
                                onBack = { screenState.value = Screen.HOME },
                                onPaletteSelected = { settingsViewModel.setPalette(it.id) },
                                onPickWallpaper = { pickWallpaper() },
                                onClearWallpaper = { settingsViewModel.setUseWallpaper(false) },
                                onIconSizeChange = settingsViewModel::setIconSizeFactor,
                                onMonochromeChange = settingsViewModel::setMonochromeIcons,
                                onIconSilhouetteChange = settingsViewModel::setIconSilhouette,
                                onScreenTintModeChange = settingsViewModel::setScreenTintMode,
                                onIndexWaveOffsetChange = settingsViewModel::setIndexWaveOffset,
                                onHomeResetSecondsChange = settingsViewModel::setHomeResetSeconds,
                                onAddWidget = { screenState.value = Screen.WIDGET_PICKER },
                                onRemoveWidget = { id ->
                                    WidgetHostProvider.get(context).deleteAppWidgetId(id)
                                    settingsViewModel.removeWidget(id)
                                },
                                onCheckForUpdate = updateViewModel::checkForUpdate,
                                onDownloadUpdate = {
                                    (updateState as? UpdateUiState.Available)?.let {
                                        updateViewModel.downloadUpdate(it.info)
                                    }
                                },
                                onInstallUpdate = {
                                    (updateState as? UpdateUiState.ReadyToInstall)?.let {
                                        installApk(it.file)
                                    }
                                },
                                onOpenFavoritesPicker = { screenState.value = Screen.FAVORITES_PICKER },
                                onOpenHiddenPicker = { screenState.value = Screen.HIDDEN_PICKER },
                                onOpenColorPicker = { screenState.value = Screen.COLOR_PICKER },
                                onOpenSoundPicker = { screenState.value = Screen.SOUND_PICKER },
                                onShareCrashLog = { shareCrashLog() },
                                onClearCrashLog = { CrashLogger.clear(this@MainActivity) },
                                isDefaultLauncher = isDefaultLauncher,
                                onChangeDefaultLauncher = {
                                    // Requesting the ROLE_HOME role when it's already held
                                    // just auto-resolves with no UI, which read as the
                                    // button "doing nothing" - to switch AWAY from being
                                    // default, the system's own Default apps > Home screen
                                    // picker is the only way, so go straight there instead.
                                    if (isDefaultLauncher) {
                                        startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                                    } else {
                                        requestDefaultLauncher()
                                    }
                                },
                                onAmbientLockChange = settingsViewModel::setAmbientLockEnabled,
                                onWidgetBackgroundChange = settingsViewModel::setWidgetBackground,
                                onFrequentsEnabledChange = settingsViewModel::setFrequentsEnabled,
                                onFrequentsCountChange = settingsViewModel::setFrequentsCount,
                                onUseSystemUsageChange = { enabled ->
                                    settingsViewModel.setUseSystemUsageStats(enabled)
                                    if (enabled && !hasUsageAccess) openUsageAccessSettings()
                                    homeViewModel.refreshSystemUsage()
                                },
                                hasUsageAccess = hasUsageAccess,
                                onOpenUsageAccess = { openUsageAccessSettings() },
                                onClearUsageHistory = { settingsViewModel.clearUsageHistory() },
                            )

                            Screen.WIDGET_PICKER -> WidgetPickerScreen(
                                palette = homeState.prefs.palette,
                                onBack = { screenState.value = Screen.SETTINGS },
                                onProviderSelected = { provider -> startBind(provider) },
                            )

                            Screen.FAVORITES_PICKER -> AppPickerScreen(
                                title = "Apps favoritas",
                                palette = homeState.prefs.palette,
                                allApps = homeState.allApps,
                                selectedKeys = homeState.prefs.favoriteAppKeys.toSet(),
                                onToggle = { app, selected -> settingsViewModel.toggleFavoriteApp(app.key, selected) },
                                onBack = { screenState.value = Screen.SETTINGS },
                            )

                            Screen.HIDDEN_PICKER -> AppPickerScreen(
                                title = "Apps ocultas",
                                palette = homeState.prefs.palette,
                                allApps = homeState.allApps,
                                selectedKeys = homeState.prefs.hiddenApps,
                                onToggle = { app, hidden -> homeViewModel.toggleHidden(app, hidden) },
                                onBack = { screenState.value = Screen.SETTINGS },
                            )

                            Screen.COLOR_PICKER -> ColorPickerScreen(
                                palette = homeState.prefs.palette,
                                initialColor = homeState.prefs.palette.accent,
                                onColorChange = { color ->
                                    settingsViewModel.setCustomAccentColor(color.toArgb())
                                },
                                onBack = { screenState.value = Screen.SETTINGS },
                            )

                            Screen.SOUND_PICKER -> SoundPickerScreen(
                                palette = homeState.prefs.palette,
                                selectedId = homeState.prefs.soundId,
                                volume = homeState.prefs.soundVolume,
                                onSelect = settingsViewModel::setSoundId,
                                onVolumeChange = settingsViewModel::setSoundVolume,
                                onBack = { screenState.value = Screen.SETTINGS },
                            )
                        }

                        if (isLocked) {
                            AmbientLockScreen(
                                palette = homeState.prefs.palette,
                                onUnlock = { isLocked = false },
                            )
                        }
                    }
                }
            }

            // Screen tint desaturates (and optionally re-tints) the whole screen as a
            // single rendered layer, including native widget Views embedded via
            // AndroidView, which a Compose ColorFilter can't reach since they aren't
            // Compose draw calls.
            val tintMode = homeState.prefs.screenTintMode
            val tintAccent = homeState.prefs.palette.accent
            LaunchedEffect(tintMode, tintAccent) {
                val contentRoot = findViewById<ViewGroup>(android.R.id.content)
                when (tintMode) {
                    com.staticum.niagaralauncher.data.ScreenTintMode.NONE -> {
                        contentRoot.setLayerType(View.LAYER_TYPE_NONE, null)
                    }
                    com.staticum.niagaralauncher.data.ScreenTintMode.GRAYSCALE -> {
                        val paint = Paint().apply {
                            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                        }
                        contentRoot.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
                    }
                    com.staticum.niagaralauncher.data.ScreenTintMode.COLOR -> {
                        // Desaturate first, then scale each channel toward the accent
                        // color's own channel weights - a cheap duotone: the picture's
                        // luminance ends up rendered entirely in shades of that color.
                        val grayscale = ColorMatrix().apply { setSaturation(0f) }
                        val r = android.graphics.Color.red(tintAccent.toArgb()) / 255f
                        val g = android.graphics.Color.green(tintAccent.toArgb()) / 255f
                        val b = android.graphics.Color.blue(tintAccent.toArgb()) / 255f
                        val tint = ColorMatrix(
                            floatArrayOf(
                                r, 0f, 0f, 0f, 0f,
                                0f, g, 0f, 0f, 0f,
                                0f, 0f, b, 0f, 0f,
                                0f, 0f, 0f, 1f, 0f,
                            ),
                        )
                        grayscale.postConcat(tint)
                        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(grayscale) }
                        contentRoot.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
                    }
                }
            }
        }
    }

    /** AppWidgetHost must listen only while the Activity is actually shown, per its
     * documented contract — otherwise widget updates (e.g. Spotify's now-playing state)
     * can silently stop propagating to the host views after the app has been backgrounded
     * and resumed a few times, which is what made widgets appear to "break". */
    override fun onStart() {
        super.onStart()
        WidgetHostProvider.get(this).startListening()
        lockTrigger.value++
    }

    override fun onStop() {
        WidgetHostProvider.get(this).stopListening()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.refreshApps()
        isDefaultLauncherState.value = isDefaultLauncher(this)
        homeViewModel.refreshSystemUsage()
        refreshNextAlarm()
        // Only mark the launch as "settled" (safe to try widgets again next time)
        // after staying up for a bit - a near-instant crash right after resuming
        // wouldn't get the chance to run this.
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
            { SafeModeGuard.onLaunchSettled(this) },
            2500,
        )
    }

    private fun refreshNextAlarm() {
        val alarmManager = getSystemService(AlarmManager::class.java)
        nextAlarmMillis.value = alarmManager?.nextAlarmClock?.triggerTime
    }

    private fun requestDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                requestHomeRoleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
                return
            }
        }
        startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
    }

    private fun launchApp(app: AppInfo) {
        // Single choke point for every app opening in the launcher, so this is the
        // one place the usage counter has to be fed.
        homeViewModel.recordLaunch(app)
        val factory = com.staticum.niagaralauncher.data.AppRepository(this)
        startActivity(factory.launchIntentFor(app))
    }

    private fun openAppDetailsSettings(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    /** PACKAGE_USAGE_STATS is a special permission: there is no runtime dialog for
     * it, the user has to toggle it by hand in the system's Usage access screen. */
    private fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            android.widget.Toast.makeText(
                this,
                "Este dispositivo no ofrece la pantalla de Acceso de uso",
                android.widget.Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun shareCrashLog() {
        val file = CrashLogger.logFile(this)
        if (!CrashLogger.hasLog(this)) {
            android.widget.Toast.makeText(this, "No hay errores registrados", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Compartir registro de errores"))
    }

    private fun installApk(file: java.io.File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        startActivity(intent)
    }

    private fun pickWallpaper() {
        onWallpaperPicked = { uri ->
            if (uri != null) {
                WallpaperManager.getInstance(this).setStream(contentResolver.openInputStream(uri))
                settingsViewModel.setWallpaperUri(uri.toString())
                settingsViewModel.setUseWallpaper(true)
            }
            // uri == null means the user cancelled the picker; leave the previous setting untouched.
        }
        pickImageLauncher.launch("image/*")
    }

    /** Reliable widget-add flow for a third-party launcher: allocate an id, try the
     * automatic bind the OS grants to the current default launcher, and only fall back
     * to the user-facing ACTION_APPWIDGET_BIND consent screen if that's refused. */
    private fun startBind(provider: AppWidgetProviderInfo) {
        val host = WidgetHostProvider.get(this)
        val manager = WidgetHostProvider.manager(this)
        val id = host.allocateAppWidgetId()
        pendingWidgetId = id

        val allowed = manager.bindAppWidgetIdIfAllowed(id, provider.provider)
        if (allowed) {
            proceedAfterBind(id)
        } else {
            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
            }
            requestBindLauncher.launch(bindIntent)
        }
    }

    private fun proceedAfterBind(id: Int) {
        val provider = WidgetHostProvider.manager(this).getAppWidgetInfo(id)
        if (provider?.configure != null) {
            pendingWidgetId = id
            val configureIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = provider.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            }
            configureWidgetLauncher.launch(configureIntent)
        } else {
            pendingWidgetId = -1
            settingsViewModel.addWidget(id)
            screenState.value = Screen.SETTINGS
        }
    }
}
