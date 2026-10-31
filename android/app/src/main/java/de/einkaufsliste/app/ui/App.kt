package de.einkaufsliste.app.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.einkaufsliste.app.data.AppViewModel
import de.einkaufsliste.app.data.Item
import de.einkaufsliste.app.data.ShareCode
import de.einkaufsliste.app.data.ShoppingList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed interface DialogState {
    data class EditItem(val listId: String, val itemId: String) : DialogState
    data class EditList(val listId: String?) : DialogState
    data class DeleteList(val listId: String) : DialogState
    data class DeleteItem(val listId: String, val itemId: String) : DialogState
    data class ClearDone(val listId: String) : DialogState
    data class QuickAdd(val listId: String) : DialogState
    data object Arrange : DialogState
    data object History : DialogState
    data object PasteImport : DialogState
}

/** Alles, was die Bildschirme auslösen können. */
class AppActions(
    private val vm: AppViewModel,
    private val snackbar: SnackbarHostState,
    private val scope: CoroutineScope,
    private val context: Context,
    val openList: (String) -> Unit,
    val showDialog: (DialogState) -> Unit,
) {
    fun message(text: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(text, duration = SnackbarDuration.Short)
        }
    }

    /** Führt eine Änderung aus und bietet sie über die Snackbar zum Rückgängigmachen an. */
    private fun undoable(text: String, change: () -> Unit) {
        val before = vm.snapshot()
        change()
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(text, actionLabel = "Rückgängig", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) vm.restore(before)
        }
    }

    fun toggle(listId: String, item: Item) =
        undoable(if (item.done) "„${item.name}“ zurückgeholt" else "„${item.name}“ erledigt") { vm.toggleDone(listId, item.id) }

    /** Löschen immer erst nach Rückfrage. */
    fun delete(listId: String, item: Item) = showDialog(DialogState.DeleteItem(listId, item.id))

    fun confirmedDelete(listId: String, item: Item) =
        undoable("„${item.name}“ gelöscht") { vm.deleteItem(listId, item.id) }

    fun edit(listId: String, item: Item) = showDialog(DialogState.EditItem(listId, item.id))

    fun clearDone(listId: String) = showDialog(DialogState.ClearDone(listId))

    fun confirmedClearDone(listId: String) {
        val count = vm.list(listId)?.items?.count { it.done } ?: 0
        undoable("$count erledigte Artikel entfernt") { vm.clearDone(listId) }
    }

    fun uncheckAll(listId: String) = undoable("Alle Artikel wieder auf der Liste") { vm.uncheckAll(listId) }

    fun duplicate(listId: String) {
        vm.duplicateList(listId)
        message("Liste kopiert")
    }

    fun share(list: ShoppingList) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, list.name)
            putExtra(Intent.EXTRA_TEXT, ShareCode.text(list))
        }
        context.startActivity(Intent.createChooser(send, "Liste teilen über …"))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(vm: AppViewModel) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var openListId by rememberSaveable { mutableStateOf<String?>(null) }
    var dialog by remember { mutableStateOf<DialogState?>(null) }

    val actions = remember(vm) {
        AppActions(vm, snackbar, scope, context, openList = { openListId = it }, showDialog = { dialog = it })
    }

    val openList = openListId?.let { vm.list(it) }
    if (openList != null) {
        BackHandler { openListId = null }
        ListScreen(vm, openList, actions, snackbar, onBack = { openListId = null })
    } else {
        HomeScreen(vm, actions, snackbar)
    }

    val close = { dialog = null }
    when (val d = dialog) {
        null -> Unit
        is DialogState.EditItem -> {
            val item = vm.list(d.listId)?.items?.find { it.id == d.itemId }
            if (item == null) close() else EditItemDialog(
                item = item,
                onDismiss = close,
                onSave = { name, qty -> vm.editItem(d.listId, item.id, name, qty); close() },
                onToggle = { close(); actions.toggle(d.listId, item) },
                onDelete = { close(); actions.delete(d.listId, item) },
            )
        }
        is DialogState.DeleteItem -> {
            val item = vm.list(d.listId)?.items?.find { it.id == d.itemId }
            if (item == null) close() else ConfirmDialog(
                title = "„${item.name}“ löschen?",
                text = "Der Artikel wird von der Liste entfernt.",
                confirmLabel = "Löschen",
                onDismiss = close,
                onConfirm = { close(); actions.confirmedDelete(d.listId, item) },
            )
        }
        is DialogState.ClearDone -> {
            val count = vm.list(d.listId)?.items?.count { it.done } ?: 0
            ConfirmDialog(
                title = "Erledigte entfernen?",
                text = "$count erledigte Artikel werden von der Liste gelöscht.",
                confirmLabel = "Entfernen",
                onDismiss = close,
                onConfirm = { close(); actions.confirmedClearDone(d.listId) },
            )
        }
        is DialogState.EditList -> EditListDialog(
            list = d.listId?.let { vm.list(it) },
            onDismiss = close,
            onSave = { name, emoji ->
                if (d.listId == null) openListId = vm.addList(name, emoji) else vm.editList(d.listId, name, emoji)
                close()
            },
        )
        is DialogState.DeleteList -> {
            val list = vm.list(d.listId)
            if (list == null) close() else ConfirmDialog(
                title = "„${list.name}“ löschen?",
                text = "Alle Einträge dieser Liste gehen verloren. Deine Vorschläge bleiben erhalten.",
                confirmLabel = "Löschen",
                onDismiss = close,
                onConfirm = {
                    if (openListId == list.id) openListId = null
                    vm.deleteList(list.id)
                    close()
                },
            )
        }
        is DialogState.QuickAdd -> {
            val list = vm.list(d.listId)
            if (list == null) close() else {
                var info by remember { mutableStateOf("") }
                ModalBottomSheet(
                    onDismissRequest = close,
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                ) {
                    Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp).imePadding()) {
                        Text(
                            "${list.emoji} ${list.name}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(12.dp))
                        AddItemBar(vm, list.id, onMessage = { info = it }, autoFocus = true, modifier = Modifier.fillMaxWidth())
                        val open = list.items.count { !it.done }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            info.ifEmpty { "$open Artikel offen" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        DialogState.Arrange -> ArrangeDialog(vm, onDismiss = close)
        DialogState.History -> HistoryDialog(vm, onDismiss = close)
        DialogState.PasteImport -> PasteImportDialog(
            onDismiss = close,
            onImport = { text ->
                if (vm.receiveShared(text)) close() else actions.message("Kein gültiger Listen-Code gefunden")
            },
        )
    }

    vm.pendingImport?.let { shared ->
        ImportChoiceDialog(
            shared = shared,
            lists = vm.data.lists,
            onDismiss = { vm.pendingImport = null },
            onNew = { openListId = vm.importAsNew(shared) },
            onMerge = { target ->
                val added = vm.importInto(shared, target.id)
                openListId = target.id
                actions.message("$added Artikel übernommen")
            },
        )
    }
}
