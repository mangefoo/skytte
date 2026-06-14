package se.mindphaser.skytte

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

    /**
     * True when the signed-in account is rejected by the Firestore security rules (the single source
     * of truth for who's allowed). Drives the "authorization failed" screen — there is no allowlist
     * in the app; we simply react to what Firestore tells us.
     */
    private val _accessDenied = MutableStateFlow(false)
    val accessDenied: StateFlow<Boolean> = _accessDenied

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
                    _accessDenied.value = false
                } else if (isAccessDenied(uid)) {
                    _accessDenied.value = true
                    _repositories.value = null
                } else {
                    _accessDenied.value = false
                    val repos = Repositories(firestore, uid)
                    _repositories.value = repos
                    // One-time import of any pre-cloud local data into this user's Firestore subtree.
                    LegacyDbMigration.migrateIfNeeded(this@SkytteApp, repos, uid)
                }
            }
        }
    }

    /**
     * Probes whether the security rules reject this account, by attempting one read of the user's
     * own document. A PERMISSION_DENIED means "not allowed". Any other failure (e.g. offline) is
     * treated as not-denied so we stay optimistic and let the offline cache serve data.
     */
    private suspend fun isAccessDenied(uid: String): Boolean = try {
        firestore.collection("users").document(uid).get().await()
        false
    } catch (e: FirebaseFirestoreException) {
        e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
    } catch (e: Exception) {
        false
    }
}
