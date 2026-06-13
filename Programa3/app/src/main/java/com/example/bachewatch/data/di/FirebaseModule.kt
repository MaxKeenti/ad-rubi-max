package com.example.bachewatch.data.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The Firebase SDK singletons. Separate from [AppModule] because these are
 * `@Provides` factories (framework types we don't own), not `@Binds`.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun firestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun auth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun storage(): FirebaseStorage = FirebaseStorage.getInstance()
}
