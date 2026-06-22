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

class VersionChecker(private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .build()) {

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

                val latestRelease = releases
                    .filter { !it.prerelease && !it.tagName.isNullOrBlank() }
                    .maxByOrNull { compareVersions(it.tagName.removePrefix("v"), "0") }

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

    private fun compareVersions(v1: String, v2: String): Int {
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