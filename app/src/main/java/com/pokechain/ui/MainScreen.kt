package com.pokechain.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pokechain.data.models.AppLanguage
import com.pokechain.data.models.Strings
import com.pokechain.data.version.VersionCheckResult
import com.pokechain.data.version.VersionChecker
import com.pokechain.ui.types.TypesScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

private enum class Screen { HOME, CHAIN, TYPES }

@Composable
fun MainScreen() {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var language by remember { mutableStateOf(AppLanguage.ES) }

    val context = LocalContext.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
        } catch (e: Exception) {
            "?"
        }
    }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<VersionCheckResult.UpdateAvailable?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            val checker = VersionChecker()
            val result = checker.checkForUpdate(versionName)
            when (result) {
                is VersionCheckResult.UpdateAvailable -> {
                    updateInfo = result
                    showUpdateDialog = true
                }
                else -> {}
            }
        }
    }

    when (currentScreen) {
        Screen.HOME -> {
            HomeScreen(
                language = language,
                onLanguageChange = { language = it },
                onChainClick = { currentScreen = Screen.CHAIN },
                onTypesClick = { currentScreen = Screen.TYPES }
            )
        }
        Screen.CHAIN -> {
            ChainScreen(
                language = language,
                onBack = { currentScreen = Screen.HOME }
            )
        }
        Screen.TYPES -> {
            TypesScreen(
                language = language,
                onBack = { currentScreen = Screen.HOME }
            )
        }
    }

    if (showUpdateDialog && updateInfo != null) {
        UpdateDialog(
            info = updateInfo!!,
            currentVersion = versionName,
            language = language,
            isDownloading = isDownloading,
            downloadProgress = downloadProgress,
            downloadError = downloadError,
            onDismiss = {
                showUpdateDialog = false
                downloadError = null
            },
            onDownload = {
                scope.launch {
                    isDownloading = true
                    downloadProgress = 0f
                    downloadError = null
                    try {
                        val url = updateInfo!!.apkDownloadUrl
                        if (url == null) {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(updateInfo!!.releaseUrl)
                            )
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                            showUpdateDialog = false
                            return@launch
                        }
                        val file = downloadApk(url, context) { progress ->
                            downloadProgress = progress
                        }
                        downloadProgress = 1f
                        installApk(context, file)
                        showUpdateDialog = false
                    } catch (e: Exception) {
                        downloadError = e.message ?: "Unknown error"
                    } finally {
                        isDownloading = false
                    }
                }
            }
        )
    }
}

@Composable
fun UpdateDialog(
    info: VersionCheckResult.UpdateAvailable,
    currentVersion: String,
    language: AppLanguage,
    isDownloading: Boolean,
    downloadProgress: Float,
    downloadError: String?,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        title = { Text(Strings.newVersionAvailable(language, info.latestVersion)) },
        text = {
            Column {
                if (isDownloading) {
                    Text(
                        text = Strings.downloading(language),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${(downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (downloadError != null) {
                    Text(
                        text = Strings.updateFailed(language),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = downloadError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = Strings.updateLink(language),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Current: v$currentVersion\nLatest: v${info.latestVersion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    info.releaseNotes?.let { notes ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).verticalScroll(rememberScrollState())
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (downloadError != null) {
                TextButton(onClick = {
                    onDismiss()
                }) {
                    Text(Strings.close(language))
                }
            } else if (!isDownloading) {
                TextButton(onClick = onDownload) {
                    Text(Strings.download(language))
                }
            }
        },
        dismissButton = {
            if (downloadError != null) {
                TextButton(onClick = onDownload) {
                    Text(Strings.download(language))
                }
            } else if (!isDownloading) {
                TextButton(onClick = onDismiss) {
                    Text(Strings.later(language))
                }
            }
        }
    )
}

private suspend fun downloadApk(url: String, context: android.content.Context, onProgress: (Float) -> Unit): java.io.File {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        val body = response.body ?: throw Exception("Empty response")
        val contentLength = body.contentLength()
        val inputStream = body.byteStream()

        val dir = java.io.File(context.cacheDir, "apk")
        dir.mkdirs()
        val file = java.io.File(dir, "update.apk")

        file.outputStream().use { output ->
            val buffer = ByteArray(8192)
            var bytesRead: Long = 0
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
                bytesRead += read
                if (contentLength > 0) {
                    withContext(Dispatchers.Main) {
                        onProgress(bytesRead.toFloat() / contentLength)
                    }
                }
            }
        }
        file
    }
}

private fun installApk(context: android.content.Context, file: java.io.File) {
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
