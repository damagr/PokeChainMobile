package com.pokechain.data.dialgadex

import android.content.Context
import com.pokechain.data.models.AppLanguage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class NameTranslator(context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val enNames: List<String>
    private val esNames: List<String>
    private val esMoveNames: Map<String, String>
    private val moveNameToId: Map<String, String>

    init {
        val enRoot = loadJson(context, "locales/pokedata_en.json")
        val esRoot = loadJson(context, "locales/pokedata_es.json")

        enNames = parseSpeciesList(enRoot)
        esNames = parseSpeciesList(esRoot)

        val enMoves = enRoot?.jsonObject?.get("moves")?.jsonObject ?: emptyMap()
        val esMoves = esRoot?.jsonObject?.get("moves")?.jsonObject ?: emptyMap()
        // special_moves (Weather Ball, Aura Wheel, Techno Blast) nunca se cargaban
        val enSpecial = enRoot?.jsonObject?.get("special_moves")?.jsonObject ?: emptyMap()
        val esSpecial = esRoot?.jsonObject?.get("special_moves")?.jsonObject ?: emptyMap()

        esMoveNames = (esMoves + esSpecial).mapValues {
            it.value.jsonPrimitive.content.trim()
        }

        // Normalizar guiones a espacios en las claves: dialgadex envía "x_scissor"
        // y el JSON trae "X-Scissor" -> sin esto, los movimientos con guion no se traducen
        moveNameToId = (enMoves + enSpecial).entries.associate { (id, name) ->
            name.jsonPrimitive.content.lowercase().replace("-", " ") to id
        }
    }

    private fun loadJson(context: Context, path: String) = try {
        val text = context.assets.open(path).bufferedReader().use { it.readText() }
        json.parseToJsonElement(text)
    } catch (e: Exception) {
        null
    }

    private fun parseSpeciesList(root: kotlinx.serialization.json.JsonElement?) =
        try {
            root?.jsonObject?.get("species")?.jsonArray?.mapNotNull {
                val s = it.jsonPrimitive.content
                if (s.isBlank()) null else s
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

    fun getName(dex: Int, language: AppLanguage): String {
        val list = when (language) {
            AppLanguage.EN -> enNames
            AppLanguage.ES -> esNames
        }
        return list.getOrNull(dex - 1) ?: enNames.getOrNull(dex - 1) ?: "???"
    }

    /**
     * Returns the full list of (dexNumber, name) pairs for autocomplete lookup.
     * The list is sorted by dex number (ascending).
     */
    fun getAllNames(language: AppLanguage): List<Pair<Int, String>> {
        val list = when (language) {
            AppLanguage.EN -> enNames
            AppLanguage.ES -> esNames
        }
        return list
            .mapIndexedNotNull { index, name ->
                val dex = index + 1
                if (name.isNotBlank()) dex to name else null
            }
    }

    fun getMoveName(moveIdOrName: String, language: AppLanguage): String {
        // Normalizar guiones también: "mud-slap" del JSON casa con "mud_slap" de dialgadex
        var normalized = moveIdOrName
            .replace("_", " ")
            .replace("-", " ")
            .lowercase()
            .trim()

        // Extract supermega suffix (+, ++, +++, *, **, etc.) to preserve it
        val suffix = when {
            normalized.endsWith("+") || normalized.endsWith("*") -> {
                val match = Regex("(\\++|\\*+)$").find(normalized)?.value ?: ""
                normalized = normalized.dropLast(match.length)
                match
            }
            else -> ""
        }

        val dialgaDexId = moveNameToId[normalized]
        val baseDisplayName = when (language) {
            AppLanguage.ES -> esMoveNames[dialgaDexId] ?: normalized
            AppLanguage.EN -> normalized.split(" ").joinToString(" ") { it.replaceFirstChar { it.uppercase() } }
        }
        // Fallback: usar normalized (SIN sufijo) para no duplicarlo al concatenar
        return "${if (dialgaDexId != null) baseDisplayName else normalized.split(" ").joinToString(" ") { it.replaceFirstChar { it.uppercase() } }}$suffix"
    }
}
