package com.example.nanosonicproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.nanosonicproject.ui.theme.NanoSonicProjectTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint  // ← YES! This one needs the annotation
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NanoSonicProjectTheme {
                NanoSonicApp()  // ← Calls the navigation composable
            }
        }
    }
}