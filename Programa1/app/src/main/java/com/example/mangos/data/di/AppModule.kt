package com.example.mangos.data.di

import com.example.mangos.data.repository.AuthRepository
import com.example.mangos.data.repository.PurchaseRepository
import com.example.mangos.data.repository.SupplierRepository
import com.example.mangos.data.repository.fake.FakeAuthRepository
import com.example.mangos.data.repository.fake.FakePurchaseRepository
import com.example.mangos.data.repository.fake.FakeSupplierRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FakeAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSupplierRepository(impl: FakeSupplierRepository): SupplierRepository

    @Binds
    @Singleton
    abstract fun bindPurchaseRepository(impl: FakePurchaseRepository): PurchaseRepository
}
