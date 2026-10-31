package de.einkaufsliste.app.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import de.einkaufsliste.app.data.SharedList
import de.einkaufsliste.app.data.ShareCode
import de.einkaufsliste.app.data.ShoppingList

/** Auswahl, welche Listen geteilt werden, und wie (Nachricht oder QR-Code). */
@Composable
fun ShareDialog(
    lists: List<ShoppingList>,
    preselected: Set<String>,
    onDismiss: () -> Unit,
    onSendText: (List<ShoppingList>) -> Unit,
    onShowQr: (List<ShoppingList>) -> Unit,
) {
    var selected by remember { mutableStateOf(preselected) }
    val chosen = lists.filter { it.id in selected }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Listen teilen") },
        text = {
            Column {
                Text(
                    "Welche Listen möchtest du teilen?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(lists, key = { it.id }) { list ->
                        val checked = list.id in selected
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                selected = if (checked) selected - list.id else selected + list.id
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(12.dp))
                            Text("${list.emoji} ${list.name}", modifier = Modifier.weight(1f), maxLines = 1)
                            Text(
                                "${list.items.count { !it.done }}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                        }
                    }
                }
                if (lists.size > 1) {
                    TextButton(onClick = {
                        selected = if (selected.size == lists.size) emptySet() else lists.map { it.id }.toSet()
                    }) { Text(if (selected.size == lists.size) "Keine auswählen" else "Alle auswählen") }
                }
                Spacer(Modifier.height(8.dp))
                Button(enabled = chosen.isNotEmpty(), onClick = { onSendText(chosen) }, modifier = Modifier.fillMaxWidth()) {
                    Text("📤  Als Nachricht senden")
                }
                OutlinedButton(enabled = chosen.isNotEmpty(), onClick = { onShowQr(chosen) }, modifier = Modifier.fillMaxWidth()) {
                    Text("🔳  QR-Code anzeigen")
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

/** Erzeugt den QR-Code; null, wenn die Listen zu groß dafür sind. */
private fun qrBitmap(content: String): Bitmap? = try {
    val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L, EncodeHintType.MARGIN to 2)
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
    val w = matrix.width
    val h = matrix.height
    val pixels = IntArray(w * h) { i -> if (matrix.get(i % w, i / w)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
    Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
} catch (e: Exception) {
    null
}

@Composable
fun QrDialog(lists: List<ShoppingList>, onDismiss: () -> Unit, onSendText: () -> Unit) {
    val bitmap = remember(lists) { qrBitmap(ShareCode.code(lists)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (lists.size == 1) "${lists[0].emoji} ${lists[0].name}" else "${lists.size} Listen") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "QR-Code",
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.None,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(Color.White)
                            .padding(8.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Auf dem anderen Handy in der Einkaufsliste-App: ⋮ → „Listen importieren“ → „QR-Code scannen“.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "Die Listen sind zu groß für einen QR-Code. Wähle weniger Listen aus oder teile sie als Nachricht.",
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fertig") } },
        dismissButton = { if (bitmap == null) TextButton(onClick = onSendText) { Text("Als Nachricht") } },
    )
}

/** QR-Code mit dem Scanner von Google Play-Diensten einlesen (keine Kamera-Berechtigung nötig). */
fun scanQr(context: Context, onResult: (String) -> Unit, onError: (String) -> Unit) {
    val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    GmsBarcodeScanning.getClient(context, options).startScan()
        .addOnSuccessListener { barcode -> barcode.rawValue?.let(onResult) }
        .addOnFailureListener { onError("QR-Scanner nicht verfügbar – bitte Google Play-Dienste aktualisieren") }
}

/** Mehrere empfangene Listen auf einmal übernehmen. */
@Composable
fun MultiImportDialog(shared: List<SharedList>, lists: List<ShoppingList>, onDismiss: () -> Unit, onImport: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${shared.size} Listen empfangen") },
        text = {
            Column {
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(shared) { s ->
                        val exists = lists.any { it.name.equals(s.name, ignoreCase = true) }
                        Row(Modifier.padding(vertical = 6.dp)) {
                            Text("${s.emoji} ${s.name}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${s.items.size} Artikel" + if (exists) " · ergänzen" else " · neu",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Listen, die du schon hast, werden um fehlende Artikel ergänzt. Die anderen werden neu angelegt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onImport) { Text("Übernehmen") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
