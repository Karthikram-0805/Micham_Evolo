package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.MainAppContainer
import com.example.ui.theme.MichamEvloTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val userSettings by viewModel.userSettings.collectAsState()

            MichamEvloTheme(themeMode = userSettings.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppContainer(viewModel = viewModel)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // If app lock is enabled, lock app when going to background
        viewModel.lockApp()
    }
}
