package com.example.mangos

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MangosApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("MangosApp", "Mangos USA - bootstrap OK")
    }
}
