package com.example.oneorder_sm.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import java.time.Duration
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ResilienceModule {

    @Provides
    @Singleton
    fun provideCircuitBreaker(): CircuitBreaker {
        val config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50.0f) // Float
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(5)
            .slidingWindowSize(20)
            .build()
        return CircuitBreaker.of("supabaseCircuitBreaker", config)
    }

    @Provides
    @Singleton
    fun provideRetry(): Retry {
        val config = RetryConfig.custom<Any>()
            .maxAttempts(3)
            .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofMillis(500), 2.0))
            .build()
        return Retry.of("supabaseRetry", config)
    }
}
