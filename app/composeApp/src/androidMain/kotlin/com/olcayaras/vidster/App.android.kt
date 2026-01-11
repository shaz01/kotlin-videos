package com.olcayaras.vidster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.olcayaras.vidster.di.initKoin
import com.arkivanov.decompose.defaultComponentContext
import com.olcayaras.vidster.ui.App
import com.olcayaras.vidster.util.ActivityHolder

class AppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initKoin()

        ActivityHolder.setActivity(this)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent {
            App(componentContext = defaultComponentContext())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityHolder.clearActivity()
    }
}

@Preview
@Composable
fun AppPreview() {
    App()
}
