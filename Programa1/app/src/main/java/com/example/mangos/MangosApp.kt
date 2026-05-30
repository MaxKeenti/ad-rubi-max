package com.example.mangos

import android.app.Application
import android.util.Log
import com.example.mangos.data.repository.SupplierRepository
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class MangosApp : Application() {

    @Inject lateinit var supplierRepository: SupplierRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Firebase.firestore.firestoreSettings = firestoreSettings {
            setLocalCacheSettings(persistentCacheSettings { })
        }
        appScope.launch {
            runCatching { supplierRepository.ensureUnregisteredExists() }
                .onFailure { Log.w("MangosApp", "ensureUnregisteredExists falló", it) }
        }
        Log.i("MangosApp", "Mangos USA - bootstrap OK")
    }
}
