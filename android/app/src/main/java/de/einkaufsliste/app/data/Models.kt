package de.einkaufsliste.app.data

import kotlinx.serialization.Serializable
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
)

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
)

fun newId(): String = UUID.randomUUID().toString()

fun keyOf(name: String): String = name.trim().lowercase(Locale.GERMAN)

fun cleanName(name: String): String = name.trim().replace(Regex("\\s+"), " ")

val LIST_EMOJIS = listOf(
    "🛒", "🥦", "🥖", "🍎", "🧀", "🥩", "🍷", "🧴", "💊", "🐾",
    "🏠", "🔧", "🎒", "🧳", "🏕️", "🎁", "📝", "⭐",
)
