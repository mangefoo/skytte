package se.mindphaser.skytte.data.migration

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import se.mindphaser.skytte.data.Ammunition
import se.mindphaser.skytte.data.Session
import se.mindphaser.skytte.data.Weapon
import se.mindphaser.skytte.data.repo.Repositories
import java.time.LocalDate

/**
 * One-time import of pre-cloud local data into the signed-in user's Firestore subtree.
 *
 * Older installs stored everything in a Room/SQLite database (`skytte.db`) keyed by Long ids. We
 * read that file directly (read-only) once per user, assign fresh Firestore document ids, and remap
 * each session's weapon/ammunition references to the new ids. Guarded by a per-uid SharedPreferences
 * flag so it never runs twice. Firestore writes are queued offline-first and sync in the background.
 */
object LegacyDbMigration {

    suspend fun migrateIfNeeded(context: Context, repos: Repositories, uid: String) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val doneKey = "legacy_migrated_$uid"
        if (prefs.getBoolean(doneKey, false)) return

        val dbFile = context.getDatabasePath("skytte.db")
        if (!dbFile.exists()) {
            prefs.edit().putBoolean(doneKey, true).apply()
            return
        }

        runCatching {
            withContext(Dispatchers.IO) {
                SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                    val weaponIdMap = HashMap<Long, String>()
                    readWeapons(db).forEach { (oldId, weapon) ->
                        val newId = repos.weapons.newId()
                        repos.weapons.save(weapon.copy(id = newId))
                        weaponIdMap[oldId] = newId
                    }

                    val ammoIdMap = HashMap<Long, String>()
                    readAmmunition(db).forEach { (oldId, ammo) ->
                        val newId = repos.ammunition.newId()
                        repos.ammunition.save(ammo.copy(id = newId))
                        ammoIdMap[oldId] = newId
                    }

                    readSessions(db).forEach { row ->
                        repos.sessions.save(
                            Session(
                                id = repos.sessions.newId(),
                                date = row.date,
                                location = row.location,
                                weaponId = row.oldWeaponId?.let(weaponIdMap::get),
                                ammunitionId = row.oldAmmoId?.let(ammoIdMap::get),
                                ammoCount = row.ammoCount,
                                shootingType = row.shootingType,
                                fee = row.fee,
                                feeIncludesAmmo = row.feeIncludesAmmo,
                            )
                        )
                    }
                }
            }
        }
        // Mark done regardless: a partial import is preferable to looping; the JSON backup remains
        // a manual fallback for anything that didn't come across.
        prefs.edit().putBoolean(doneKey, true).apply()
    }

    private fun readWeapons(db: SQLiteDatabase): List<Pair<Long, Weapon>> =
        db.rawQuery("SELECT id, name, caliber, notes FROM weapons", null).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        c.getLong(0) to Weapon(
                            name = c.getString(1) ?: "",
                            caliber = c.getString(2) ?: "",
                            notes = c.getString(3) ?: "",
                        )
                    )
                }
            }
        }

    private fun readAmmunition(db: SQLiteDatabase): List<Pair<Long, Ammunition>> =
        db.rawQuery("SELECT id, name, caliber, notes, costPerRound FROM ammunition", null).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        c.getLong(0) to Ammunition(
                            name = c.getString(1) ?: "",
                            caliber = c.getString(2) ?: "",
                            notes = c.getString(3) ?: "",
                            costPerRound = if (c.isNull(4)) null else c.getDouble(4),
                        )
                    )
                }
            }
        }

    private fun readSessions(db: SQLiteDatabase): List<LegacySessionRow> =
        db.rawQuery(
            "SELECT date, location, weaponId, ammunitionId, ammoCount, shootingType, fee, feeIncludesAmmo FROM sessions",
            null
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        LegacySessionRow(
                            date = LocalDate.ofEpochDay(c.getLong(0)),
                            location = c.getString(1) ?: "",
                            oldWeaponId = if (c.isNull(2)) null else c.getLong(2),
                            oldAmmoId = if (c.isNull(3)) null else c.getLong(3),
                            ammoCount = c.getInt(4),
                            shootingType = c.getString(5) ?: "",
                            fee = if (c.isNull(6)) null else c.getDouble(6),
                            feeIncludesAmmo = c.getInt(7) != 0,
                        )
                    )
                }
            }
        }

    private data class LegacySessionRow(
        val date: LocalDate,
        val location: String,
        val oldWeaponId: Long?,
        val oldAmmoId: Long?,
        val ammoCount: Int,
        val shootingType: String,
        val fee: Double?,
        val feeIncludesAmmo: Boolean,
    )
}
