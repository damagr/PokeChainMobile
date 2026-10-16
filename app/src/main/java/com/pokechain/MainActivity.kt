package com.pokechain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pokechain.data.scanner.ScannerState
import com.pokechain.ui.MainScreen
import com.pokechain.ui.theme.PokeChainTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PokeChainTheme {
                MainScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // El chip flotante se oculta dentro de PokeChain (solo aparece sobre PoGo)
        ScannerState.appInForeground.value = true
    }

    override fun onPause() {
        super.onPause()
        ScannerState.appInForeground.value = false
    }
}