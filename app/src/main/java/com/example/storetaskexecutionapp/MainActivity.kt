package com.example.storetaskexecutionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.storetaskexecutionapp.ui.navigation.StoreTaskNavHost
import com.example.storetaskexecutionapp.ui.theme.StoreTaskExecutionAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StoreTaskExecutionAppTheme {
                StoreTaskNavHost()
            }
        }
    }
}
