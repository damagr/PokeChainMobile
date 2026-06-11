package com.pokechain.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pokechain.ui.pve.PvEScreen
import com.pokechain.ui.pvp.PvPScreen
import com.pokechain.ui.components.LanguageSelector
import com.pokechain.data.models.AppLanguage
import com.pokechain.data.models.Strings
import com.pokechain.data.version.VersionCheckResult
import com.pokechain.data.version.VersionChecker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    var language by remember { mutableStateOf(AppLanguage.ES) }
    val tabs = listOf("PvP", "PvE")
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text("PokeChain")
                        Text(
                            text = "v$versionName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    LanguageSelector(
                        selected = language,
                        onSelect = { language = it }
                    )
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            when (selectedTab) {
                0 -> PvPScreen(language = language)
                1 -> PvEScreen(language = language)
            }
        }
    }

    if (showUpdateDialog && updateInfo != null) {
        UpdateDialog(
            info = updateInfo!!,
            currentVersion = versionName,
            language = language,
            onDismiss = { showUpdateDialog = false },
            onDownload = {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(updateInfo!!.releaseUrl)
                )
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                showUpdateDialog = false
            }
        )
    }
}

@Composable
fun UpdateDialog(
    info: VersionCheckResult.UpdateAvailable,
    currentVersion: String,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.newVersionAvailable(language, info.latestVersion)) },
        text = {
            Column {
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
        },
        confirmButton = {
            TextButton(onClick = onDownload) {
                Text(Strings.download(language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.later(language))
            }
        }
    )
}
