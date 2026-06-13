package com.example.bachewatch.data.di

import com.example.bachewatch.data.auth.FakeSesionAnonima
import com.example.bachewatch.data.auth.SesionAnonima
import com.example.bachewatch.data.location.FakeLocationProvider
import com.example.bachewatch.data.location.LocationProvider
import com.example.bachewatch.data.repository.ReporteRepository
import com.example.bachewatch.data.repository.fake.FakeReporteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Fakes bound while the app runs without Firebase (Day-1 checkpoint).
 * Real bindings flip in task 05 (location) and task 06 (repository +
 * sesión) — P1's commented-binding pattern.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindReporteRepository(impl: FakeReporteRepository): ReporteRepository
    // task 06: abstract fun bindReporteRepository(impl: ReporteRepositoryFirestore): ReporteRepository

    @Binds
    @Singleton
    abstract fun bindLocationProvider(impl: FakeLocationProvider): LocationProvider
    // task 05: abstract fun bindLocationProvider(impl: FusedLocationProvider): LocationProvider

    @Binds
    @Singleton
    abstract fun bindSesionAnonima(impl: FakeSesionAnonima): SesionAnonima
    // task 06: abstract fun bindSesionAnonima(impl: SesionAnonimaFirebase): SesionAnonima
}
