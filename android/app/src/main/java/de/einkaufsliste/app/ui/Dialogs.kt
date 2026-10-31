package de.einkaufsliste.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.einkaufsliste.app.data.AppViewModel
import de.einkaufsliste.app.data.Item
import de.einkaufsliste.app.data.LIST_EMOJIS
import de.einkaufsliste.app.data.SharedList
import de.einkaufsliste.app.data.ShoppingList
import de.einkaufsliste.app.data.keyOf

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditListDialog(
    list: ShoppingList?,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String) -> Unit,
) {
    var name by remember { mutableStateOf(list?.name ?: "") }
    var emoji by remember { mutableStateOf(list?.emoji ?: LIST_EMOJIS.first()) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (list == null) "Neue Liste" else "Liste bearbeiten") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text("Name") },
                    placeholder = { Text("z. B. Rewe, dm, Urlaub packen") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                Spacer(Modifier.height(16.dp))
                Text("Symbol", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LIST_EMOJIS.forEach { e ->
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (e == emoji) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                .clickable { emoji = e },
                            contentAlignment = Alignment.Center,
                        ) { Text(e, fontSize = 22.sp) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name, emoji) }) {
                Text(if (list == null) "Anlegen" else "Speichern")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
fun EditItemDialog(
    item: Item,
    onDismiss: () -> Unit,
    onSave: (name: String, qty: String) -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember { mutableStateOf(item.name) }
    var qty by remember { mutableStateOf(item.qty) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Artikel bearbeiten") },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(80) },
                    label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = qty, onValueChange = { qty = it.take(40) },
                    label = { Text("Menge / Notiz (optional)") },
                    placeholder = { Text("z. B. 2 Stück, 500 g, Bio") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
                    Icon(if (item.done) Icons.Filled.Refresh else Icons.Filled.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (item.done) "Wieder auf die Liste" else "Als erledigt markieren")
                }
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text("Artikel löschen", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(name, qty) }) { Text("Speichern") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

/** Reihenfolge der Listen und Auswahl für „Alle auf einen Blick“. */
@Composable
fun ArrangeDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val lists = vm.data.lists
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Listen anordnen") },
        text = {
            Column {
                Text(
                    "Mit den Pfeilen änderst du die Reihenfolge. Haken = Liste erscheint in der Ansicht „Zusammen“.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    itemsIndexed(lists, key = { _, l -> l.id }) { i, l ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = l.inOverview, onCheckedChange = { vm.setInOverview(l.id, it) })
                            Text("${l.emoji} ${l.name}", modifier = Modifier.weight(1f), maxLines = 1)
                            IconButton(enabled = i > 0, onClick = { vm.moveList(l.id, -1) }) {
                                Icon(Icons.Filled.KeyboardArrowUp, "Nach oben")
                            }
                            IconButton(enabled = i < lists.lastIndex, onClick = { vm.moveList(l.id, 1) }) {
                                Icon(Icons.Filled.KeyboardArrowDown, "Nach unten")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fertig") } },
    )
}

@Composable
fun HistoryDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val entries = vm.data.history.entries.sortedBy { it.value.name.lowercase() }
    var confirmClear by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vorschläge verwalten") },
        text = {
            Column {
                Text(
                    "Diese Artikel werden beim Eintippen vorgeschlagen. Mit ✕ entfernst du einen Vorschlag.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                if (entries.isEmpty()) {
                    Text("Noch keine Vorschläge gespeichert.", modifier = Modifier.padding(vertical = 16.dp))
                }
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(entries, key = { it.key }) { (key, h) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(h.name, modifier = Modifier.weight(1f))
                            Text("${h.count}×", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            IconButton(onClick = { toDelete = key }) { Icon(Icons.Filled.Close, "Entfernen") }
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fertig") } },
        dismissButton = {
            if (entries.isNotEmpty()) TextButton(onClick = { confirmClear = true }) { Text("Alle löschen") }
        },
    )

    toDelete?.let { key ->
        ConfirmDialog(
            title = "Vorschlag „${vm.data.history[key]?.name ?: key}“ löschen?",
            text = "Der Artikel wird dann nicht mehr vorgeschlagen.",
            confirmLabel = "Löschen",
            onDismiss = { toDelete = null },
            onConfirm = { vm.deleteHistory(key); toDelete = null },
        )
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "Alle Vorschläge löschen?",
            text = "Die Listen selbst bleiben erhalten.",
            confirmLabel = "Alle löschen",
            onDismiss = { confirmClear = false },
            onConfirm = { vm.clearHistory(); confirmClear = false },
        )
    }
}

/** Geteilte Nachricht per Einfügen importieren. */
@Composable
fun PasteImportDialog(onDismiss: () -> Unit, onScan: () -> Unit, onImport: (String) -> Unit) {
    val clipboard = LocalClipboardManager.current
    var text by remember {
        mutableStateOf(clipboard.getText()?.text?.takeIf { "EKL1:" in it || "EKL2:" in it } ?: "")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Listen importieren") },
        text = {
            Column {
                Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) { Text("🔳  QR-Code scannen") }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Oder die geteilte Nachricht hier einfügen (lange tippen → Einfügen).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    minLines = 4, maxLines = 8, modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { onImport(text) }) { Text("Importieren") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

/** Fragt, ob eine empfangene Liste neu angelegt oder in eine gleichnamige übernommen wird. */
@Composable
fun ImportChoiceDialog(
    shared: SharedList,
    lists: List<ShoppingList>,
    onDismiss: () -> Unit,
    onNew: () -> Unit,
    onMerge: (ShoppingList) -> Unit,
) {
    val same = lists.find { keyOf(it.name) == keyOf(shared.name) }
    val preview = shared.items.take(8).joinToString(", ") { it.first } + if (shared.items.size > 8) " …" else ""
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Geteilte Liste empfangen") },
        text = {
            Column {
                Text("${shared.emoji} ${shared.name}", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Text(
                    "${shared.items.size} Artikel" + if (preview.isNotEmpty()) ": $preview" else "",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onNew, modifier = Modifier.fillMaxWidth()) { Text("Als neue Liste anlegen") }
                if (same != null) {
                    OutlinedButton(onClick = { onMerge(same) }, modifier = Modifier.fillMaxWidth()) {
                        Text("In „${same.name}“ übernehmen")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
