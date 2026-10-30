package de.einkaufsliste.app.data

import android.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** Eine empfangene, noch nicht übernommene Liste. */
data class SharedList(val name: String, val emoji: String, val items: List<Pair<String, String>>)

/**
 * Listen werden als Text verschickt: lesbare Aufzählung plus eine Zeile
 * „EKL1:<base64>“, aus der die App die Liste wieder einlesen kann.
 */
object ShareCode {
    private const val PREFIX = "EKL1:"
    private val CODE = Regex("EKL1:([A-Za-z0-9_-]+)")
    private const val FLAGS = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP

    fun text(list: ShoppingList): String {
        val open = list.items.filter { !it.done }
        val payload = buildJsonObject {
            put("n", list.name)
            put("e", list.emoji)
            putJsonArray("i") {
                open.forEach { item ->
                    addJsonArray {
                        add(item.name)
                        if (item.qty.isNotEmpty()) add(item.qty)
                    }
                }
            }
        }
        val code = Base64.encodeToString(payload.toString().toByteArray(Charsets.UTF_8), FLAGS)
        val lines = if (open.isEmpty()) "(leer)" else open.joinToString("\n") { item ->
            "• ${item.name}" + if (item.qty.isNotEmpty()) " (${item.qty})" else ""
        }
        return "${list.emoji} ${list.name}\n$lines\n\n" +
            "Zum Übernehmen in die Einkaufsliste-App: Nachricht teilen → „Einkaufsliste“ wählen " +
            "oder kopieren und in der App „Liste importieren“ nutzen.\n$PREFIX$code"
    }

    fun parse(text: String): SharedList? = try {
        val code = CODE.find(text)?.groupValues?.get(1) ?: return null
        val raw = String(Base64.decode(code, FLAGS), Charsets.UTF_8)
        val obj = Json.parseToJsonElement(raw).jsonObject
        val name = obj["n"]?.jsonPrimitive?.contentOrNull?.take(60)?.ifBlank { null } ?: "Geteilte Liste"
        val emoji = obj["e"]?.jsonPrimitive?.contentOrNull?.take(8) ?: "📝"
        val items = (obj["i"] as? JsonArray).orEmpty().mapNotNull { entry ->
            val arr = entry.jsonArray
            val itemName = arr.getOrNull(0)?.jsonPrimitive?.contentOrNull?.let(::cleanName)?.take(80)
            if (itemName.isNullOrEmpty()) null
            else itemName to (arr.getOrNull(1)?.jsonPrimitive?.contentOrNull?.take(40) ?: "")
        }
        SharedList(name, emoji, items)
    } catch (e: Exception) {
        null
    }
}
