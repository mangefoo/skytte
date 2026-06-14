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
 * read that file directly (read-only) once per user and write each row to Firestore, **reusing the
 * old Long id (as a string) as the document id**. Keeping the ids stable means session references
 * carry over unchanged, and it matches the id-preserving scheme used by the JSON importer — so
 * migrating and later restoring a backup of the same data are both idempotent (no duplicates).
 *
 * Guarded by a per-uid SharedPreferences flag so it never runs twice. Firestore writes are queued
 * offline-first and sync in the background.
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
                    readWeapons(db).forEach { repos.weapons.save(it) }
                    readAmmunition(db).forEach { repos.ammunition.save(it) }
                    readSessions(db).forEach { repos.sessions.save(it) }
                }
            }
        }
        // Mark done regardless: a partial import is preferable to looping; the JSON backup remains
        // a manual fallback for anything that didn't come across.
        prefs.edit().putBoolean(doneKey, true).apply()
    }

    private fun readWeapons(db: SQLiteDatabase): List<Weapon> =
        db.rawQuery("SELECT id, name, caliber, notes FROM weapons", null).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        Weapon(
                            id = c.getLong(0).toString(),
                            name = c.getString(1) ?: "",
                            caliber = c.getString(2) ?: "",
                            notes = c.getString(3) ?: "",
                        )
                    )
                }
            }
        }

    private fun readAmmunition(db: SQLiteDatabase): List<Ammunition> =
        db.rawQuery("SELECT id, name, caliber, notes, costPerRound FROM ammunition", null).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        Ammunition(
                            id = c.getLong(0).toString(),
                            name = c.getString(1) ?: "",
                            caliber = c.getString(2) ?: "",
                            notes = c.getString(3) ?: "",
                            costPerRound = if (c.isNull(4)) null else c.getDouble(4),
                        )
                    )
                }
            }
        }

    private fun readSessions(db: SQLiteDatabase): List<Session> =
        db.rawQuery(
            "SELECT id, date, location, weaponId, ammunitionId, ammoCount, shootingType, fee, feeIncludesAmmo FROM sessions",
            null
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        Session(
                            id = c.getLong(0).toString(),
                            date = LocalDate.ofEpochDay(c.getLong(1)),
                            location = c.getString(2) ?: "",
                            weaponId = if (c.isNull(3)) null else c.getLong(3).toString(),
                            ammunitionId = if (c.isNull(4)) null else c.getLong(4).toString(),
                            ammoCount = c.getInt(5),
                            shootingType = c.getString(6) ?: "",
                            fee = if (c.isNull(7)) null else c.getDouble(7),
                            feeIncludesAmmo = c.getInt(8) != 0,
                        )
                    )
                }
            }
        }
}
