package de.einkaufsliste.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.einkaufsliste.app.data.AppViewModel
import de.einkaufsliste.app.data.RECENT_DONE
import de.einkaufsliste.app.data.ShoppingList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GreenTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit,
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

/** Drei-Punkte-Menü mit einfachen Einträgen (Text -> Aktion). */
@Composable
private fun OverflowMenu(entries: List<Pair<String, () -> Unit>>, tint: Color? = null) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            if (tint != null) Icon(Icons.Filled.MoreVert, "Menü", tint = tint) else Icon(Icons.Filled.MoreVert, "Menü")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            entries.forEach { (label, action) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { open = false; action() })
            }
        }
    }
}

private fun listMenu(list: ShoppingList, a: AppActions): List<Pair<String, () -> Unit>> = listOf(
    "📤  Liste teilen" to { a.share(list) },
    "✏️  Umbenennen / Symbol" to { a.showDialog(DialogState.EditList(list.id)) },
    (if (list.alphabetical) "🕒  Neueste zuerst sortieren" else "🔤  Alphabetisch sortieren") to {
        a.setAlphabetical(list.id, !list.alphabetical)
    },
    "📄  Liste kopieren" to { a.duplicate(list.id) },
    "🗑️  Liste löschen" to { a.showDialog(DialogState.DeleteList(list.id)) },
)

/* ------------------------------------------------------------------ */
/* Startbildschirm: Listenübersicht oder alle Listen untereinander     */
/* ------------------------------------------------------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: AppViewModel, a: AppActions, snackbar: SnackbarHostState) {
    val data = vm.data
    Scaffold(
        topBar = {
            GreenTopBar("Meine Listen") {
                OverflowMenu(
                    listOf(
                        "➕  Neue Liste" to { a.showDialog(DialogState.EditList(null)) },
                        "↕️  Reihenfolge der Listen" to { a.showDialog(DialogState.Arrange) },
                        "📥  Liste importieren" to { a.showDialog(DialogState.PasteImport) },
                        "💡  Vorschläge verwalten" to { a.showDialog(DialogState.History) },
                        (if (data.showRecentDone) "🙈  Zuletzt Erledigte ausblenden" else "👁️  Zuletzt Erledigte anzeigen") to {
                            vm.setShowRecentDone(!data.showRecentDone)
                        },
                    )
                )
            }
        },
        floatingActionButton = {
            if (!data.overviewMode) {
                FloatingActionButton(onClick = { a.showDialog(DialogState.EditList(null)) }) {
                    Icon(Icons.Filled.Add, "Neue Liste")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                SegmentedButton(
                    selected = !data.overviewMode,
                    onClick = { vm.setOverviewMode(false) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("Listen") }
                SegmentedButton(
                    selected = data.overviewMode,
                    onClick = { vm.setOverviewMode(true) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("Zusammen") }
            }
            if (data.overviewMode) OverviewContent(vm, a) else ListsContent(vm, a)
        }
    }
}

@Composable
private fun ListsContent(vm: AppViewModel, a: AppActions) {
    val lists = vm.data.lists
    if (lists.isEmpty()) {
        EmptyHint("Noch keine Liste.\nTippe auf +, um eine anzulegen.")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(lists, key = { it.id }) { list ->
            val open = list.items.count { !it.done }
            val done = list.items.size - open
            Card(
                onClick = { a.openList(list.id) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(list.emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(list.name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            when {
                                list.items.isEmpty() -> "leer"
                                done == 0 -> "$open offen"
                                else -> "$open offen · $done erledigt"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                        )
                    }
                    OverflowMenu(listMenu(list, a), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** Die ausgewählten Listen untereinander, z. B. erst Rewe, darunter dm. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewContent(vm: AppViewModel, a: AppActions) {
    val all = vm.data.lists
    val lists = all.filter { it.inOverview }

    // Auswahl: Welche Listen sollen zusammen angezeigt werden?
    Text(
        "Welche Listen zusammen anzeigen?",
        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        items(all, key = { it.id }) { list ->
            FilterChip(
                selected = list.inOverview,
                onClick = { vm.setInOverview(list.id, !list.inOverview) },
                label = { Text("${list.emoji} ${list.name}") },
                leadingIcon = if (list.inOverview) {
                    { Icon(Icons.Filled.Check, null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                } else null,
            )
        }
    }

    if (lists.isEmpty()) {
        EmptyHint("Tippe oben die Listen an, die du zusammen sehen möchtest – z. B. Rewe und dm.")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        lists.forEachIndexed { index, list ->
            val open = list.openItemsSorted()
            // erledigte Artikel stehen neueste zuerst in der Liste
            val doneItems = list.items.filter { it.done }
            val done = doneItems.size
            val recent = if (vm.data.showRecentDone) doneItems.take(RECENT_DONE) else emptyList()
            item(key = "head-${list.id}") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (index == 0) 0.dp else 18.dp, bottom = 2.dp)
                        .clickable { a.openList(list.id) }
                        .padding(start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(list.emoji, fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(list.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${open.size} offen", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalIconButton(onClick = { a.showDialog(DialogState.QuickAdd(list.id)) }) {
                        Icon(Icons.Filled.Add, "Artikel zu ${list.name} hinzufügen")
                    }
                    OverflowMenu(listMenu(list, a), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (open.isEmpty()) {
                item(key = "empty-${list.id}") {
                    Text(
                        if (done > 0) "Alles erledigt 🎉" else "Nichts auf der Liste",
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(open, key = { "${list.id}-${it.id}" }) { item ->
                ItemRow(
                    item = item,
                    onToggle = { a.toggle(list.id, item) },
                    onClick = { a.edit(list.id, item) },
                    modifier = Modifier.animateItem(),
                )
            }
            if (recent.isNotEmpty()) {
                item(key = "recent-${list.id}") {
                    Text(
                        "Zuletzt erledigt – zur Seite wischen zum Zurückholen",
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(recent, key = { "${list.id}-${it.id}" }) { item ->
                    ItemRow(item, { a.toggle(list.id, item) }, { a.edit(list.id, item) }, Modifier.animateItem())
                }
            }
            if (done > recent.size) {
                item(key = "done-${list.id}") {
                    val more = done - recent.size
                    TextButton(onClick = { a.openList(list.id) }) {
                        Text(if (recent.isEmpty()) "$done erledigt – Liste öffnen" else "$more weitere erledigt – Liste öffnen")
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Eine einzelne Liste                                                 */
/* ------------------------------------------------------------------ */

@Composable
fun ListScreen(vm: AppViewModel, list: ShoppingList, a: AppActions, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val open = list.openItemsSorted()
    val done = list.items.filter { it.done }
    var showAllDone by remember(list.id) { mutableStateOf(false) }
    val visibleDone = if (showAllDone) done else done.take(RECENT_DONE)

    Scaffold(
        topBar = {
            GreenTopBar("${list.emoji} ${list.name}", onBack = onBack) {
                OverflowMenu(
                    listMenu(list, a).take(3) + listOf(
                        "↩️  Alle Erledigten zurückholen" to { a.uncheckAll(list.id) },
                        "🧹  Erledigte entfernen" to { a.clearDone(list.id) },
                    ) + listMenu(list, a).drop(3)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            AddItemBar(vm, list.id, onMessage = a::message, modifier = Modifier.padding(12.dp))
            LazyColumn(
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (open.isEmpty()) {
                    item(key = "empty") {
                        EmptyHint(if (done.isNotEmpty()) "Alles erledigt! 🎉" else "Die Liste ist leer.\nTippe oben einen Artikel ein.")
                    }
                }
                items(open, key = { it.id }) { item ->
                    ItemRow(item, { a.toggle(list.id, item) }, { a.edit(list.id, item) }, Modifier.animateItem())
                }
                if (open.isNotEmpty() && !vm.data.seenSwipeHint) {
                    item(key = "hint") {
                        Text(
                            "Tipp: Artikel zur Seite wischen = erledigt,\nantippen = bearbeiten oder löschen.",
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (done.isNotEmpty()) {
                    item(key = "done-head") {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 18.dp, start = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (done.size > RECENT_DONE && !showAllDone) "Zuletzt erledigt (${done.size})" else "Erledigt (${done.size})",
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                            )
                            TextButton(onClick = { a.clearDone(list.id) }) { Text("Erledigte entfernen") }
                        }
                    }
                    items(visibleDone, key = { it.id }) { item ->
                        ItemRow(item, { a.toggle(list.id, item) }, { a.edit(list.id, item) }, Modifier.animateItem())
                    }
                    if (done.size > RECENT_DONE) {
                        item(key = "done-more") {
                            TextButton(onClick = { showAllDone = !showAllDone }) {
                                Text(if (showAllDone) "Nur die letzten $RECENT_DONE anzeigen" else "Alle ${done.size} erledigten anzeigen")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
