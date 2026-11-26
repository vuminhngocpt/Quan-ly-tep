package com.vuminhngoc.quanlytep

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.vuminhngoc.quanlytep.model.StorageSource
import com.vuminhngoc.quanlytep.ui.FileManagerScreen
import com.vuminhngoc.quanlytep.ui.theme.QuanLyTepTheme
import com.vuminhngoc.quanlytep.viewmodel.FileManagerViewModel

class MainActivity : ComponentActivity() {

    private var hasRequestedAllFilesAccess = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContext = applicationContext

        setContent {
            val viewModel = remember(appContext) { FileManagerViewModel(appContext) }
            val state = viewModel.uiState
            val lifecycleOwner = LocalLifecycleOwner.current

            LaunchedEffect(Unit) {
                requestAllFilesAccessIfNeeded()
            }

            DisposableEffect(lifecycleOwner) {
                viewModel.refreshRealStorageMetadata(StorageSource.DEVICE)
                viewModel.refreshRealStorageMetadata(StorageSource.SD)

                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewModel.refreshRealStorageMetadata(StorageSource.DEVICE)
                        viewModel.refreshRealStorageMetadata(StorageSource.SD)
                        viewModel.refreshRealStorageMetadata(state.source)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            BackHandler(enabled = state.selectMode || state.folderStack.isNotEmpty() || state.canGoBack) {
                when {
                    state.selectMode -> viewModel.setSelectMode(false)
                    state.folderStack.isNotEmpty() -> viewModel.navigateCrumb(state.folderStack.lastIndex - 1)
                    state.canGoBack -> viewModel.goHistory(-1)
                }
            }

            QuanLyTepTheme(themeMode = state.settings.themeMode) {
                FileManagerScreen(viewModel = viewModel)
            }
        }
    }

    private fun requestAllFilesAccessIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            if (hasRequestedAllFilesAccess) return
            hasRequestedAllFilesAccess = true
            runCatching {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }.onFailure {
                runCatching {
                    startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            }
        }
    }
}
