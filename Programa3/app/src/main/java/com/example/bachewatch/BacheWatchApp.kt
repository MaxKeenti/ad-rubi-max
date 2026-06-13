package com.example.bachewatch

import android.app.Application
import com.example.bachewatch.data.auth.SesionAnonima
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class BacheWatchApp : Application() {

    @Inject lateinit var sesion: SesionAnonima

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Silent anonymous sign-in (ADR-0001): rules require a signed-in user
        // even for the read path, so warm it up before the first screen queries.
        scope.launch { sesion.ensureSignedIn() }
    }
}
