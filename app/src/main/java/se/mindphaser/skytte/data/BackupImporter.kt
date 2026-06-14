package se.mindphaser.skytte.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private val importJson = Json { ignoreUnknownKeys = true } // tolerate older/newer backup files

sealed interface ImportResult {
    data class Success(val weapons: Int, val ammunition: Int, val sessions: Int) : ImportResult
    data class Error(val message: String) : ImportResult
}

/**
 * Reads a backup JSON from [uri] and merges it into the database: each entry is upserted by id
 * (existing rows with the same id are replaced), and any existing rows not present in the file are
 * left untouched. Weapons/ammunition are inserted before sessions so session foreign keys hold.
 */
suspend fun importBackup(context: Context, uri: Uri): ImportResult = withContext(Dispatchers.IO) {
    try {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: return@withContext ImportResult.Error("kunde inte öppna filen")
        val backup = importJson.decodeFromString<BackupData>(text)
        val db = AppDatabase.get(context)
        db.withTransaction {
            backup.weapons.forEach { db.weaponDao().insert(it.toEntity()) }
            backup.ammunition.forEach { db.ammunitionDao().insert(it.toEntity()) }
            backup.sessions.forEach { db.sessionDao().insert(it.toEntity()) }
        }
        ImportResult.Success(backup.weapons.size, backup.ammunition.size, backup.sessions.size)
    } catch (e: Exception) {
        ImportResult.Error(e.message ?: "okänt fel")
    }
}
