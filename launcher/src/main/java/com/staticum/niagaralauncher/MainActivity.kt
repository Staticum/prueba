package com.staticum.niagaralauncher

import android.app.Activity
import android.app.WallpaperManager
import android.app.role.RoleManager
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.staticum.niagaralauncher.data.AppInfo
import com.staticum.niagaralauncher.data.SwipeDirection
import com.staticum.niagaralauncher.ui.home.HomeScreen
import com.staticum.niagaralauncher.ui.home.HomeViewModel
import com.staticum.niagaralauncher.ui.settings.SettingsScreen
import com.staticum.niagaralauncher.ui.settings.SettingsViewModel
import com.staticum.niagaralauncher.ui.theme.LauncherTheme
import com.staticum.niagaralauncher.update.UpdateUiState
import com.staticum.niagaralauncher.update.UpdateViewModel
import com.staticum.niagaralauncher.util.ViewModelFactory
import com.staticum.niagaralauncher.util.isDefaultLauncher
import com.staticum.niagaralauncher.widget.WidgetHostProvider

private enum class Screen { HOME, SETTINGS }

class MainActivity : ComponentActivity() {

    private val factory by lazy { ViewModelFactory(this) }
    private val homeViewModel: HomeViewModel by viewModels { factory }
    private val settingsViewModel: SettingsViewModel by viewModels { factory }
    private val updateViewModel: UpdateViewModel by viewModels { factory }

    private var onWallpaperPicked: ((Uri?) -> Unit)? = null
    private var onWidgetPicked: ((Int?) -> Unit)? = null
    private var pendingConfigureWidgetId: Int = -1

    private val isDefaultLauncherState = mutableStateOf(false)

    private val requestHomeRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { isDefaultLauncherState.value = isDefaultLauncher(this) }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri -> onWallpaperPicked?.invoke(uri) }

    private val bindWidgetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val id = pendingConfigureWidgetId
        pendingConfigureWidgetId = -1
        onWidgetPicked?.invoke(if (result.resultCode == Activity.RESULT_OK && id != -1) id else null)
    }

    private val pickWidgetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (result.resultCode != Activity.RESULT_OK || id == -1) {
            onWidgetPicked?.invoke(null)
            return@registerForActivityResult
        }
        val provider = WidgetHostProvider.manager(this).getAppWidgetInfo(id)
        if (provider?.configure != null) {
            val configureIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = provider.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            }
            pendingConfigureWidgetId = id
            bindWidgetLauncher.launch(configureIntent)
        } else {
            onWidgetPicked?.invoke(id)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var screen by remember { mutableStateOf(Screen.HOME) }
            val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val updateState by updateViewModel.state.collectAsStateWithLifecycle()
            val isDefaultLauncher by isDefaultLauncherState
            val context = LocalContext.current

            LauncherTheme(palette = homeState.prefs.palette) {
                Surface(color = androidx.compose.ui.graphics.Color.Transparent) {
                    Box(modifier = Modifier) {
                        when (screen) {
                            Screen.HOME -> HomeScreen(
                                state = homeState,
                                isDefaultLauncher = isDefaultLauncher,
                                widgetIds = settingsState.widgetIds,
                                onQueryChange = homeViewModel::onQueryChange,
                                onLaunchApp = { app -> launchApp(app) },
                                onLongPressApp = { app -> homeViewModel.toggleHidden(app, hidden = true) },
                                onOpenSettings = { screen = Screen.SETTINGS },
                                onSwipe = { direction ->
                                    homeState.favoriteFor(direction)?.let { launchApp(it) }
                                },
                                onSetAsDefaultLauncher = { requestDefaultLauncher() },
                            )

                            Screen.SETTINGS -> SettingsScreen(
                                state = settingsState,
                                allApps = homeState.allApps,
                                updateState = updateState,
                                onBack = { screen = Screen.HOME },
                                onPaletteSelected = { settingsViewModel.setPalette(it.id) },
                                onPickWallpaper = { pickWallpaper() },
                                onClearWallpaper = { settingsViewModel.setUseWallpaper(false) },
                                onIconSizeChange = settingsViewModel::setIconSizeFactor,
                                onMonochromeChange = settingsViewModel::setMonochromeIcons,
                                onToggleHidden = { app, hidden -> homeViewModel.toggleHidden(app, hidden) },
                                onAddWidget = { pickWidget() },
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
                            )
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                WidgetHostProvider.get(context).startListening()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.refreshApps()
        isDefaultLauncherState.value = isDefaultLauncher(this)
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
        val factory = com.staticum.niagaralauncher.data.AppRepository(this)
        startActivity(factory.launchIntentFor(app))
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

    private fun pickWidget() {
        val host = WidgetHostProvider.get(this)
        val newId = host.allocateAppWidgetId()
        onWidgetPicked = { grantedId ->
            if (grantedId != null) {
                settingsViewModel.addWidget(grantedId)
            } else {
                host.deleteAppWidgetId(newId)
            }
        }
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, newId)
        }
        pickWidgetLauncher.launch(pickIntent)
    }
}
