package com.pokechain.ui.types

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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

    var showDropdown by rememberSaveable { mutableStateOf(false) }
    var selectedType by rememberSaveable { mutableStateOf<PokemonType?>(null) }
    var results by remember { mutableStateOf<List<PvERankingEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var fromCache by remember { mutableStateOf(false) }
    var cacheDate by remember { mutableStateOf<String?>(null) }
    var cancelled by remember { mutableStateOf(false) }
    var loadJob by remember { mutableStateOf<Job?>(null) }
    val listState = rememberLazyListState()

    // Respaldo local de los rankings (por si dittobase no es accesible)
    val snapshot = remember { MaxBattleSnapshot.load(context) }
    val loader = remember(engine, snapshot) {
        MaxBattleLoader({ type, count -> engine.computeMaxByType(type, count) }, snapshot)
    }

    LaunchedEffect(Unit) {
        typeProvider.ensureLoaded()
    }

    LaunchedEffect(selectedType, refreshKey) {
        val type = selectedType ?: return@LaunchedEffect
        isLoading = true
        error = null
        fromCache = false
        cacheDate = null
        cancelled = false
        loadJob = launch {
            try {
                when (val result = loader.load(type.nameEn.lowercase(), 10)) {
                    is MaxBattleLoader.Result.Live -> {
                        results = result.entries
                    }
                    is MaxBattleLoader.Result.Cached -> {
                        // La red no respondió: se muestran los datos empaquetados
                        results = result.entries
                        fromCache = true
                        cacheDate = result.generatedAt
                    }
                    is MaxBattleLoader.Result.Failed -> {
                        error = if (result.isNetwork) {
                            Strings.maxRankingNoConnection(language)
                        } else {
                            Strings.maxRankingNoData(language)
                        }
                        results = emptyList()
                    }
                }
                listState.scrollToItem(0)
            } catch (e: CancellationException) {
                // Botón Cancelar o salir de la pantalla: no es un error
                cancelled = true
                throw e
            } finally {
                isLoading = false
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
                        if (fromCache) {
                            item(key = "cache-banner") {
                                CacheBanner(
                                    date = cacheDate,
                                    language = language,
                                    onRetry = { refreshKey++ }
                                )
                            }
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
 * Aviso de que los datos mostrados vienen del snapshot local (la fuente en
 * línea no respondió) con opción de reintentar la descarga.
 */
@Composable
private fun CacheBanner(
    date: String?,
    language: AppLanguage,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = Strings.maxRankingCached(language, date.orEmpty()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRetry) {
            Text(Strings.retry(language))
        }
    }
}