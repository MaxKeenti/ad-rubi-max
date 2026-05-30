package com.example.mangos

import android.app.Application
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MangosApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Firebase.firestore.firestoreSettings = firestoreSettings {
            setLocalCacheSettings(persistentCacheSettings { })
        }
        Log.i("MangosApp", "Mangos USA - bootstrap OK")
    }
}
