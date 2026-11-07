package de.einkaufsliste.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

private const val LATEST_RELEASE_API = "https://api.github.com/repos/Spark633/Einkaufsliste/releases/latest"
private const val RELEASES_PAGE = "https://github.com/Spark633/Einkaufsliste/releases/latest"

/** Neueste veröffentlichte Version mit Download-Link der APK. */
data class Release(val version: String, val downloadUrl: String)

fun currentVersion(context: Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"

/** Fragt bei GitHub die neueste Version ab; null ohne Internet oder bei Fehlern. */
suspend fun fetchLatestRelease(): Release? = withContext(Dispatchers.IO) {
    try {
        val conn = URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        try {
            if (conn.responseCode != 200) return@withContext null
            val obj = Json.parseToJsonElement(conn.inputStream.bufferedReader().readText()).jsonObject
            val tag = obj["tag_name"]?.jsonPrimitive?.contentOrNull ?: return@withContext null
            val apk = obj["assets"]?.jsonArray
                ?.map { it.jsonObject }
                ?.firstOrNull { it["name"]?.jsonPrimitive?.contentOrNull?.endsWith(".apk") == true }
                ?.get("browser_download_url")?.jsonPrimitive?.contentOrNull
            Release(tag.removePrefix("v"), apk ?: RELEASES_PAGE)
        } finally {
            conn.disconnect()
        }
    } catch (e: Exception) {
        null
    }
}

/** true, wenn [latest] (z. B. "2.0.19") neuer ist als [current]. */
fun isNewer(latest: String, current: String): Boolean {
    val a = latest.split('.').map { it.toIntOrNull() ?: 0 }
    val b = current.split('.').map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

/** APK im Browser herunterladen; danach installiert Android sie über die alte Version. */
fun openDownload(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        // kein Browser vorhanden – nichts zu tun
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val current = remember { currentVersion(context) }
    var checking by remember { mutableStateOf(true) }
    var latest by remember { mutableStateOf<Release?>(null) }
    LaunchedEffect(Unit) {
        latest = fetchLatestRelease()
        checking = false
    }
    val release = latest

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Version & Updates") },
        text = {
            Column {
                Text("Installiert: Version $current", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        checking -> "Suche nach neuer Version …"
                        release == null -> "Neueste Version konnte nicht abgefragt werden (keine Internetverbindung?)."
                        isNewer(release.version, current) -> "Neue Version ${release.version} ist verfügbar."
                        else -> "Du hast die neueste Version."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (release != null && isNewer(release.version, current)) {
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { openDownload(context, release.downloadUrl) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Version ${release.version} herunterladen")
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Nach dem Download die Datei öffnen und installieren – deine Listen bleiben erhalten.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schließen") } },
        dismissButton = {
            if (release == null && !checking) {
                TextButton(onClick = { openDownload(context, RELEASES_PAGE) }) { Text("Download-Seite öffnen") }
            }
        },
    )
}
