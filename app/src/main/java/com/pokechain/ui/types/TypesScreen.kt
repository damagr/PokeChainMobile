package com.pokechain.ui.types

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pokechain.data.dialgadex.NameTranslator
import com.pokechain.data.dialgadex.PokemonTypeEntry
import com.pokechain.data.dialgadex.PokemonTypeProvider
import com.pokechain.data.models.AppLanguage
import com.pokechain.data.models.PokemonType
import com.pokechain.data.models.Strings
import com.pokechain.data.models.TypeChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypesScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val translator = remember { NameTranslator(context) }
    val typeProvider = remember { PokemonTypeProvider() }

    var searchQuery by remember { mutableStateOf("") }
    var showDropdown by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<PokemonTypeEntry?>(null) }
    var isLoadingTypes by remember { mutableStateOf(true) }
    var typeLoadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            typeProvider.ensureLoaded()
        } catch (e: Exception) {
            typeLoadError = when (language) {
                AppLanguage.EN -> "Failed to load data: ${e.message}"
                AppLanguage.ES -> "Error al cargar datos: ${e.message}"
            }
        } finally {
            isLoadingTypes = false
        }
    }

    val suggestions = remember(searchQuery, typeProvider.isLoaded) {
        if (!typeProvider.isLoaded) emptyList()
        else typeProvider.searchAll(searchQuery, translator, language).take(30)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = Strings.back(language)
                        )
                    }
                },
                title = { Text(Strings.typesSection(language)) }
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
                expanded = showDropdown && suggestions.isNotEmpty() && selectedEntry == null,
                onExpandedChange = { showDropdown = it }
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { newValue ->
                        searchQuery = newValue
                        showDropdown = true
                        selectedEntry = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable),
                    placeholder = { Text(Strings.typeSearch(language)) },
                    singleLine = true
                )

                ExposedDropdownMenu(
                    expanded = showDropdown && suggestions.isNotEmpty() && selectedEntry == null,
                    onDismissRequest = { showDropdown = false }
                ) {
                    suggestions.forEach { entry ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                // Name on the left
                                val displayName = entry.displayName(language, translator)
                                Text(
                                    text = displayName,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                    // Types + number on the right
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        entry.types.forEach { t ->
                                            Surface(
                                                color = t.color,
                                                shape = RoundedCornerShape(3.dp)
                                            ) {
                                                Text(
                                                    text = t.displayName(language),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                    color = androidx.compose.ui.graphics.Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            text = "#${entry.dex}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            onClick = {
                                searchQuery = entry.displayName(language, translator)
                                selectedEntry = entry
                                showDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            when {
                isLoadingTypes -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = when (language) {
                                    AppLanguage.EN -> "Downloading Pokémon data…"
                                    AppLanguage.ES -> "Descargando datos de Pokémon…"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                typeLoadError != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = typeLoadError!!,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                selectedEntry != null -> {
                    PokemonTypeDetail(
                        entry = selectedEntry!!,
                        language = language
                    )
                }

                typeProvider.isLoaded && selectedEntry == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Strings.noPokemonSelected(language),
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

@Composable
private fun PokemonTypeDetail(
    entry: PokemonTypeEntry,
    language: AppLanguage
) {
    val context = LocalContext.current
    val translator = remember { NameTranslator(context) }
    val displayName = remember(entry.speciesId, language) {
        entry.displayName(language, translator)
    }

    val (resistances, weaknesses) = remember(entry.types) {
        TypeChart.getEffectiveness(entry.types)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Pokémon card: name left, dex top-right, types bottom-left
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Top row: name left, dex right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "#${entry.dex}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
                // Types bottom-left
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    entry.types.forEach { type ->
                        TypeBadge(type = type, language = language)
                    }
                }
            }
        }

        SectionCard(
            title = Strings.resistantTo(language),
            types = resistances,
            language = language
        )

        SectionCard(
            title = Strings.weakTo(language),
            types = weaknesses,
            language = language
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectionCard(
    title: String,
    types: List<PokemonType>,
    language: AppLanguage
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            if (types.isEmpty()) {
                Text(
                    text = when (language) {
                        AppLanguage.EN -> "None"
                        AppLanguage.ES -> "Ninguno"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    types.forEach { type ->
                        TypeBadge(type = type, language = language)
                    }
                }
            }
        }
    }
}

@Composable
fun TypeBadge(
    type: PokemonType,
    language: AppLanguage
) {
    Surface(
        color = type.color,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = type.displayName(language),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = androidx.compose.ui.graphics.Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
