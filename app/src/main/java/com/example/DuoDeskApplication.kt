package com.example

import android.app.Application
import com.example.data.AppPreferences
import com.example.data.DuoDeskDatabase
import com.example.data.DuoDeskRepository

class DuoDeskApplication : Application() {
    val database: DuoDeskDatabase by lazy { DuoDeskDatabase.getInstance(this) }
    val repository: DuoDeskRepository by lazy {
        DuoDeskRepository(database.duoDeskDao(), AppPreferences(this))
    }
}
