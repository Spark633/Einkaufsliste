package de.einkaufsliste.app.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.min

/** Hält alle Listen und speichert sie bei jeder Änderung als JSON-Datei auf dem Gerät. */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        /** Ab so vielen Buchstaben erscheinen Vorschläge. */
        const val MIN_QUERY = 2
    }

    private val file = File(app.filesDir, "data.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val writer = Dispatchers.IO.limitedParallelism(1)

    var data by mutableStateOf(load())
        private set

    /** Empfangene Listen, für die gerade der Import-Dialog angezeigt wird. */
    var pendingImport by mutableStateOf<List<SharedList>?>(null)

    private fun load(): AppData = try {
        if (file.exists()) json.decodeFromString(AppData.serializer(), file.readText()) else AppData()
    } catch (e: Exception) {
        AppData()
    }

    private fun update(change: (AppData) -> AppData) {
        data = change(data)
        val snapshot = data
        viewModelScope.launch(writer) {
            val tmp = File(file.parentFile, "data.json.tmp")
            tmp.writeText(json.encodeToString(AppData.serializer(), snapshot))
            tmp.renameTo(file)
        }
    }

    private fun updateList(listId: String, change: (ShoppingList) -> ShoppingList) = update { d ->
        d.copy(lists = d.lists.map { if (it.id == listId) change(it) else it })
    }

    private fun remember(d: AppData, name: String, listId: String): AppData {
        val key = keyOf(name)
        val old = d.history[key] ?: HistoryEntry(name = name)
        val entry = old.copy(
            name = name,
            count = old.count + 1,
            last = System.currentTimeMillis(),
            lists = old.lists + (listId to (old.lists[listId] ?: 0) + 1),
        )
        return d.copy(history = d.history + (key to entry))
    }

    fun list(id: String): ShoppingList? = data.lists.find { it.id == id }

    /* ---------------- Listen ---------------- */

    fun addList(name: String, emoji: String): String {
        val list = ShoppingList(name = cleanName(name), emoji = emoji, inOverview = false)
        update { it.copy(lists = it.lists + list) }
        return list.id
    }

    fun editList(id: String, name: String, emoji: String) =
        updateList(id) { it.copy(name = cleanName(name), emoji = emoji) }

    fun deleteList(id: String) = update { d -> d.copy(lists = d.lists.filter { it.id != id }) }

    fun duplicateList(id: String) = update { d ->
        val idx = d.lists.indexOfFirst { it.id == id }
        if (idx < 0) return@update d
        val src = d.lists[idx]
        val copy = src.copy(id = newId(), name = "${src.name} (Kopie)", items = src.items.map { it.copy(id = newId()) })
        d.copy(lists = d.lists.toMutableList().apply { add(idx + 1, copy) })
    }

    fun moveList(id: String, delta: Int) = update { d ->
        val idx = d.lists.indexOfFirst { it.id == id }
        val target = idx + delta
        if (idx < 0 || target !in d.lists.indices) return@update d
        val lists = d.lists.toMutableList()
        lists.add(target, lists.removeAt(idx))
        d.copy(lists = lists)
    }

    fun setAlphabetical(id: String, on: Boolean) = updateList(id) { it.copy(alphabetical = on) }

    fun setInOverview(id: String, show: Boolean) = updateList(id) { it.copy(inOverview = show) }

    fun setOverviewMode(on: Boolean) = update { it.copy(overviewMode = on) }

    /** Für „Rückgängig“: Stand der Listen vor einer Aktion. */
    fun snapshot(): List<ShoppingList> = data.lists

    fun restore(lists: List<ShoppingList>) = update { it.copy(lists = lists) }

    /* ---------------- Artikel ---------------- */

    /** Fügt einen Artikel hinzu. Gibt false zurück, wenn er schon offen auf der Liste steht. */
    fun addItem(listId: String, rawName: String, qty: String = ""): Boolean {
        val name = cleanName(rawName)
        val list = list(listId) ?: return false
        if (name.isEmpty()) return true
        val existing = list.items.find { keyOf(it.name) == keyOf(name) }
        if (existing != null && !existing.done) return false
        update { d ->
            val withItem = d.copy(lists = d.lists.map { l ->
                if (l.id != listId) l
                else if (existing != null) {
                    // war erledigt -> wieder auf die Liste holen
                    val revived = existing.copy(done = false, qty = qty.ifEmpty { existing.qty })
                    l.copy(items = l.items.filter { it.id != existing.id } + revived)
                } else l.copy(items = l.items + Item(name = name, qty = qty))
            })
            remember(withItem, name, listId)
        }
        return true
    }

    fun toggleDone(listId: String, itemId: String) = updateList(listId) { l ->
        val item = l.items.find { it.id == itemId } ?: return@updateList l
        val toggled = item.copy(done = !item.done)
        val rest = l.items.filter { it.id != itemId }
        // Neu Erledigtes oben im Erledigt-Bereich, Zurückgeholtes unten bei den offenen
        l.copy(items = if (toggled.done) listOf(toggled) + rest else rest + toggled)
    }.also { if (!data.seenSwipeHint) update { it.copy(seenSwipeHint = true) } }

    fun deleteItem(listId: String, itemId: String) =
        updateList(listId) { l -> l.copy(items = l.items.filter { it.id != itemId }) }

    fun editItem(listId: String, itemId: String, rawName: String, qty: String) {
        val name = cleanName(rawName)
        if (name.isEmpty()) return
        update { d ->
            val old = d.lists.find { it.id == listId }?.items?.find { it.id == itemId }
            val changed = d.copy(lists = d.lists.map { l ->
                if (l.id != listId) l
                else l.copy(items = l.items.map { if (it.id == itemId) it.copy(name = name, qty = qty.trim()) else it })
            })
            if (old != null && keyOf(old.name) != keyOf(name)) remember(changed, name, listId) else changed
        }
    }

    fun clearDone(listId: String) = updateList(listId) { l -> l.copy(items = l.items.filter { !it.done }) }

    fun uncheckAll(listId: String) = updateList(listId) { l -> l.copy(items = l.items.map { it.copy(done = false) }) }

    /* ---------------- Vorschläge ---------------- */

    /**
     * Liste, auf der ein Artikel bisher stand, wenn er auf [listId] noch nie eingetragen wurde
     * (z. B. Zahnpasta bisher immer bei dm). Sonst null.
     */
    fun usualListFor(listId: String, name: String): ShoppingList? {
        val h = data.history[keyOf(name)] ?: return null
        if ((h.lists[listId] ?: 0) > 0) return null
        return h.lists.entries
            .filter { it.key != listId && it.value > 0 }
            .sortedByDescending { it.value }
            .firstNotNullOfOrNull { list(it.key) }
    }

    /**
     * Vorschläge aus allen Listen mit demselben Symbol (z. B. alle 🛒-Listen), aber erst ab
     * [MIN_QUERY] Buchstaben und nur, wenn der Name (oder ein Wort darin) mit dem Eingetippten
     * beginnt. Artikel dieser Liste stehen vorne.
     */
    fun suggestions(listId: String, query: String): List<HistoryEntry> {
        val list = list(listId) ?: return emptyList()
        val q = keyOf(query)
        if (q.length < MIN_QUERY) return emptyList()
        val onList = list.items.filter { !it.done }.map { keyOf(it.name) }.toSet()
        val sameIcon = data.lists.filter { it.emoji == list.emoji }.map { it.id }.toSet()
        return data.history.entries.mapNotNull { (key, h) ->
            if (key in onList) return@mapNotNull null
            if (h.lists.none { (id, count) -> id in sameIcon && count > 0 }) return@mapNotNull null
            var score = when {
                key.startsWith(q) -> 1000.0
                key.split(Regex("[\\s-]+")).any { it.startsWith(q) } -> 600.0
                else -> return@mapNotNull null
            }
            if (key == q) score += 500
            val hereCount = h.lists[listId] ?: 0
            if (hereCount > 0) score += 300
            score += min(h.count, 50) + hereCount * 4 + h.last / 1e13
            h to score
        }.sortedByDescending { it.second }.take(10).map { it.first }
    }

    fun setOverviewShowDone(show: Boolean) = update { it.copy(overviewShowDone = show) }

    fun setSuggestionsCollapsed(collapsed: Boolean) = update { it.copy(suggestionsCollapsed = collapsed) }

    fun deleteHistory(key: String) = update { it.copy(history = it.history - key) }

    fun clearHistory() = update { it.copy(history = emptyMap()) }

    /* ---------------- Teilen ---------------- */

    /** Liest eine geteilte Nachricht ein und öffnet den Import-Dialog. */
    fun receiveShared(text: String): Boolean {
        val shared = ShareCode.parse(text) ?: return false
        pendingImport = shared
        return true
    }

    fun importAsNew(shared: SharedList): String {
        val nameTaken = data.lists.any { keyOf(it.name) == keyOf(shared.name) }
        val list = ShoppingList(
            name = if (nameTaken) "${shared.name} (geteilt)" else shared.name,
            emoji = shared.emoji,
            inOverview = false,
            items = shared.items.map { (name, qty) -> Item(name = name, qty = qty) },
        )
        update { d -> shared.items.fold(d.copy(lists = d.lists + list)) { acc, (name, _) -> remember(acc, name, list.id) } }
        pendingImport = null
        return list.id
    }

    /** Ergänzt fehlende Artikel in einer bestehenden Liste. Gibt die Anzahl übernommener Artikel zurück. */
    fun importInto(shared: SharedList, listId: String): Int {
        var added = 0
        shared.items.forEach { (name, qty) -> if (addItem(listId, name, qty)) added++ }
        pendingImport = null
        return added
    }

    /** Mehrere Listen übernehmen: gleichnamige Listen werden ergänzt, andere neu angelegt. */
    fun importAll(shared: List<SharedList>) {
        shared.forEach { s ->
            val same = data.lists.find { keyOf(it.name) == keyOf(s.name) }
            if (same != null) importInto(s, same.id) else importAsNew(s)
        }
        pendingImport = null
    }
}
