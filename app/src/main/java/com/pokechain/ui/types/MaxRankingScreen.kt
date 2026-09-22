package com.pokechain.ui.types

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pokechain.data.dialgadex.MaxBattleScrapingEngine
import com.pokechain.data.dialgadex.NameTranslator
import com.pokechain.data.dialgadex.PokemonTypeProvider
import com.pokechain.data.models.AppLanguage
import com.pokechain.data.models.PvERankingEntry
import com.pokechain.data.models.PokemonType
import com.pokechain.data.models.Strings
import com.pokechain.ui.components.PokemonRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaxRankingScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val engine = remember { MaxBattleScrapingEngine() }
    val translator = remember { NameTranslator(context) }
    val typeProvider = remember { PokemonTypeProvider() }

    var showDropdown by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf<PokemonType?>(null) }
    var results by remember { mutableStateOf<List<PvERankingEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        typeProvider.ensureLoaded()
    }

    LaunchedEffect(selectedType, refreshKey) {
        val type = selectedType ?: return@LaunchedEffect
        isLoading = true
        error = null
        try {
            results = engine.computeMaxByType(type.nameEn.lowercase(), 10)
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

                selectedType != null -> {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(results, key = { it.originalRank to it.name to it.form }) { entry ->
                            val moveset = buildString {
                                append(entry.fm?.let { translator.getMoveName(it, language) } ?: "-")
                                append(" / ")
                                append(entry.cm?.let { translator.getMoveName(it, language) } ?: "-")
                            }
                            PokemonRow(
                                rank = entry.originalRank,
                                name = cleanMaxName(entry),
                                score = "Max Damage\n${"%.1f".format(entry.rat)}",
                                subtitle = moveset,
tags = listOfNotNull(
                                if (entry.form == "Gigantamax") Strings.tagGmax(language) else null
                            ),
                                spriteUrl = typeProvider.resolveSpriteUrlForMax(entry.id, entry.name, entry.form)
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