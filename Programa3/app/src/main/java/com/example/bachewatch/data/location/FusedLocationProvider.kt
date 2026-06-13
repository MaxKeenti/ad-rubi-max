package com.example.bachewatch.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.example.bachewatch.data.model.LocationFix
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Single-shot high-accuracy fix per the Q5 policy: fine-or-nothing, no
 * continuous updates, no background location. `lastLocation` is never the
 * primary read (stale fixes lie about accuracy) — only a fallback when it
 * is fresher than 30 s.
 */
@Singleton
class FusedLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationProvider {

    private val cliente: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission") // permission checked first; SinPermiso otherwise
    override suspend fun fixActual(): Result<LocationFix> {
        if (!tienePermisoFino()) return Result.failure(LocationError.SinPermiso)

        val cancelacion = CancellationTokenSource()
        return try {
            val live = withTimeout(TIMEOUT_MS) {
                cliente.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancelacion.token,
                ).await()
            }
            // getCurrentLocation can resolve null (location off / undeterminable).
            val location = live ?: lastLocationReciente()
            if (location == null) {
                Result.failure(LocationError.FixNoDisponible)
            } else {
                Result.success(location.toFix())
            }
        } catch (_: TimeoutCancellationException) {
            cancelacion.cancel()
            // A fresh-enough lastLocation beats nothing when the live fix stalls.
            lastLocationReciente()?.let { return Result.success(it.toFix()) }
            Result.failure(LocationError.FixTimeout)
        }
    }

    @SuppressLint("MissingPermission") // guarded by tienePermisoFino() in fixActual
    private suspend fun lastLocationReciente(): Location? {
        val ultima = runCatching { cliente.lastLocation.await() }.getOrNull() ?: return null
        val edadMs = (SystemClock.elapsedRealtimeNanos() - ultima.elapsedRealtimeNanos) / 1_000_000
        return ultima.takeIf { edadMs in 0..MAX_EDAD_FALLBACK_MS }
    }

    private fun tienePermisoFino(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    private fun Location.toFix() = LocationFix(
        lat = latitude,
        lng = longitude,
        // Unknown accuracy trips the soft gate (warn), never silently passes.
        accuracyMeters = if (hasAccuracy()) accuracy.toDouble() else Double.MAX_VALUE,
    )

    private companion object {
        const val TIMEOUT_MS = 15_000L
        const val MAX_EDAD_FALLBACK_MS = 30_000L
    }
}
