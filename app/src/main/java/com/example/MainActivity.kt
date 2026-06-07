package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.example.ui.MainLayout
import com.example.ui.MusicViewModel
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.TbmMusicTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var themeMode by remember { mutableStateOf(ThemeMode.DARK) }

            TbmMusicTheme(themeMode = themeMode) {
                MainLayout(
                    viewModel = viewModel,
                    themeMode = themeMode,
                    onThemeChanged = {
                        themeMode = it
                    }
                )
            }
        }
    }
}
