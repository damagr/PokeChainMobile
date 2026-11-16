package com.pokechain.data.pvpoke

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object PvPokeApi {
    private const val TAG = "PvPokeApi"
    private const val BASE_URL = "https://pvpoke.com/data"
    private const val GAMEMASTER_URL = "$BASE_URL/gamemaster.min.json"
    private const val RANKINGS_URL = "$BASE_URL/rankings/%s/overall/rankings-%d.json"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        // Deadline global: con la red rota la llamada puede quedarse colgada
        // en la resolucion DNS, que connect/readTimeout no cubren
        .callTimeout(45, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    // Cache de rankings: los mismos ficheros sirven para cualquier Pokémon seleccionado
    // (la clave identifica copa+liga). Se vacía al reiniciar el proceso.
    private val rankingsCache = ConcurrentHashMap<String, List<PvPRawEntry>>()

    /**
     * GameMaster de PvPoke.
     *
     * Devuelve null si la descarga falla (sin red, timeout, HTTP error): NUNCA
     * lanza, porque un fallo de red no debe tumbar la app (la llamada se hace
     * desde LaunchedEffect, donde una excepcion no controlada cierra la app).
     */
    suspend fun fetchGameMaster(): GameMasterResponse? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(GAMEMASTER_URL).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "HTTP ${response.code} al descargar el gamemaster")
                    return@withContext null
                }
                val body = response.body?.string() ?: return@withContext null
                json.decodeFromString(body)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallo al descargar el gamemaster: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    /**
     * Rankings de una liga. Devuelve null si la descarga falla (nunca lanza).
     */
    suspend fun fetchRankings(cp: Int, cup: String? = null): List<PvPRawEntry>? = withContext(Dispatchers.IO) {
        val key = "${cup ?: "all"}-$cp"
        rankingsCache[key]?.let { return@withContext it }
        try {
            val url = RANKINGS_URL.format(cup ?: "all", cp)
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "HTTP ${response.code} al descargar los rankings de $key")
                    return@withContext null
                }
                val body = response.body?.string() ?: return@withContext null
                val parsed: List<PvPRawEntry> = json.decodeFromString(body)
                rankingsCache[key] = parsed
                parsed
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallo al descargar los rankings de $key: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }
}
