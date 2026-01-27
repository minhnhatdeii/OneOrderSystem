package com.example.oneorder

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OneOrderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("OneOrderApp", "Application started")
        Log.d("OneOrderApp", "SUPABASE_URL: ${BuildConfig.SUPABASE_URL}")
        Log.d("OneOrderApp", "SUPABASE_KEY length: ${BuildConfig.SUPABASE_KEY.length}")
    }
}
