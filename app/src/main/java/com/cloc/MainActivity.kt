package com.cloc

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cloc.ui.theme.ND
import com.cloc.ui.theme.NothingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        setContent {
            NothingTheme {
                Column(
                    Modifier.fillMaxSize().background(ND.colors.black),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("12:00", style = ND.type.numberXl, color = ND.colors.textDisplay)
                    Spacer(Modifier.height(ND.space.sm))
                    Text("CLOCK", style = ND.type.label, color = ND.colors.textSecondary)
                }
            }
        }
    }
}

