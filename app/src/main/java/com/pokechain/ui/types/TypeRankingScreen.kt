package com.pokechain.ui.types

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pokechain.data.dialgadex.NameTranslator
import com.pokechain.data.dialgadex.PokemonTypeEntry
import com.pokechain.data.dialgadex.PokemonTypeProvider
import com.pokechain.data.dialgadex.PvEScrapingEngine
import com.pokechain.data.models.AppLanguage
import com.pokechain.data.models.PvERankingEntry
import com.pokechain.data.models.PokemonType
import com.pokechain.data.models.Strings
import com.pokechain.ui.components.PokemonRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeRankingScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val engine = remember { PvEScrapingEngine(context as android.app.Activity) }
    val scope = rememberCoroutineScope()
    val translator = remember { NameTranslator(context) }
    val typeProvider = remember { PokemonTypeProvider() }

    var showDropdown by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf<PokemonType?>(null) }
    var isGlobal by remember { mutableStateOf(true) }
    var showMega by remember { mutableStateOf(true) }
    var results by remember { mutableStateOf<List<PvERankingEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        engine.init()
        typeProvider.ensureLoaded()
    }

    LaunchedEffect(selectedType, isGlobal, showMega, refreshKey) {
        isLoading = true
        error = null
        try {
            val typeKey = if (isGlobal) "Any" else selectedType?.nameEn
            if (typeKey == null) return@LaunchedEffect
            val count = if (isGlobal) 200 else 50
            val raw = engine.computeByType(typeKey, count)
            val filtered = if (showMega) raw else raw.filter { !it.form.startsWith("Mega") }
            results = filtered.mapIndexed { index, entry -> entry.copy(originalRank = index + 1) }
            // Reset scroll to top after new results are loaded
            listState.scrollToItem(0)
        } catch (e: Exception) {
            error = e.message
            results = emptyList()
            listState.scrollToItem(0)
        } finally {
            isLoading = false
        }
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
                        text = Strings.typeRankingSection(language),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                actions = {
                    Row(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .align(Alignment.CenterVertically),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Strings.tagMega(language),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(4.dp))
                        Switch(
                            checked = showMega,
                            onCheckedChange = { showMega = !showMega }
                        )
                    }
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
                    value = if (isGlobal) Strings.typeRankingGlobal(language) else selectedType?.displayName(language) ?: "",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                    placeholder = { Text(Strings.typeRankingPickType(language)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdown) }
                )
                ExposedDropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(Strings.typeRankingGlobal(language)) },
                        onClick = {
                            isGlobal = true
                            selectedType = null
                            showDropdown = false
                        }
                    )
                    PokemonType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName(language)) },
                            leadingIcon = { TypeBadge(type = type, language = language) },
                            onClick = {
                                isGlobal = false
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
                                text = Strings.pveLoading(language),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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

                isGlobal || selectedType != null -> {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(results, key = { it.id to it.form to it.shadow }) { entry ->
                            val moveset = buildString {
                                append(entry.fm?.let { translator.getMoveName(it, language) } ?: "-")
                                if (entry.fmIsElite) append("*")
                                append(" / ")
                                append(entry.cm?.let { translator.getMoveName(it, language) } ?: "-")
                                if (entry.cmIsElite) append("*")
                            }
PokemonRow(
                                rank = entry.originalRank,
                                name = cleanName(entry, language),
                                score = "eDPS\n${"%.2f".format(entry.rat)}",
                                subtitle = entry.tier?.let { "${Strings.typeRankingTier(language, it)} — $moveset" } ?: moveset,
                                tags = listOfNotNull(
                                    if (entry.shadow) Strings.tagShadow(language) else null,
                                    if (entry.form.startsWith("Mega")) PokemonTypeEntry.translateForm(entry.form, language) else null
                                ),
                                spriteUrl = typeProvider.resolveSpriteUrl(entry.id, entry.name, entry.form, entry.shadow)
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
                            text = Strings.typeRankingPickType(language),
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

private fun cleanName(entry: PvERankingEntry, language: AppLanguage): String {
    val name = entry.name
    return when {
        entry.form == "Normal" -> name
        name.startsWith("Mega ") || name.startsWith("Primal ") -> name
        entry.form.startsWith("Mega") -> "${PokemonTypeEntry.translateForm(entry.form, language) ?: "Mega"} $name"
        else -> {
            val translated = PokemonTypeEntry.translateForm(entry.form, language) ?: entry.form
            "$name ($translated)"
        }
    }
}