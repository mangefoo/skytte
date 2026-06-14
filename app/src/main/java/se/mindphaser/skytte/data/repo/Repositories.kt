package se.mindphaser.skytte.data.repo

import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import se.mindphaser.skytte.data.Ammunition
import se.mindphaser.skytte.data.Session
import se.mindphaser.skytte.data.SessionWithRefs
import se.mindphaser.skytte.data.Weapon
import java.time.LocalDate

/**
 * Holds the per-user repositories. Built from the signed-in [uid]; all data lives under
 * `/users/{uid}/…` in Firestore. Rebuilt whenever the signed-in user changes (see SkytteApp).
 */
class Repositories(firestore: FirebaseFirestore, uid: String) {
    private val userDoc = firestore.collection("users").document(uid)
    val weapons = WeaponRepository(userDoc.collection("weapons"))
    val ammunition = AmmunitionRepository(userDoc.collection("ammunition"))
    val sessions = SessionRepository(userDoc.collection("sessions"), weapons, ammunition)
}

/**
 * Emits the collection's documents as a list and re-emits on every change. Backed by a Firestore
 * snapshot listener, so it serves the offline cache immediately and syncs in the background.
 */
private fun <T> CollectionReference.observe(map: (DocumentSnapshot) -> T?): Flow<List<T>> =
    callbackFlow {
        val registration = addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Don't crash the app on a listener error (e.g. PERMISSION_DENIED, transient
                // rejections). Log it and complete the flow normally; collectors keep their last value.
                Log.w("Repositories", "Snapshot listener error for ${this@observe.path}", error)
                close()
                return@addSnapshotListener
            }
            if (snapshot != null) trySend(snapshot.documents.mapNotNull(map))
        }
        awaitClose { registration.remove() }
    }

class WeaponRepository(private val col: CollectionReference) {
    /** A fresh document id, used when persisting a new entity or remapping ids during import. */
    fun newId(): String = col.document().id

    fun observeAll(): Flow<List<Weapon>> =
        col.observe { it.toWeapon() }.map { list -> list.sortedBy { it.name.lowercase() } }

    suspend fun getAll(): List<Weapon> =
        col.get().await().documents.mapNotNull { it.toWeapon() }.sortedBy { it.name.lowercase() }

    // Writes are fire-and-forget: Firestore updates the local cache immediately and the returned
    // Task only completes on server ack, so awaiting it would hang while offline. Sync happens in
    // the background; observeAll() reflects the change at once via the cache.
    fun save(weapon: Weapon) {
        val id = weapon.id.ifBlank { newId() }
        col.document(id).set(weapon.toMap())
    }

    fun delete(weapon: Weapon) {
        if (weapon.id.isNotBlank()) col.document(weapon.id).delete()
    }
}

class AmmunitionRepository(private val col: CollectionReference) {
    fun newId(): String = col.document().id

    fun observeAll(): Flow<List<Ammunition>> =
        col.observe { it.toAmmunition() }.map { list -> list.sortedBy { it.name.lowercase() } }

    suspend fun getAll(): List<Ammunition> =
        col.get().await().documents.mapNotNull { it.toAmmunition() }.sortedBy { it.name.lowercase() }

    fun save(ammo: Ammunition) {
        val id = ammo.id.ifBlank { newId() }
        col.document(id).set(ammo.toMap())
    }

    fun delete(ammo: Ammunition) {
        if (ammo.id.isNotBlank()) col.document(ammo.id).delete()
    }
}

class SessionRepository(
    private val col: CollectionReference,
    private val weaponRepo: WeaponRepository,
    private val ammunitionRepo: AmmunitionRepository,
) {
    fun newId(): String = col.document().id

    private fun observeSessions(): Flow<List<Session>> =
        col.observe { it.toSession() }
            .map { list ->
                list.sortedWith(
                    compareByDescending<Session> { it.date }.thenByDescending { it.id }
                )
            }

    /**
     * Sessions joined with their weapon/ammunition. Replaces Room's `@Relation`: the join is done
     * in-memory by combining the three live collections. A reference to a deleted weapon/ammo
     * resolves to null (matching the old SET_NULL behavior).
     */
    fun observeAll(): Flow<List<SessionWithRefs>> =
        combine(
            observeSessions(),
            weaponRepo.observeAll(),
            ammunitionRepo.observeAll()
        ) { sessions, weapons, ammo ->
            val weaponsById = weapons.associateBy { it.id }
            val ammoById = ammo.associateBy { it.id }
            sessions.map { s ->
                SessionWithRefs(s, s.weaponId?.let(weaponsById::get), s.ammunitionId?.let(ammoById::get))
            }
        }

    suspend fun getSession(id: String): Session? =
        col.document(id).get().await().takeIf { it.exists() }?.toSession()

    suspend fun getAll(): List<Session> =
        col.get().await().documents.mapNotNull { it.toSession() }

    fun save(session: Session) {
        val id = session.id.ifBlank { newId() }
        col.document(id).set(session.toMap())
    }

    fun delete(session: Session) {
        if (session.id.isNotBlank()) col.document(session.id).delete()
    }
}

// --- Firestore <-> domain mapping -------------------------------------------------------------
// LocalDate is stored as an epoch-day Long (timezone-free, sorts/filters trivially). A future web
// client must agree on this representation.

private fun Weapon.toMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "caliber" to caliber,
    "notes" to notes,
)

private fun DocumentSnapshot.toWeapon(): Weapon? = Weapon(
    id = id,
    name = getString("name") ?: "",
    caliber = getString("caliber") ?: "",
    notes = getString("notes") ?: "",
)

private fun Ammunition.toMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "caliber" to caliber,
    "notes" to notes,
    "costPerRound" to costPerRound,
)

private fun DocumentSnapshot.toAmmunition(): Ammunition? = Ammunition(
    id = id,
    name = getString("name") ?: "",
    caliber = getString("caliber") ?: "",
    notes = getString("notes") ?: "",
    costPerRound = getDouble("costPerRound"),
)

private fun Session.toMap(): Map<String, Any?> = mapOf(
    "date" to date.toEpochDay(),
    "location" to location,
    "weaponId" to weaponId,
    "ammunitionId" to ammunitionId,
    "ammoCount" to ammoCount,
    "shootingType" to shootingType,
    "fee" to fee,
    "feeIncludesAmmo" to feeIncludesAmmo,
)

private fun DocumentSnapshot.toSession(): Session? {
    val epochDay = getLong("date") ?: return null
    return Session(
        id = id,
        date = LocalDate.ofEpochDay(epochDay),
        location = getString("location") ?: "",
        weaponId = getString("weaponId"),
        ammunitionId = getString("ammunitionId"),
        ammoCount = (getLong("ammoCount") ?: 0L).toInt(),
        shootingType = getString("shootingType") ?: "",
        fee = getDouble("fee"),
        feeIncludesAmmo = getBoolean("feeIncludesAmmo") ?: false,
    )
}
