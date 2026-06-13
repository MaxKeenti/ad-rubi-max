package com.example.bachewatch.data.location

import com.example.bachewatch.data.model.LocationFix
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * Jittered fix near UPIICSA after a realistic delay. Accuracy spans
 * both sides of the soft gate (8–30 m) so the warning UI is visible
 * in demos without leaving the desk.
 */
@Singleton
class FakeLocationProvider @Inject constructor() : LocationProvider {

    override suspend fun fixActual(): Result<LocationFix> {
        delay(1_500)
        return Result.success(
            LocationFix(
                lat = 19.3953 + Random.nextDouble(-0.001, 0.001),
                lng = -99.0921 + Random.nextDouble(-0.001, 0.001),
                accuracyMeters = Random.nextDouble(8.0, 30.0),
            )
        )
    }
}
