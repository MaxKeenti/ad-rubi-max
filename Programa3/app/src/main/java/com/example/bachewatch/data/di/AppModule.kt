package com.example.bachewatch.data.di

import com.example.bachewatch.data.auth.SesionAnonima
import com.example.bachewatch.data.auth.SesionAnonimaFirebase
import com.example.bachewatch.data.location.FusedLocationProvider
import com.example.bachewatch.data.location.LocationProvider
import com.example.bachewatch.data.repository.ReporteRepository
import com.example.bachewatch.data.repository.firestore.ReporteRepositoryFirestore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Real bindings (tasks 05–06). The Fake* implementations stay compiled for
 * unit tests but are no longer bound — P1's commented-binding pattern, so
 * flipping back to fakes for a desk demo is a one-line change.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindReporteRepository(impl: ReporteRepositoryFirestore): ReporteRepository
    // desk-demo fallback: abstract fun bindReporteRepository(impl: FakeReporteRepository): ReporteRepository

    @Binds
    @Singleton
    abstract fun bindLocationProvider(impl: FusedLocationProvider): LocationProvider
    // desk-demo fallback: abstract fun bindLocationProvider(impl: FakeLocationProvider): LocationProvider

    @Binds
    @Singleton
    abstract fun bindSesionAnonima(impl: SesionAnonimaFirebase): SesionAnonima
    // desk-demo fallback: abstract fun bindSesionAnonima(impl: FakeSesionAnonima): SesionAnonima
}
