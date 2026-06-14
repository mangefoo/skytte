package se.mindphaser.skytte

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import se.mindphaser.skytte.auth.AuthManager
import se.mindphaser.skytte.data.ThemePreferences
import se.mindphaser.skytte.data.migration.LegacyDbMigration
import se.mindphaser.skytte.data.repo.Repositories

class SkytteApp : Application() {

    /** Firestore is the source of truth. Offline persistence is enabled by default on Android. */
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    lateinit var authManager: AuthManager
        private set

    lateinit var themePreferences: ThemePreferences
        private set

    // Repositories are bound to the signed-in user; null when signed out. Rebuilt on every uid change.
    private val _repositories = MutableStateFlow<Repositories?>(null)
    val repositories: StateFlow<Repositories?> = _repositories

    /** Synchronous accessor for the ViewModel factory (ViewModels only exist behind the auth gate). */
    val currentRepositories: Repositories? get() = _repositories.value

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        themePreferences = ThemePreferences(this)
        authManager = AuthManager(this)

        appScope.launch {
            authManager.uid.collect { uid ->
                if (uid == null) {
                    _repositories.value = null
                } else {
                    val repos = Repositories(firestore, uid)
                    _repositories.value = repos
                    // One-time import of any pre-cloud local data into this user's Firestore subtree.
                    LegacyDbMigration.migrateIfNeeded(this@SkytteApp, repos, uid)
                }
            }
        }
    }
}
