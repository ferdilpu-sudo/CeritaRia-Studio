package com.flyonz.ceritaria.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.flyonz.ceritaria.studio.app.CeritariaStudioApp
import com.flyonz.ceritaria.studio.core.designsystem.theme.CeritariaStudioTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CeritariaStudioTheme {
                CeritariaStudioApp()
            }
        }
    }
}
