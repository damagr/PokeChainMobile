package com.pokechain.data.version

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("name") val name: String,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("body") val body: String?,
    @SerialName("prerelease") val prerelease: Boolean,
    @SerialName("assets") val assets: List<GitHubReleaseAsset>? = null
)

@Serializable
data class GitHubReleaseAsset(
    @SerialName("name") val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String
)

class VersionChecker {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdate(currentVersion: String, repoOwner: String = "damagr", repoName: String = "PokeChainMobile"): VersionCheckResult {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.github.com/repos/$repoOwner/$repoName/releases"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext VersionCheckResult.Error("HTTP ${response.code}")
                }

                val body = response.body?.string() ?: return@withContext VersionCheckResult.Error("Empty response")
                val releases: List<GitHubRelease> = json.decodeFromString(body)

                val current = currentVersion.removePrefix("v")

                val latestRelease = selectLatest(releases)

                if (latestRelease == null) {
                    return@withContext VersionCheckResult.UpToDate
                }

                val latestVersion = latestRelease.tagName.removePrefix("v")

                if (compareVersions(latestVersion, current) > 0) {
                    val apkUrl = latestRelease.assets
                        ?.find { it.name.endsWith(".apk") }
                        ?.browserDownloadUrl
                    VersionCheckResult.UpdateAvailable(latestVersion, latestRelease.htmlUrl, latestRelease.name, latestRelease.body, apkUrl)
                } else {
                    VersionCheckResult.UpToDate
                }
            } catch (e: Exception) {
                VersionCheckResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Comparador por pares de versiones ("1.9.0" vs "1.8.14").
     * NOTA: no usar como key de maxByOrNull contra una constante — devolvería
     * el mismo signo para todas las versiones del mismo major y daría la
     * primera de la lista, no la más alta (bug histórico corregido).
     */
    internal fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split("-")[0].split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split("-")[0].split(".").map { it.toIntOrNull() ?: 0 }
        val maxLength = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLength) {
            val p1 = if (i < parts1.size) parts1[i] else 0
            val p2 = if (i < parts2.size) parts2[i] else 0
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }
}

/**
 * Clave de ordenación por versión como Long: 1.9.0 → 1_009_000, 1.8.14 → 1_008_014.
 * Long es Comparable → usable como key de maxByOrNull.
 * [1,9,0] > [1,8,14] correcto aunque el patch de 1.8.14 sea mayor.
 */
internal fun versionKey(tagName: String): Long {
    val parts = tagName.removePrefix("v").split("-")[0].split(".").map { it.toIntOrNull() ?: 0 }
    val major = parts.getOrElse(0) { 0 }
    val minor = parts.getOrElse(1) { 0 }
    val patch = parts.getOrElse(2) { 0 }
    return major * 1_000_000L + minor * 1_000L + patch
}

/**
 * Selecciona la release estable (no prerelease) con la versión más alta,
 * independiente del orden en que GitHub devuelva la lista.
 */
internal fun selectLatest(releases: List<GitHubRelease>): GitHubRelease? =
    releases
        .filter { !it.prerelease && it.tagName.isNotBlank() }
        .maxByOrNull { versionKey(it.tagName) }

sealed interface VersionCheckResult {
    data class UpdateAvailable(
        val latestVersion: String,
        val releaseUrl: String,
        val releaseName: String,
        val releaseNotes: String?,
        val apkDownloadUrl: String? = null
    ) : VersionCheckResult
    object UpToDate : VersionCheckResult
    data class Error(val message: String) : VersionCheckResult
}