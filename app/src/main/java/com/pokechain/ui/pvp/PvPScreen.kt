package com.pokechain.ui.pvp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pokechain.data.dialgadex.NameTranslator
import com.pokechain.data.models.*
import com.pokechain.data.pvpoke.*
import com.pokechain.domain.PvPFilterUseCase
import com.pokechain.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun PvPScreen(language: AppLanguage = AppLanguage.ES) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val translator = remember { NameTranslator(context) }
    var filters by remember { mutableStateOf(PvPFilterParams()) }
    var results by remember { mutableStateOf<List<PvPResult>>(emptyList()) }
    var searchString by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var progressMessage by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var topCountText by remember { mutableStateOf(filters.count.toString()) }
    var cachedBaseDexes by remember { mutableStateOf<List<Int>>(emptyList()) }
    var cachedLeague by remember { mutableStateOf(PvPLeague.GREAT) }
    var cachedIncludeShadow by remember { mutableStateOf(false) }

    LaunchedEffect(language) {
        if (cachedBaseDexes.isEmpty()) return@LaunchedEffect
        val names = cachedBaseDexes.distinct().map { translator.getName(it, language) }
        val prefix = if (cachedLeague == PvPLeague.MASTER) {
            "4*;3*&"
        } else {
            val cp = cachedLeague.cp
            when (language) {
                AppLanguage.EN -> "cp-$cp&-1attack&3-defense&3-hp&"
                AppLanguage.ES -> "PC-$cp&3-4puntos de salud&3-4defensa&0-1ataque&"
            }
        }
        val shadowTag = if (cachedIncludeShadow && cachedLeague != PvPLeague.MASTER) {
            when (language) {
                AppLanguage.EN -> "shadow&"
                AppLanguage.ES -> "oscuro&"
            }
        } else ""
        val pokemonPart = names.joinToString(";") { "+$it" }
        searchString = "$prefix$shadowTag$pokemonPart&!#"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        LeagueSelector(
            selected = filters.league,
            language = language,
            onSelect = { filters = filters.copy(league = it) }
        )

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { showFilters = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(Strings.filters(language))
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = topCountText,
            onValueChange = {
                topCountText = it
                it.toIntOrNull()?.let { n -> filters = filters.copy(count = n) }
            },
            label = { Text(Strings.topCount(language)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    loading = true
                    error = null
                    showErrorDialog = false
                    val progressStages = Strings.pvpProgress
                    var stageIdx = 0
                    fun advanceStage() {
                        if (stageIdx < progressStages.size) {
                            val stage = progressStages[stageIdx]
                            progress = stage.first
                            progressMessage = stage.second(language)
                            stageIdx++
                        }
                    }
                    try {
                        advanceStage()
                        val api = com.pokechain.data.pvpoke.PvPokeApi
                        val gameMaster = api.fetchGameMaster()

                        advanceStage()
                        val rankings = api.fetchRankings(filters.league.cp)

                        advanceStage()
                        val processor = PvPDataProcessor(gameMaster)
                        val useCase = PvPFilterUseCase(processor)
                        val filtered = useCase.execute(rankings, filters)
                        results = filtered

                        advanceStage()
                        cachedBaseDexes = filtered.mapNotNull { processor.traceBaseDex(it) }
                        cachedLeague = filters.league
                        cachedIncludeShadow = filters.includeShadow

                        advanceStage()
                        val names = cachedBaseDexes.distinct().map { translator.getName(it, language) }
                        val prefix = if (cachedLeague == PvPLeague.MASTER) {
                            "4*;3*&"
                        } else {
                            val cp = cachedLeague.cp
                            when (language) {
                                AppLanguage.EN -> "cp-$cp&-1attack&3-defense&3-hp&"
                                AppLanguage.ES -> "PC-$cp&3-4puntos de salud&3-4defensa&0-1ataque&"
                            }
                        }
                        val shadowTag = if (cachedIncludeShadow && cachedLeague != PvPLeague.MASTER) {
                            when (language) {
                                AppLanguage.EN -> "shadow&"
                                AppLanguage.ES -> "oscuro&"
                            }
                        } else ""
                        val pokemonPart = names.joinToString(";") { "+$it" }
                        searchString = "$prefix$shadowTag$pokemonPart&!#"

                        advanceStage()
                    } catch (e: Exception) {
                        error = "${e::class.simpleName}: ${e.message}\n\n${e.stackTraceToString()}"
                        showErrorDialog = true
                    } finally {
                        loading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(Strings.generate(language))
        }

        Spacer(Modifier.height(8.dp))

        if (loading) {
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = progressMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (searchString.isNotBlank()) {
            Text(
                text = Strings.topPvP(language, results.size, Strings.leagueName(filters.league, language)),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            SearchStringCard(searchString = searchString, language = language)
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(results) { index, result ->
                PokemonRow(
                    rank = index + 1,
                    name = result.speciesName,
                    score = result.score.toString(),
                    subtitle = result.moveset.joinToString(", ") { translator.getMoveName(it, language) },
                    tags = listOfNotNull(
                        if (result.isShadow) Strings.tagShadow(language) else null,
                        if (result.needsXL) Strings.tagXL(language) else null,
                        if (result.hasEliteMove) Strings.tagElite(language) else null
                    )
                )
            }
        }
    }

    if (showFilters) {
        FilterBottomSheet(
            filters = filters,
            language = language,
            onDismiss = { showFilters = false },
            onApply = { newFilters ->
                filters = newFilters
                showFilters = false
            }
        )
    }

    if (showErrorDialog && error != null) {
        ErrorDialog(
            title = Strings.errorTitle("PvP", language),
            message = error!!,
            language = language,
            onDismiss = { showErrorDialog = false }
        )
    }
}
