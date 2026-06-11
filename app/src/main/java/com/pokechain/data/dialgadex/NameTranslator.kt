package com.pokechain.data.dialgadex

import android.content.Context
import com.pokechain.data.models.AppLanguage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class NameTranslator(context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val enNames: List<String>
    private val esNames: List<String>

    init {
        enNames = loadSpeciesList(context, "locales/pokedata_en.json")
        esNames = loadSpeciesList(context, "locales/pokedata_es.json")
    }

    private fun loadSpeciesList(context: Context, path: String): List<String> {
        return try {
            val text = context.assets.open(path).bufferedReader().use { it.readText() }
            val root = json.parseToJsonElement(text)
            val species = root.jsonObject["species"] ?: return emptyList()
            species.jsonArray.mapNotNull {
                val s = it.jsonPrimitive.content
                if (s.isBlank()) null else s
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getName(dex: Int, language: AppLanguage): String {
        val list = when (language) {
            AppLanguage.EN -> enNames
            AppLanguage.ES -> esNames
        }
        return list.getOrNull(dex - 1) ?: enNames.getOrNull(dex - 1) ?: "???"
    }
}
