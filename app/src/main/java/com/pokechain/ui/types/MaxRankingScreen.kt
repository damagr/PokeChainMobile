package com.pokechain.ui.types

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pokechain.data.dialgadex.MaxBattleLoader
import com.pokechain.data.dialgadex.MaxBattleScrapingEngine
import com.pokechain.data.dialgadex.MaxBattleSnapshot
import com.pokechain.data.dialgadex.NameTranslator
import com.pokechain.data.dialgadex.PokemonTypeProvider
import com.pokechain.data.models.AppLanguage
import com.pokechain.data.models.PvERankingEntry
import com.pokechain.data.models.PokemonType
import com.pokechain.data.models.Strings
import com.pokechain.ui.components.PokemonRow
import com.pokechain.ui.rememberVersionName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaxRankingScreen(
    language: AppLanguage,
    onBack: () -> Unit,
    engine: MaxBattleScrapingEngine,
    typeProvider: PokemonTypeProvider,
    onPokemonClick: (PvERankingEntry) -> Unit = {}
) {
    val context = LocalContext.current
    val translator = remember { NameTranslator(context) }
    val scope = rememberCoroutineScope()

    var showDropdown by rememberSaveable { mutableStateOf(false) }
    var selectedType by rememberSaveable { mutableStateOf<PokemonType?>(null) }
    var results by remember { mutableStateOf<List<PvERankingEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var fromCache by remember { mutableStateOf(false) }
    var cacheDate by remember { mutableStateOf<String?>(null) }
    var cancelled by remember { mutableStateOf(false) }
    var lastError by remember { mutableStateOf<String?>(null) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var loadJob by remember { mutableStateOf<Job?>(null) }
    val listState = rememberLazyListState()

    // Respaldo local de los rankings (por si dittobase no es accesible)
    val snapshot = remember { MaxBattleSnapshot.load(context) }
    val loader = remember(engine, snapshot) {
        MaxBattleLoader({ type, count -> engine.computeMaxByType(type, count) }, snapshot)
    }
    val appVersion = rememberVersionName()

    LaunchedEffect(Unit) {
        typeProvider.ensureLoaded()
    }

    LaunchedEffect(selectedType, refreshKey) {
        val type = selectedType ?: return@LaunchedEffect
        val slug = type.nameEn.lowercase()

        error = null
        lastError = null
        elapsedSeconds = 0
        cancelled = false

        // 1) Datos guardados: al instante y sin red. Si existen, la pantalla
        //    nunca puede quedarse en blanco ni con el spinner girando.
        val local = loader.localEntries(slug, 10)
        if (local.isNotEmpty()) {
            results = local
            fromCache = true
            cacheDate = loader.snapshotDate
            isLoading = false
        } else {
            results = emptyList()
            fromCache = false
            cacheDate = null
            isLoading = true
        }

        // 2) Actualización desde la fuente en segundo plano: si responde, la
        //    lista se refresca; si no, se conserva la local.
        isRefreshing = true
        loadJob = launch {
            val ticker = launch {
                while (true) {
                    delay(1000)
                    elapsedSeconds++
                }
            }
            try {
                when (val result = loader.refresh(slug, 10)) {
                    is MaxBattleLoader.Result.Live -> {
                        results = result.entries
                        fromCache = false
                        cacheDate = null
                        lastError = null
                    }
                    is MaxBattleLoader.Result.Failed -> {
                        lastError = result.message
                        // Sin datos guardados y sin red: solo queda el error
                        if (results.isEmpty()) {
                            error = if (result.isNetwork) {
                                Strings.maxRankingNoConnection(language)
                            } else {
                                Strings.maxRankingNoData(language)
                            }
                        }
                    }
                }
                if (results.isNotEmpty()) listState.scrollToItem(0)
            } catch (e: CancellationException) {
                // Botón Cancelar o salir de la pantalla: no es un error
                cancelled = true
                throw e
            } finally {
                ticker.cancel()
                isLoading = false
                isRefreshing = false
            }
        }
        loadJob?.invokeOnCompletion { loadJob = null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.back(language))
                    }
                },
                title = {
                    Text(
                        text = Strings.maxRankingSection(language),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = showDropdown,
                onExpandedChange = { showDropdown = it }
            ) {
                OutlinedTextField(
                    value = selectedType?.displayName(language) ?: "",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                    placeholder = { Text(Strings.maxRankingPickType(language)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdown) }
                )
                ExposedDropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false }
                ) {
                    PokemonType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName(language)) },
                            leadingIcon = { TypeBadge(type = type, language = language) },
                            onClick = {
                                selectedType = type
                                showDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = Strings.maxRankingLoading(language),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { loadJob?.cancel() }) {
                                Text(Strings.maxRankingCancel(language))
                            }
                        }
                    }
                }

                error != null -> {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = error!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = { refreshKey++ }) {
                            Text(Strings.retry(language))
                        }
                    }
                }

                selectedType != null && !isLoading && error == null && results.isEmpty() && !cancelled -> {
                    // La fuente puede cambiar de estructura o fallar sin
                    // excepción: nunca dejar la lista en blanco silencioso
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = Strings.maxRankingNoData(language),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                        TextButton(onClick = { refreshKey++ }) {
                            Text(Strings.retry(language))
                        }
                    }
                }

                selectedType != null -> {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        item(key = "status-banner") {
                            MaxStatusBanner(
                                fromCache = fromCache,
                                cacheDate = cacheDate,
                                isRefreshing = isRefreshing,
                                elapsedSeconds = elapsedSeconds,
                                lastError = lastError,
                                appVersion = appVersion,
                                language = language,
                                onRefresh = { refreshKey++ },
                                onCancel = { loadJob?.cancel() },
                                onTestConnection = {
                                    scope.launch {
                                        val probe = engine.probe()
                                        lastError = probe
                                    }
                                }
                            )
                        }
                        items(results, key = { it.originalRank to it.name to it.form }) { entry ->
                            // Dittobase no muestra el fast move: subtitle = solo el Max Move
                            val moveset = entry.cm?.let { translator.getMoveName(it, language) } ?: "-"
                            PokemonRow(
                                rank = entry.originalRank,
                                name = cleanMaxName(entry),
                                score = "Max Damage\n${"%.1f".format(entry.rat)}",
                                subtitle = moveset,
                                tags = listOfNotNull(
                                    if (entry.form == "Gigantamax") Strings.tagGmax(language) else null
                                ),
                                spriteUrl = entry.spriteUrl
                                    ?: typeProvider.resolveSpriteUrlForMax(entry.id, entry.name, entry.form),
                                onClick = { onPokemonClick(entry) }
                            )
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Strings.maxRankingPickType(language),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private fun cleanMaxName(entry: PvERankingEntry): String {
    return when (entry.form) {
        "Dynamax" -> "Dynamax ${entry.name}"
        "Gigantamax" -> "Gigantamax ${entry.name}"
        "Crowned Sword", "Crowned Shield" -> "${entry.name} (${entry.form})"
        "Eternamax" -> "Eternamax ${entry.name}"
        else -> entry.name
    }
}

/**
 * Panel de estado del Top por Max: de dónde vienen los datos, si se está
 * actualizando y el último error técnico (para diagnosticar en el dispositivo
 * sin necesidad de logcat).
 */
@Composable
private fun MaxStatusBanner(
    fromCache: Boolean,
    cacheDate: String?,
    isRefreshing: Boolean,
    elapsedSeconds: Int,
    lastError: String?,
    appVersion: String,
    language: AppLanguage,
    onRefresh: () -> Unit,
    onCancel: () -> Unit,
    onTestConnection: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (fromCache) Icons.Outlined.CloudOff else Icons.Outlined.CloudDone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        isRefreshing -> Strings.maxRankingUpdating(language, elapsedSeconds)
                        fromCache -> Strings.maxRankingCached(language, cacheDate.orEmpty())
                        else -> Strings.maxRankingLive(language)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!lastError.isNullOrBlank()) {
                    Text(
                        text = lastError,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (lastError.startsWith("HTTP 2")) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
                Text(
                    text = Strings.maxRankingVersion(language, appVersion),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isRefreshing) {
                TextButton(onClick = onCancel) {
                    Text(Strings.maxRankingCancel(language))
                }
            } else {
                TextButton(onClick = onRefresh) {
                    Text(Strings.maxRankingRefresh(language))
                }
            }
        }
        TextButton(onClick = onTestConnection) {
            Text(Strings.maxRankingTestConnection(language))
        }
    }
}