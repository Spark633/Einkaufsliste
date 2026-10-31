package de.einkaufsliste.app.data

import android.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

/** Eine empfangene, noch nicht übernommene Liste. */
data class SharedList(val name: String, val emoji: String, val items: List<Pair<String, String>>)

/**
 * Listen werden als Text (oder QR-Code) verschickt: lesbare Aufzählung plus eine Zeile
 * „EKL2:<base64>“ mit den komprimierten Listen, aus der die App sie wieder einlesen kann.
 * Das ältere Format „EKL1:“ (eine Liste, unkomprimiert) wird weiterhin gelesen.
 */
object ShareCode {
    private val CODE_V1 = Regex("EKL1:([A-Za-z0-9_-]+)")
    private val CODE_V2 = Regex("EKL2:([A-Za-z0-9_-]+)")
    private const val FLAGS = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP

    /** Nur der Code, z. B. für den QR-Code. */
    fun code(lists: List<ShoppingList>): String {
        val payload = buildJsonArray {
            lists.forEach { list ->
                add(buildJsonObject {
                    put("n", list.name)
                    put("e", list.emoji)
                    putJsonArray("i") {
                        list.items.filter { !it.done }.forEach { item ->
                            addJsonArray {
                                add(item.name)
                                if (item.qty.isNotEmpty()) add(item.qty)
                            }
                        }
                    }
                })
            }
        }
        return "EKL2:" + Base64.encodeToString(deflate(payload.toString().toByteArray(Charsets.UTF_8)), FLAGS)
    }

    /** Lesbarer Text mit Code am Ende, zum Verschicken per Messenger. */
    fun text(lists: List<ShoppingList>): String {
        val readable = lists.joinToString("\n\n") { list ->
            val open = list.items.filter { !it.done }
            val lines = if (open.isEmpty()) "(leer)" else open.joinToString("\n") { item ->
                "• ${item.name}" + if (item.qty.isNotEmpty()) " (${item.qty})" else ""
            }
            "${list.emoji} ${list.name}\n$lines"
        }
        return "$readable\n\n" +
            "Zum Übernehmen in die Einkaufsliste-App: Nachricht teilen → „Einkaufsliste“ wählen " +
            "oder kopieren und in der App „Listen importieren“ nutzen.\n${code(lists)}"
    }

    fun parse(text: String): List<SharedList>? = try {
        val v2 = CODE_V2.find(text)?.groupValues?.get(1)
        val v1 = CODE_V1.find(text)?.groupValues?.get(1)
        val lists = when {
            v2 != null -> Json.parseToJsonElement(String(inflate(Base64.decode(v2, FLAGS)), Charsets.UTF_8))
                .jsonArray.mapNotNull { parseList(it) }
            v1 != null -> listOfNotNull(parseList(Json.parseToJsonElement(String(Base64.decode(v1, FLAGS), Charsets.UTF_8))))
            else -> emptyList()
        }
        lists.ifEmpty { null }
    } catch (e: Exception) {
        null
    }

    private fun parseList(element: JsonElement): SharedList? {
        val obj = element as? JsonObject ?: return null
        val name = obj["n"]?.jsonPrimitive?.contentOrNull?.take(60)?.ifBlank { null } ?: "Geteilte Liste"
        val emoji = obj["e"]?.jsonPrimitive?.contentOrNull?.take(8) ?: "📝"
        val items = (obj["i"] as? JsonArray).orEmpty().mapNotNull { entry ->
            val arr = entry.jsonArray
            val itemName = arr.getOrNull(0)?.jsonPrimitive?.contentOrNull?.let(::cleanName)?.take(80)
            if (itemName.isNullOrEmpty()) null
            else itemName to (arr.getOrNull(1)?.jsonPrimitive?.contentOrNull?.take(40) ?: "")
        }
        return SharedList(name, emoji, items)
    }

    private fun deflate(data: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        deflater.setInput(data)
        deflater.finish()
        val out = ByteArrayOutputStream()
        val buf = ByteArray(1024)
        while (!deflater.finished()) out.write(buf, 0, deflater.deflate(buf))
        deflater.end()
        return out.toByteArray()
    }

    private fun inflate(data: ByteArray): ByteArray {
        val inflater = Inflater(true)
        inflater.setInput(data)
        val out = ByteArrayOutputStream()
        val buf = ByteArray(1024)
        while (!inflater.finished()) {
            val n = inflater.inflate(buf)
            if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
            out.write(buf, 0, n)
            if (out.size() > 1_000_000) break // Schutz vor manipulierten Codes
        }
        inflater.end()
        return out.toByteArray()
    }
}
