package de.einkaufsliste.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.einkaufsliste.app.data.AppViewModel
import de.einkaufsliste.app.data.Item
import de.einkaufsliste.app.data.ShoppingList

/**
 * Ein Artikel. Nach rechts wischen = erledigt umschalten, nach links wischen = löschen,
 * antippen = bearbeiten. Ein einfacher Tipp hakt also nie etwas ab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemRow(
    item: Item,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val toggle by rememberUpdatedState(onToggle)
    val delete by rememberUpdatedState(onDelete)
    val haptic = LocalHapticFeedback.current
    val state = rememberSwipeToDismissBoxState(positionalThreshold = { it * 0.35f })

    LaunchedEffect(state.currentValue) {
        when (state.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                toggle()
                state.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.EndToStart -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                delete()
                state.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }

    SwipeToDismissBox(
        state = state,
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        backgroundContent = {
            val toStart = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (toStart) MaterialTheme.colorScheme.error else DoneGreen)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (toStart) Arrangement.End else Arrangement.Start,
            ) {
                if (toStart) {
                    Text("Löschen", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.Delete, null, tint = Color.White)
                } else {
                    Icon(if (item.done) Icons.Filled.Refresh else Icons.Filled.Check, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(if (item.done) "Zurückholen" else "Erledigt", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        },
    ) {
        Surface(onClick = onClick, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .heightIn(min = 54.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CheckCircle(item.done)
                Spacer(Modifier.width(14.dp))
                Text(
                    item.name,
                    modifier = Modifier.weight(1f),
                    fontSize = 17.sp,
                    color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (item.done) TextDecoration.LineThrough else null,
                )
                if (item.qty.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text(item.qty, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun CheckCircle(done: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .then(
                if (done) Modifier.background(DoneGreen)
                else Modifier.border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (done) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

/** Eingabefeld mit Vorschlägen aus allem, was schon einmal eingetragen wurde. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddItemBar(
    vm: AppViewModel,
    listId: String,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = false,
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Artikel, der bisher auf einer anderen Liste stand -> erst nachfragen
    var askMove by remember { mutableStateOf<Pair<String, ShoppingList>?>(null) }

    fun addTo(targetId: String, name: String) {
        if (!vm.addItem(targetId, name)) {
            val where = if (targetId == listId) "der Liste" else "„${vm.list(targetId)?.name}“"
            onMessage("„${name.trim()}“ steht schon auf $where")
        } else if (targetId != listId) {
            onMessage("„${name.trim()}“ zu ${vm.list(targetId)?.name} hinzugefügt")
        }
    }

    fun add(name: String) {
        if (name.isBlank()) return
        val other = vm.usualListFor(listId, name)
        if (other != null) askMove = name.trim() to other else addTo(listId, name)
        text = ""
    }

    askMove?.let { (name, other) ->
        val here = vm.list(listId)
        AlertDialog(
            onDismissRequest = { askMove = null },
            title = { Text("Lieber auf „${other.name}“?") },
            text = { Text("„$name“ stand bisher auf ${other.emoji} ${other.name}. Wo möchtest du es eintragen?") },
            confirmButton = {
                TextButton(onClick = { askMove = null; addTo(other.id, name) }) { Text("Auf ${other.name}") }
            },
            dismissButton = {
                TextButton(onClick = { askMove = null; addTo(listId, name) }) { Text("Hier auf ${here?.name ?: "dieser Liste"}") }
            },
        )
    }

    if (autoFocus) LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Artikel hinzufügen …") },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { add(text) }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = { add(text) }, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "Hinzufügen")
            }
        }

        val suggestions = vm.suggestions(listId, text)
        val collapsed = vm.data.suggestionsCollapsed
        if (suggestions.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(Modifier.padding(start = 12.dp, end = 4.dp, bottom = if (collapsed) 0.dp else 12.dp)) {
                    // Kopfzeile zum Ein-/Ausklappen; der Zustand bleibt gespeichert
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.setSuggestionsCollapsed(!collapsed) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (collapsed) "${suggestions.size} Vorschläge – zum Anzeigen tippen"
                            else "Vorschläge – antippen zum Hinzufügen",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            if (collapsed) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                            contentDescription = if (collapsed) "Vorschläge anzeigen" else "Vorschläge einklappen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                    if (!collapsed) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { h ->
                            // Artikel von anderen Listen bekommen das Symbol ihrer üblichen Liste
                            val other = vm.usualListFor(listId, h.name)
                            SuggestionChip(
                                onClick = { add(h.name) },
                                label = { Text(if (other != null) "${h.name}  ${other.emoji}" else h.name, fontSize = 15.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                ),
                                border = null,
                            )
                        }
                    }
                }
            }
        }
    }
}
