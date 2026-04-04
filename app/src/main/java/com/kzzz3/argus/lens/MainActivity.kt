package com.kzzz3.argus.lens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kzzz3.argus.lens.app.ArgusLensApp
import com.kzzz3.argus.lens.ui.theme.ArguslensTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArguslensTheme {
                ArgusLensApp()
            }
        }
    }
}
