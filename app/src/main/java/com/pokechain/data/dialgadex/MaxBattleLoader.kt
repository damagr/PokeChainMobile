package com.pokechain.data.dialgadex

import com.pokechain.data.models.PvERankingEntry
import kotlinx.coroutines.CancellationException

/** Fuente de datos de los rankings Max (permite usar un falso en los tests). */
fun interface MaxBattleSource {
    suspend fun fetch(type: String, count: Int): List<PvERankingEntry>
}

/**
 * Carga el Top por Max con respaldo local:
 *  - red OK -> [Result.Live]
 *  - red KO o timeout -> snapshot empaquetado -> [Result.Cached]
 *  - sin snapshot para ese tipo -> [Result.Failed]
 *
 * La lógica vive aquí (y no en la UI) para poder testearla sin Compose.
 */
class MaxBattleLoader(
    private val source: MaxBattleSource,
    private val snapshot: MaxBattleSnapshot?
) {

    sealed interface Result {
        /** Datos descargados de Dittobase. */
        data class Live(val entries: List<PvERankingEntry>) : Result

        /** Datos del snapshot local (la red no estaba disponible). */
        data class Cached(val entries: List<PvERankingEntry>, val generatedAt: String) : Result

        /** No hay datos ni en red ni en el snapshot. */
        data class Failed(val message: String, val isNetwork: Boolean) : Result
    }

    suspend fun load(type: String, count: Int = 10): Result {
        val key = type.trim().lowercase()
        return try {
            val live = source.fetch(key, count)
            if (live.isNotEmpty()) {
                Result.Live(live)
            } else {
                cached(key, count) ?: Result.Failed(NO_DATA, isNetwork = false)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            cached(key, count) ?: Result.Failed(e.message ?: NETWORK_ERROR, isNetwork = true)
        }
    }

    private fun cached(type: String, count: Int): Result.Cached? {
        val entries = snapshot?.entriesFor(type).orEmpty()
        if (entries.isEmpty()) return null
        return Result.Cached(entries.take(count), snapshot?.generatedAtLabel.orEmpty())
    }

    companion object {
        const val NO_DATA = "NO_DATA"
        const val NETWORK_ERROR = "NETWORK_ERROR"
    }
}
