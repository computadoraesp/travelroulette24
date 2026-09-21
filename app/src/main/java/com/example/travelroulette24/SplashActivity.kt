package com.example.travelroulette24

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.example.travelroulette24.ui.theme.TravelRoulette24Theme

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TravelRoulette24Theme {
                SplashScreen {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
        }
    }
}

@Composable
private fun SplashScreen(onFinished: () -> Unit) {
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        factory = { context ->
            VideoView(context).apply {
                setVideoURI(Uri.parse("android.resource://${context.packageName}/raw/splash"))
                setOnCompletionListener { onFinished() }
                setOnErrorListener { _, _, _ -> onFinished(); true }
                start()
            }
        },
        update = { it.start() },
        onRelease = { it.stopPlayback() }
    )
}