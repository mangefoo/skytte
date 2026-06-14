package se.mindphaser.skytte.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import se.mindphaser.skytte.data.repo.Repositories
import java.time.LocalDate

private val importJson = Json { ignoreUnknownKeys = true } // tolerate older/newer backup files

sealed interface ImportResult {
    data class Success(val weapons: Int, val ammunition: Int, val sessions: Int) : ImportResult
    data class Error(val message: String) : ImportResult
}

/**
 * Reads a backup JSON from [uri] and merges it into the user's Firestore data. Every entry gets a
 * fresh document id (so an import never clobbers an existing doc with the same id), and session
 * references are remapped onto the newly assigned weapon/ammunition ids. Writes are queued
 * offline-first; this returns once they are enqueued.
 */
suspend fun importBackup(context: Context, uri: Uri, repos: Repositories): ImportResult =
    withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return@withContext ImportResult.Error("kunde inte öppna filen")
            val backup = importJson.decodeFromString<BackupData>(text)

            val weaponIdMap = HashMap<String, String>()
            backup.weapons.forEach { dto ->
                val newId = repos.weapons.newId()
                repos.weapons.save(Weapon(newId, dto.name, dto.caliber, dto.notes))
                weaponIdMap[dto.id] = newId
            }

            val ammoIdMap = HashMap<String, String>()
            backup.ammunition.forEach { dto ->
                val newId = repos.ammunition.newId()
                repos.ammunition.save(Ammunition(newId, dto.name, dto.caliber, dto.notes, dto.costPerRound))
                ammoIdMap[dto.id] = newId
            }

            backup.sessions.forEach { dto ->
                repos.sessions.save(
                    Session(
                        id = repos.sessions.newId(),
                        date = LocalDate.parse(dto.date),
                        location = dto.location,
                        weaponId = dto.weaponId?.let(weaponIdMap::get),
                        ammunitionId = dto.ammunitionId?.let(ammoIdMap::get),
                        ammoCount = dto.ammoCount,
                        shootingType = dto.shootingType,
                        fee = dto.fee,
                        feeIncludesAmmo = dto.feeIncludesAmmo,
                    )
                )
            }

            ImportResult.Success(backup.weapons.size, backup.ammunition.size, backup.sessions.size)
        } catch (e: Exception) {
            ImportResult.Error(e.message ?: "okänt fel")
        }
    }
