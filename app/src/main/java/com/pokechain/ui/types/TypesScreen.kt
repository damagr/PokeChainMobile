package com.pokechain.ui.types

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pokechain.data.dialgadex.NameTranslator
import com.pokechain.data.dialgadex.PokemonTypeEntry
import com.pokechain.data.dialgadex.PokemonTypeProvider
import com.pokechain.data.models.AppLanguage
import com.pokechain.data.models.PokemonType
import com.pokechain.data.models.PvPLeague
import com.pokechain.data.models.Strings
import com.pokechain.data.models.TypeChart
import com.pokechain.data.pvpoke.PvPokeApi
import com.pokechain.data.pvpoke.GameMasterResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypesScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val translator = remember { NameTranslator(context) }
    val typeProvider = remember { PokemonTypeProvider() }

    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
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

    val suggestions = remember(textFieldValue.text, typeProvider.isLoaded) {
        if (!typeProvider.isLoaded) emptyList()
        else typeProvider.searchAll(textFieldValue.text, translator, language).take(30)
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
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        showDropdown = true
                        selectedEntry = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable),
                    placeholder = { Text(Strings.typeSearch(language)) },
                    singleLine = true,
                    trailingIcon = {
                        if (textFieldValue.text.isNotEmpty()) {
                            IconButton(onClick = {
                                textFieldValue = TextFieldValue("")
                                selectedEntry = null
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
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
                                val name = entry.displayName(language, translator)
                                textFieldValue = TextFieldValue(name, selection = TextRange(name.length))
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

    // ── PvP rankings state ──────────────────────────────────────
    var pvpRanks by remember { mutableStateOf<List<PvpLeagueRank>>(emptyList()) }
    var isPvpLoading by remember { mutableStateOf(false) }
    var pvpError by remember { mutableStateOf<String?>(null) }

    // ── PvE rankings state ──────────────────────────────────────
    // (derived from pveCache — see below)

    // ── Fetch PvP: all 32 leagues, dual normal + shadow ─────────
    LaunchedEffect(entry.speciesId) {
        isPvpLoading = true
        pvpError = null
        try {
            val gm = PvPokeApi.fetchGameMaster()
            val targetDex = entry.dex
            val allLeagues = PvPLeague.entries

            // Phase 1: 3 main leagues in parallel
            val mainLeagues = listOf(PvPLeague.GREAT, PvPLeague.ULTRA, PvPLeague.MASTER)
            val mainRanks = coroutineScope {
                mainLeagues.map { league ->
                    async { fetchLeagueRank(league, gm, targetDex) }
                }.awaitAll()
            }

            // Phase 2: remaining 29 cups in parallel
            val cupLeagues = allLeagues.filter { it !in mainLeagues }
            val cupRanks = coroutineScope {
                cupLeagues.map { league ->
                    async { fetchLeagueRank(league, gm, targetDex) }
                }.awaitAll()
            }

            pvpRanks = (mainRanks + cupRanks)
                .filter { it.normalRank != null || it.shadowRank != null }
                .sortedBy { it.league.ordinal }
        } catch (e: Exception) {
            pvpError = e.message
        } finally {
            isPvpLoading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Pokémon card: sprite + name + dex + types
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/home/${entry.spriteId}.png",
                    contentDescription = displayName,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
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
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        entry.types.forEach { type ->
                            TypeBadge(type = type, language = language)
                        }
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

        // ── PvP Card ────────────────────────────────────────────
        PvpRankingsCard(
            ranks = pvpRanks,
            isLoading = isPvpLoading,
            error = pvpError,
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

// ── PvP Rankings Card ───────────────────────────────────────────────

data class PvpLeagueRank(
    val league: PvPLeague,
    val normalRank: Int?,   // 1..100 or null
    val shadowRank: Int?    // 1..100 or null
)

@Composable
private fun PvpRankingsCard(
    ranks: List<PvpLeagueRank>,
    isLoading: Boolean,
    error: String?,
    language: AppLanguage
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "${Strings.pvpLeagues(language)} (${Strings.pvpTopN(language)})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(8.dp))

            when {
                isLoading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            text = Strings.pvpLoading(language),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                error != null -> {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                ranks.isEmpty() && !isLoading -> {
                    Text(
                        text = Strings.pvpOut(language),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 130.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        ranks.forEach { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                            Text(
                                text = Strings.leagueName(entry.league, language),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            entry.normalRank?.let { r ->
                                Text(
                                    text = "#$r",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }
                            entry.shadowRank?.let { r ->
                                Text(
                                    text = when (language) {
                                        AppLanguage.EN -> "Shadow #$r"
                                        AppLanguage.ES -> "Oscuro #$r"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PokemonType.POISON.color,
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

// ── Helper: fetch rank for a single league (normal + shadow) ───────

private suspend fun fetchLeagueRank(
    league: PvPLeague,
    gm: GameMasterResponse,
    targetDex: Int
): PvpLeagueRank {
    return try {
        val raw = PvPokeApi.fetchRankings(league.cp, league.cup)
        val normalIdx = raw.indexOfFirst {
            !it.speciesId.endsWith("_shadow") && resolveDex(it.speciesId, gm) == targetDex
        }
        val shadowIdx = raw.indexOfFirst {
            it.speciesId.endsWith("_shadow") && resolveDex(it.speciesId, gm) == targetDex
        }
        PvpLeagueRank(
            league = league,
            normalRank = if (normalIdx >= 0) (normalIdx + 1).takeIf { it <= 100 } else null,
            shadowRank = if (shadowIdx >= 0) (shadowIdx + 1).takeIf { it <= 100 } else null
        )
    } catch (_: Exception) {
        PvpLeagueRank(league, null, null)
    }
}

// ── Helper: resolve dex from a ranking entry speciesId ────────────

private fun resolveDex(speciesId: String, gm: GameMasterResponse): Int? {
    val cleanId = speciesId.removeSuffix("_shadow")
    return gm.pokemon.find { it.speciesId == cleanId }?.dex
}
