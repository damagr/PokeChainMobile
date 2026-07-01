package com.pokechain.data.showcase

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Per-form size data: [pokedexHeight(m), pokedexWeight(kg), maxHeight(m)].
 */
data class SpeciesSize(
    val baseHeight: Double,
    val baseWeight: Double,
    val maxHeight: Double
) {
    /** Derived maximum weight for this species. */
    val maxWeight: Double get() {
        val ratio = maxHeight / baseHeight
        return baseWeight * (ratio + 0.5)
    }
}

/**
 * Full result of a showcase score calculation.
 */
data class ShowcaseResult(
    val total: Int,
    val heightPts: Double,
    val weightPts: Double,
    val ivPts: Double,
    val haveIvs: Boolean,
    val xxlPts: Int,
    val isXXL: Boolean,
    val isXXS: Boolean,
    val ratio: Double,
    val capped: Boolean,
    val maxH: Double,
    val maxW: Double
)

/**
 * Loads per-species size data from the bundled [showcase_data.json] asset.
 *
 * Data structure: `{ "dex": { "n": "Name", "f": { "formId": [baseH, baseW, maxH], ... } } }`
 * where dex is a string, formId "" is the default form.
 */
class ShowcaseDataProvider(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val data: Map<String, SpeciesFormData> = load(context)

    data class SpeciesFormData(
        val name: String,
        val forms: Map<String, SpeciesSize>,
        val isUniform: Boolean   // true if all forms have identical size data
    )

    private fun load(context: Context): Map<String, SpeciesFormData> {
        return try {
            val text = context.assets.open("showcase_data.json")
                .bufferedReader().use { it.readText() }
            val root = json.parseToJsonElement(text).jsonObject
            root.mapValues { (_, v) ->
                val obj = v.jsonObject
                val name = obj["n"]!!.jsonPrimitive.content
                val forms = obj["f"]!!.jsonObject.mapValues { (_, arr) ->
                    val a = arr.jsonArray
                    SpeciesSize(
                        baseHeight = a[0].jsonPrimitive.content.toDouble(),
                        baseWeight = a[1].jsonPrimitive.content.toDouble(),
                        maxHeight = a[2].jsonPrimitive.content.toDouble()
                    )
                }
                val sizes = forms.values.toList()
                val isUniform = sizes.isEmpty() || sizes.all {
                    it.baseHeight == sizes[0].baseHeight &&
                    it.baseWeight == sizes[0].baseWeight &&
                    it.maxHeight == sizes[0].maxHeight
                }
                SpeciesFormData(name, forms, isUniform)
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /** Get size data for a species by dex number and optional form suffix (e.g. "alolan"). */
    fun getSize(dex: Int, formSuffix: String? = null): SpeciesFormEntry? {
        val entry = data[dex.toString()] ?: return null
        val formKey = formSuffix ?: ""
        // Try exact form key first, then any form iff all forms are identical
        val size = entry.forms[formKey]
            ?: if (entry.isUniform) entry.forms.values.firstOrNull() else null
            ?: return null
        return SpeciesFormEntry(
            dex = dex,
            name = entry.name,
            size = size
        )
    }

    /** Search by name or dex number. Returns dex numbers of matches. */
    fun search(query: String): List<Int> {
        if (query.isBlank()) return data.keys.mapNotNull { it.toIntOrNull() }.sorted()
        val q = query.trim().lowercase()
        return data.entries
            .filter { (dex, entry) ->
                dex == q ||
                entry.name.lowercase().contains(q) ||
                dex.startsWith(q)
            }
            .mapNotNull { it.key.toIntOrNull() }
            .sorted()
    }
}

data class SpeciesFormEntry(
    val dex: Int,
    val name: String,
    val size: SpeciesSize
)
