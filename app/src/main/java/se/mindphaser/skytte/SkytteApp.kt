package se.mindphaser.skytte

import android.app.Application
import se.mindphaser.skytte.data.AppDatabase
import se.mindphaser.skytte.data.ThemePreferences

class SkytteApp : Application() {
    lateinit var database: AppDatabase
        private set

    lateinit var themePreferences: ThemePreferences
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.get(this)
        themePreferences = ThemePreferences(this)
    }
}
