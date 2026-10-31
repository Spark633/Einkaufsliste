package de.einkaufsliste.app.data

import kotlinx.serialization.Serializable
import java.text.Collator
import java.util.Locale
import java.util.UUID

@Serializable
data class Item(
    val id: String = newId(),
    val name: String,
    val qty: String = "",
    val done: Boolean = false,
)

@Serializable
data class ShoppingList(
    val id: String = newId(),
    val name: String,
    val emoji: String = "🛒",
    val items: List<Item> = emptyList(),
    /** Wird in der Ansicht „Alle auf einen Blick“ angezeigt. */
    val inOverview: Boolean = true,
    /** Offene Artikel alphabetisch statt „neueste zuerst“ anzeigen. */
    val alphabetical: Boolean = false,
) {
    /** Offene Artikel in Anzeigereihenfolge. Neue Artikel werden hinten angehängt, daher umgedreht. */
    fun openItemsSorted(): List<Item> {
        val open = items.filter { !it.done }
        return if (alphabetical) open.sortedWith(compareBy(GERMAN_COLLATOR) { it.name }) else open.reversed()
    }
}

private val GERMAN_COLLATOR: Collator = Collator.getInstance(Locale.GERMAN)

/** Ein gemerkter Artikel für die Vorschläge beim Eintippen. */
@Serializable
data class HistoryEntry(
    val name: String,
    val count: Int = 0,
    val last: Long = 0,
    /** Wie oft der Artikel auf welcher Liste (nach id) eingetragen wurde. */
    val lists: Map<String, Int> = emptyMap(),
)

@Serializable
data class AppData(
    val lists: List<ShoppingList> = listOf(ShoppingList(name = "Einkauf")),
    /** Schlüssel: Artikelname in Kleinbuchstaben. */
    val history: Map<String, HistoryEntry> = emptyMap(),
    /** Startbildschirm zeigt alle Listen untereinander statt der Listenübersicht. */
    val overviewMode: Boolean = false,
    val seenSwipeHint: Boolean = false,
    /** Vorschläge unter dem Eingabefeld eingeklappt. */
    val suggestionsCollapsed: Boolean = false,
    /** In der Ansicht „Zusammen“ die zuletzt erledigten Artikel jeder Liste zeigen. */
    val showRecentDone: Boolean = true,
)

/** So viele zuletzt erledigte Artikel werden angezeigt, bevor man „alle anzeigen“ tippen muss. */
const val RECENT_DONE = 3

fun newId(): String = UUID.randomUUID().toString()

fun keyOf(name: String): String = name.trim().lowercase(Locale.GERMAN)

fun cleanName(name: String): String = name.trim().replace(Regex("\\s+"), " ")

val LIST_EMOJIS = listOf(
    "🛒", "🥦", "🥖", "🍎", "🧀", "🥩", "🍷", "🧴", "💊", "🐾",
    "🏠", "🔧", "🎒", "🧳", "🏕️", "🎁", "📝", "⭐",
)
