package com.pokechain.data.dialgadex

import android.content.Context
import com.pokechain.data.models.PvERankingEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Snapshot local de los rankings de Combates Max (Dynamax/Gigantamax).
 *
 * Se genera con `tools/generate_max_snapshot.py` a partir de Dittobase y se
 * empaqueta en los assets de la app. Sirve de respaldo: si el dispositivo no
 * puede alcanzar dittobase.com (DNS, VPN, operador), la pantalla del Top por
 * Max sigue mostrando datos en vez de quedarse cargando.
 */
class MaxBattleSnapshot private constructor(
    val generatedAt: String,
    private val byType: Map<String, List<PvERankingEntry>>
) {

    /** Fecha de generación del snapshot (ISO-8601, UTC). */
    val generatedAtLabel: String get() = generatedAt.substringBefore("T")

    /** Entradas del snapshot para un tipo (vacío si no hay datos para él). */
    fun entriesFor(type: String): List<PvERankingEntry> =
        byType[type.lowercase()].orEmpty()

    /** Tipos disponibles en el snapshot. */
    val availableTypes: Set<String> get() = byType.keys

    companion object {
        private const val ASSET_NAME = "max_battles_snapshot.json"

        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Carga el snapshot desde los assets. Devuelve null si el asset no
         * existe o está corrupto (para no romper la pantalla).
         */
        fun load(context: Context): MaxBattleSnapshot? = try {
            val raw = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
            fromJson(raw)
        } catch (e: Exception) {
            null
        }

        /** Parsea el snapshot desde su JSON (usado por load y por los tests). */
        internal fun fromJson(raw: String): MaxBattleSnapshot? = try {
            val parsed = json.decodeFromString(SnapshotFile.serializer(), raw)
            val entries = parsed.types.mapValues { (_, rows) -> rows.map { it.toEntry() } }
            MaxBattleSnapshot(parsed.generatedAt, entries)
        } catch (e: Exception) {
            null
        }
    }
}

@Serializable
private data class SnapshotFile(
    val generatedAt: String = "",
    val source: String = "",
    val types: Map<String, List<SnapshotEntry>> = emptyMap()
)

@Serializable
private data class SnapshotEntry(
    val rank: Int = 0,
    val dex: Int = 0,
    val name: String = "",
    val form: String = "Normal",
    val cm: String = "",
    val rat: Double = 0.0,
    val spriteUrl: String = ""
) {
    fun toEntry(): PvERankingEntry = PvERankingEntry(
        rat = rat,
        dps = rat,
        tdo = 0.0,
        id = dex,
        name = name,
        form = form,
        shadow = false,
        level = 40,
        unreleased = false,
        fm = null,
        fmIsElite = false,
        fmType = null,
        cm = cm,
        cmIsElite = false,
        cmType = null,
        tier = null,
        pct = null,
        originalRank = rank,
        spriteUrl = spriteUrl.ifBlank { null }
    )
}
