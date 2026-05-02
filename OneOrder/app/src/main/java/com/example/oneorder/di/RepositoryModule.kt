package com.example.oneorder.di

import com.example.oneorder.data.repository.AuthRepository
import com.example.oneorder.data.repository.AuthRepositoryImpl
import com.example.oneorder.data.repository.FollowingRepository
import com.example.oneorder.data.repository.FollowingRepositoryImpl
import com.example.oneorder.data.repository.RestaurantProfileRepository
import com.example.oneorder.data.repository.RestaurantProfileRepositoryImpl
import com.example.oneorder.data.repository.RestaurantRepository
import com.example.oneorder.data.repository.RestaurantRepositoryImpl
import com.example.oneorder.data.repository.TableRepository
import com.example.oneorder.data.repository.TableRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindMenuRepository(
        menuRepositoryImpl: com.example.oneorder.data.repository.MenuRepositoryImpl
    ): com.example.oneorder.data.repository.MenuRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(
        orderRepositoryImpl: com.example.oneorder.data.repository.OrderRepositoryImpl
    ): com.example.oneorder.data.repository.OrderRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        profileRepositoryImpl: com.example.oneorder.data.repository.ProfileRepositoryImpl
    ): com.example.oneorder.data.repository.ProfileRepository

    @Binds
    @Singleton
    abstract fun bindRestaurantRepository(
        restaurantRepositoryImpl: RestaurantRepositoryImpl
    ): RestaurantRepository

    @Binds
    @Singleton
    abstract fun bindTableRepository(
        tableRepositoryImpl: TableRepositoryImpl
    ): TableRepository

    @Binds
    @Singleton
    abstract fun bindRestaurantProfileRepository(
        restaurantProfileRepositoryImpl: RestaurantProfileRepositoryImpl
    ): RestaurantProfileRepository

    @Binds
    @Singleton
    abstract fun bindFoodFeedRepository(
        foodFeedRepositoryImpl: com.example.oneorder.data.repository.FoodFeedRepositoryImpl
    ): com.example.oneorder.data.repository.FoodFeedRepository

    @Binds
    @Singleton
    abstract fun bindFollowingRepository(
        followingRepositoryImpl: FollowingRepositoryImpl
    ): FollowingRepository
}
