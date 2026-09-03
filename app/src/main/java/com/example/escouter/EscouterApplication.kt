package com.example.escouter

import android.app.Application
import com.cloudinary.android.MediaManager

class EscouterApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val config = hashMapOf(
            "cloud_name" to "tnlkhwmr",
            "secure" to true
        )

        MediaManager.init(this, config)
    }
}