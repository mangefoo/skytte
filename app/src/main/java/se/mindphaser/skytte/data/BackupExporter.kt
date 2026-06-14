package se.mindphaser.skytte.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import se.mindphaser.skytte.data.repo.Repositories
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val json = Json {
    prettyPrint = true
    encodeDefaults = true // keep every field (incl. version + null cost fields) in the snapshot
}
private val fileStampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

/**
 * Reads all data from the user's Firestore subtree, writes it as pretty-printed JSON to a file in
 * the cache, and returns a content:// [Uri] suitable for sharing. Using the cache directory means
 * no storage permission is required.
 */
suspend fun exportBackup(context: Context, repos: Repositories): Uri = withContext(Dispatchers.IO) {
    val now = Instant.now()
    val backup = BackupData(
        exportedAt = now.toString(),
        weapons = repos.weapons.getAll().map(WeaponDto::from),
        ammunition = repos.ammunition.getAll().map(AmmunitionDto::from),
        sessions = repos.sessions.getAll().map(SessionDto::from),
    )

    val stamp = LocalDateTime.ofInstant(now, ZoneId.systemDefault()).format(fileStampFormatter)
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, "skytte-export-$stamp.json")
    file.writeText(json.encodeToString(backup))

    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/** Launches the system share sheet so the user can attach the export to e.g. email. */
fun shareBackup(context: Context, uri: Uri, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Skytte export")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, chooserTitle))
}
