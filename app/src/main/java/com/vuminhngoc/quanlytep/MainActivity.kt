package com.vuminhngoc.quanlytep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.vuminhngoc.quanlytep.ui.FileManagerScreen
import com.vuminhngoc.quanlytep.ui.theme.QuanLyTepTheme
import com.vuminhngoc.quanlytep.viewmodel.FileManagerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel = remember { FileManagerViewModel() }
            val state = viewModel.uiState

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
}
