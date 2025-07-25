package com.rjhtctn.hacostagram.view

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.rjhtctn.hacostagram.BuildConfig
import com.cloudinary.android.MediaManager
import com.rjhtctn.hacostagram.R
import androidx.core.graphics.drawable.toDrawable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : AppCompatActivity() {
    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installSplashScreen()
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val config = hashMapOf("cloud_name" to BuildConfig.CLOUD_NAME)
        MediaManager.init(this, config)

        val window = this.window
        val surface = ContextCompat.getColor(this, R.color.colorSurface)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) { // API 35 adı
            @Suppress("DEPRECATION")
            window.statusBarColor     = surface
            @Suppress("DEPRECATION")
            window.navigationBarColor = surface
        } else {
            window.setBackgroundDrawable(surface.toDrawable())
        }

    }

}