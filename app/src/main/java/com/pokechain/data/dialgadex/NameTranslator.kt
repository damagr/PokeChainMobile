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

        esMoveNames = esMoves.mapKeys { it.key }.mapValues { it.value.jsonPrimitive.content }

        moveNameToId = enMoves.entries.associate { (id, name) ->
            name.jsonPrimitive.content.lowercase() to id
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

    fun getMoveName(moveIdOrName: String, language: AppLanguage): String {
        val normalized = moveIdOrName
            .replace("_", " ")
            .lowercase()
            .trim()

        val dialgaDexId = moveNameToId[normalized]

        return when (language) {
            AppLanguage.ES -> {
                if (dialgaDexId != null) {
                    esMoveNames[dialgaDexId] ?: moveIdOrName
                } else {
                    moveIdOrName
                }
            }
            AppLanguage.EN -> moveIdOrName
        }
    }
}
