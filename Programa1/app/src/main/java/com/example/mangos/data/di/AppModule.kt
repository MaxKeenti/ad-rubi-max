package com.example.mangos.data.di

import com.example.mangos.data.repository.AuthRepository
import com.example.mangos.data.repository.PurchaseRepository
import com.example.mangos.data.repository.SupplierRepository
import com.example.mangos.data.repository.fake.FakePurchaseRepository
import com.example.mangos.data.repository.firestore.AuthRepositoryFirestoreImpl
import com.example.mangos.data.repository.firestore.SupplierRepositoryFirestoreImpl
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryFirestoreImpl): AuthRepository

    // Fake kept in the repo for now; binding disabled in favor of the real impl above.
    // @Binds
    // @Singleton
    // abstract fun bindAuthRepository(impl: FakeAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSupplierRepository(impl: SupplierRepositoryFirestoreImpl): SupplierRepository

    // Fake kept in the repo for now; binding disabled in favor of the real impl above.
    // @Binds
    // @Singleton
    // abstract fun bindSupplierRepository(impl: FakeSupplierRepository): SupplierRepository

    @Binds
    @Singleton
    abstract fun bindPurchaseRepository(impl: FakePurchaseRepository): PurchaseRepository

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

        @Provides
        @Singleton
        fun provideFirestore(): FirebaseFirestore = Firebase.firestore
    }
}
