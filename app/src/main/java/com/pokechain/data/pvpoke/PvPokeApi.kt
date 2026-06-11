package com.pokechain.data.pvpoke

import com.pokechain.data.models.Move
import com.pokechain.data.models.Pokemon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object PvPokeApi {
    private const val BASE_URL = "https://pvpoke.com/data"
    private const val GAMEMASTER_URL = "$BASE_URL/gamemaster.min.json"
    private const val RANKINGS_URL = "$BASE_URL/rankings/all/overall/rankings-%d.json"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchGameMaster(): GameMasterResponse = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(GAMEMASTER_URL).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        json.decodeFromString(body)
    }

    suspend fun fetchRankings(cp: Int): List<PvPRawEntry> = withContext(Dispatchers.IO) {
        val url = RANKINGS_URL.format(cp)
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        json.decodeFromString(body)
    }
}
