package se.mindphaser.skytte

import android.app.Application
import se.mindphaser.skytte.data.AppDatabase

class SkytteApp : Application() {
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.get(this)
    }
}
