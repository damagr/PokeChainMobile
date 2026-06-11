package com.pokechain.data.version

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("body") val body: String?,
    @SerializedName("prerelease") val prerelease: Boolean
)

class VersionChecker(private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .build()) {

    private val gson = Gson()

    suspend fun checkForUpdate(currentVersion: String, repoOwner: String = "damagr", repoName: String = "PokeChainMobile"): VersionCheckResult {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext VersionCheckResult.Error("HTTP ${response.code}")
                }

                val body = response.body?.string() ?: return@withContext VersionCheckResult.Error("Empty response")
                val release = gson.fromJson(body, GitHubRelease::class.java)

                val latestVersion = release.tagName.removePrefix("v")
                val current = currentVersion.removePrefix("v")

                if (compareVersions(latestVersion, current) > 0) {
                    VersionCheckResult.UpdateAvailable(latestVersion, release.htmlUrl, release.name, release.body)
                } else {
                    VersionCheckResult.UpToDate
                }
            } catch (e: Exception) {
                VersionCheckResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
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
        val releaseNotes: String?
    ) : VersionCheckResult
    object UpToDate : VersionCheckResult
    data class Error(val message: String) : VersionCheckResult
}