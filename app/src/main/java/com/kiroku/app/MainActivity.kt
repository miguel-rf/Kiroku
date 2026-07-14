package com.kiroku.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kiroku.app.core.designsystem.KirokuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as KirokuApplication).appContainer
        setContent {
            KirokuTheme {
                KirokuApp(
                    container = container,
                    onExit = ::finish,
                )
            }
        }
    }
}
