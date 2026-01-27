package com.example.oneorder.di

import android.util.Log
import com.example.oneorder.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    // Read from local.properties via BuildConfig - DO NOT HARDCODE SECRETS
    private val SUPABASE_URL = BuildConfig.SUPABASE_URL
    private val SUPABASE_KEY = BuildConfig.SUPABASE_KEY

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        Log.e("SUPABASE_DEBUG", "URL = >>>$SUPABASE_URL<<<")
        Log.e("SUPABASE_DEBUG", "KEY = >>>${SUPABASE_KEY.take(10)}...<<<")

        return createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            install(Auth)
            install(Postgrest)
            
            // Configure JSON serializer to ignore unknown keys from database
            // This prevents errors when database has extra fields that models don't need
            defaultSerializer = KotlinXSerializer(
                json = kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true  // Ignore fields like created_by, is_active, etc.
                    isLenient = true           // Allow for lenient parsing
                    coerceInputValues = true   // Coerce invalid values to defaults
                }
            )
            
            Log.d("SupabaseModule", "Supabase client configured with ignoreUnknownKeys = true")
        }
    }
}
