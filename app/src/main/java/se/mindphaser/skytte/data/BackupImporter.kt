package se.mindphaser.skytte.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import se.mindphaser.skytte.data.repo.Repositories
import java.time.LocalDate

private val importJson = Json { ignoreUnknownKeys = true; isLenient = true }

sealed interface ImportResult {
    data class Success(val weapons: Int, val ammunition: Int, val sessions: Int) : ImportResult
    data class Error(val message: String) : ImportResult
}

/**
 * Reads a backup JSON from [uri] and merges it into the user's Firestore data.
 *
 * The backup entry's own id is reused as the Firestore document id, so the import is an **upsert by
 * id and therefore idempotent** — importing the same file twice overwrites the same documents
 * instead of duplicating them (matching the old Room REPLACE behavior). Because ids are preserved,
 * session references (`weaponId`/`ammunitionId`) carry over unchanged with no remapping.
 *
 * Ids are read as raw strings via [idOf], so this tolerates **both** older v2 backups (numeric Long
 * ids) and current v3 backups (string ids). Writes are queued offline-first; this returns once they
 * are enqueued. Nothing is deleted (entries not in the file are left untouched).
 */
suspend fun importBackup(context: Context, uri: Uri, repos: Repositories): ImportResult =
    withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return@withContext ImportResult.Error("kunde inte öppna filen")
            val root = importJson.parseToJsonElement(text).jsonObject

            val weapons = root["weapons"]?.jsonArray.orEmptyObjects()
            val ammunition = root["ammunition"]?.jsonArray.orEmptyObjects()
            val sessions = root["sessions"]?.jsonArray.orEmptyObjects()

            weapons.forEach { w ->
                repos.weapons.save(
                    Weapon(
                        id = w.idOf("id") ?: repos.weapons.newId(),
                        name = w.str("name"),
                        caliber = w.str("caliber"),
                        notes = w.str("notes"),
                    )
                )
            }

            ammunition.forEach { a ->
                repos.ammunition.save(
                    Ammunition(
                        id = a.idOf("id") ?: repos.ammunition.newId(),
                        name = a.str("name"),
                        caliber = a.str("caliber"),
                        notes = a.str("notes"),
                        costPerRound = a["costPerRound"]?.jsonPrimitive?.doubleOrNull,
                    )
                )
            }

            sessions.forEach { s ->
                val dateText = s.idOf("date") ?: return@forEach
                repos.sessions.save(
                    Session(
                        id = s.idOf("id") ?: repos.sessions.newId(),
                        date = LocalDate.parse(dateText),
                        location = s.str("location"),
                        weaponId = s.idOf("weaponId"),
                        ammunitionId = s.idOf("ammunitionId"),
                        ammoCount = s["ammoCount"]?.jsonPrimitive?.intOrNull ?: 0,
                        shootingType = s.str("shootingType"),
                        fee = s["fee"]?.jsonPrimitive?.doubleOrNull,
                        feeIncludesAmmo = s["feeIncludesAmmo"]?.jsonPrimitive?.booleanOrNull ?: false,
                    )
                )
            }

            ImportResult.Success(weapons.size, ammunition.size, sessions.size)
        } catch (e: Exception) {
            ImportResult.Error(e.message ?: "okänt fel")
        }
    }

private fun JsonArray?.orEmptyObjects(): List<JsonObject> = this?.map { it.jsonObject } ?: emptyList()

/** Reads a primitive field as its raw string content (handles both numeric and quoted values). */
private fun JsonObject.idOf(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

private fun JsonObject.str(key: String): String = this[key]?.jsonPrimitive?.contentOrNull ?: ""
