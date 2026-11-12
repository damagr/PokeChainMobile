package com.pokechain.data.dialgadex

import com.pokechain.data.models.PvERankingEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

/** Fuente de datos de los rankings Max (permite usar un falso en los tests). */
fun interface MaxBattleSource {
    suspend fun fetch(type: String, count: Int): List<PvERankingEntry>
}

/**
 * Carga del Top por Max con respaldo local.
 *
 * La pantalla usa dos pasos independientes:
 *  1. [localEntries]: datos guardados en la app (instantáneos, sin red).
 *  2. [refresh]: actualización desde Dittobase, con un deadline propio para
 *     que la red nunca pueda dejar la carga bloqueada.
 *
 * La lógica vive aquí (y no en la UI) para poder testearla sin Compose.
 */
class MaxBattleLoader(
    private val source: MaxBattleSource,
    private val snapshot: MaxBattleSnapshot?,
    /** Deadline de la actualización (protección extra sobre el del motor). */
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS
) {

    sealed interface Result {
        /** Datos descargados de Dittobase. */
        data class Live(val entries: List<PvERankingEntry>) : Result

        /** No se pudieron obtener datos frescos. */
        data class Failed(val message: String, val isNetwork: Boolean) : Result
    }

    /** Datos guardados en la app para ese tipo (sin tocar la red). */
    fun localEntries(type: String, count: Int = 10): List<PvERankingEntry> =
        snapshot?.entriesFor(type).orEmpty().take(count)

    /** Fecha del snapshot local (vacía si no hay snapshot). */
    val snapshotDate: String get() = snapshot?.generatedAtLabel.orEmpty()

    /** ¿Hay snapshot local disponible? */
    val hasSnapshot: Boolean get() = snapshot != null

    /**
     * Actualiza el ranking desde la red. Nunca se queda bloqueada: si la
     * fuente no responde, el deadline la corta y devuelve [Result.Failed].
     */
    suspend fun refresh(type: String, count: Int = 10): Result {
        val key = type.trim().lowercase()
        return try {
            val live = withTimeoutOrNull(timeoutMs) { source.fetch(key, count) }
                ?: throw IOException("Timeout de ${timeoutMs / 1000}s")
            if (live.isNotEmpty()) {
                Result.Live(live)
            } else {
                Result.Failed(NO_DATA, isNetwork = false)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failed(e.message ?: NETWORK_ERROR, isNetwork = true)
        }
    }

    companion object {
        const val NO_DATA = "NO_DATA"
        const val NETWORK_ERROR = "NETWORK_ERROR"
        const val DEFAULT_TIMEOUT_MS = 8_000L
    }
}
