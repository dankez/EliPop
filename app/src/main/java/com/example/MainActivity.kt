package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.configureAppCheck
import com.example.ui.PastelkaRoot
import com.example.ui.PastelkaViewModel
import com.example.ui.theme.PastelkaTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PastelkaViewModel by viewModels {
        PastelkaViewModel.Factory(application as PastelkaApp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure Firebase App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            PastelkaTheme(
                themeMode = uiState.preferences.themeMode,
                dynamicColor = uiState.preferences.useDynamicColor,
            ) {
                PastelkaRoot(
                    uiState = uiState,
                    messages = viewModel.messages,
                    viewModel = viewModel,
                )
            }
        }
    }
}
