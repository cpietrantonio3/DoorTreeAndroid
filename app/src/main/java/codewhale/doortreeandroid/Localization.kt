package codewhale.doortreeandroid

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

object Localization {
    private val parser = Json { ignoreUnknownKeys = true }
    private var values: Map<String, Map<String, String>> = emptyMap()

    fun ensureLoaded(context: Context) {
        if (values.isNotEmpty()) return

        val raw = context.assets.open("localizable.json").bufferedReader().use { it.readText() }
        val root = parser.parseToJsonElement(raw).jsonObject
        val strings = root["strings"]?.jsonObject ?: JsonObject(emptyMap())

        values = strings.mapValues { (_, entry) ->
            val localizations = entry.jsonObject["localizations"]?.jsonObject ?: JsonObject(emptyMap())
            buildMap {
                localizations.forEach { (languageKey, localizedValue) ->
                    val value = localizedValue
                        .jsonObject["stringUnit"]
                        ?.jsonObject
                        ?.get("value")
                        ?.jsonPrimitive
                        ?.content
                        ?: ""
                    put(languageKey, normalizeFormatString(value))
                }
            }
        }
    }

    fun value(key: String): String {
        val localeKey = resolvedLocaleKey()
        return values[key]?.get(localeKey)
            ?: values[key]?.get("en")
            ?: key
    }

    fun format(key: String, vararg arguments: Any?): String {
        val format = value(key)
        return try {
            String.format(resolvedJavaLocale(), format, *arguments.map(::normalizeArgument).toTypedArray())
        } catch (_: Throwable) {
            format
        }
    }

    private fun resolvedLocaleKey(): String {
        val locale = Locale.getDefault()
        return when {
            locale.language.equals("fr", ignoreCase = true) && locale.country.equals("CA", ignoreCase = true) -> "fr-CA"
            locale.language.equals("fr", ignoreCase = true) -> "fr"
            else -> "en"
        }
    }

    private fun resolvedJavaLocale(): Locale {
        val locale = Locale.getDefault()
        return if (locale.language.equals("fr", ignoreCase = true) && locale.country.equals("CA", ignoreCase = true)) {
            Locale.CANADA_FRENCH
        } else {
            locale
        }
    }

    private fun normalizeArgument(value: Any?): Any? = when (value) {
        is Int, is Long, is Double, is Float, is String -> value
        else -> value?.toString()
    }

    private fun normalizeFormatString(raw: String): String {
        return raw
            .replace("%1${'$'}@", "%1${'$'}s")
            .replace("%2${'$'}@", "%2${'$'}s")
            .replace("%3${'$'}@", "%3${'$'}s")
            .replace("%4${'$'}@", "%4${'$'}s")
            .replace("%lld", "%d")
            .replace("%1${'$'}lld", "%1${'$'}d")
            .replace("%2${'$'}lld", "%2${'$'}d")
            .replace("%@", "%s")
    }
}

fun L(key: String): String = Localization.value(key)
fun LF(key: String, vararg arguments: Any?): String = Localization.format(key, *arguments)
