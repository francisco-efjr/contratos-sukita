package com.sukita.contratos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sukita.contratos.navigation.NavGraph
import com.sukita.contratos.ui.theme.ContratosSukitaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ContratosSukitaTheme {
                NavGraph()
            }
        }
    }
}
