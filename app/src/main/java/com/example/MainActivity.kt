package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.core.configureAppCheck
import com.example.ui.CityLoopApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must run before setContent, ViewModels, or any Firebase operations
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            CityLoopApp()
        }
    }
}
