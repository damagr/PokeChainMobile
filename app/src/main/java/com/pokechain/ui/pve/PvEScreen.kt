package com.pokechain.ui.pve

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pokechain.data.dialgadex.NameTranslator
import com.pokechain.data.dialgadex.PvEScrapingEngine
import com.pokechain.data.models.*
import com.pokechain.data.pvpoke.PvPDataProcessor
import com.pokechain.data.pvpoke.PvPokeApi
import com.pokechain.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PvEScreen(language: AppLanguage = AppLanguage.ES, advancedMode: Boolean = false) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val appContext = remember { context.applicationContext }
    val translator = remember { NameTranslator(appContext) }
    val engine = remember { PvEScrapingEngine(appContext) }
    var filters by remember { mutableStateOf(PvEFilterParams()) }
    var results by remember { mutableStateOf<List<PvERankingEntry>>(emptyList()) }
    var searchString by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var progressMessage by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var topCountText by remember { mutableStateOf("") }
    var fromText by remember { mutableStateOf("") }
    var cachedBaseDexes by remember { mutableStateOf<List<Int>>(emptyList()) }
    var resultMessage by remember { mutableStateOf("") }
    var showResultMessage by remember { mutableStateOf(false) }
    var showCountWarning by remember { mutableStateOf(false) }
    var cachedFromRank by remember { mutableStateOf(1) }

    LaunchedEffect(Unit) {
        engine.init()
    }

    fun buildSearchString(names: List<String>, language: AppLanguage, shadow: Boolean): String {
        val shadowSuffix = when (language) {
            AppLanguage.ES -> "oscuro"
            AppLanguage.EN -> "shadow"
        }
        val prefix = when (language) {
            AppLanguage.ES -> "4*;3*&3-ataque&"
            AppLanguage.EN -> "4*;3*&3-attack&"
        } + if (shadow) "${shadowSuffix}&" else ""
        val namesPart = names.joinToString(";") { "+$it" }
        return "$prefix$namesPart&!#"
    }

    LaunchedEffect(language) {
        if (cachedBaseDexes.isEmpty()) return@LaunchedEffect
        val names = cachedBaseDexes.distinct().map { translator.getName(it, language) }
        searchString = buildSearchString(names, language, filters.includeShadow)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        OutlinedButton(
            onClick = { showFilters = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(Strings.filters(language))
        }

        Spacer(Modifier.height(8.dp))

        if (advancedMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = fromText,
                    onValueChange = { text ->
                        val filtered = text.filter { it.isDigit() }
                        val n = filtered.toIntOrNull()
                        if (n == null || n == 0) {
                            fromText = filtered
                        } else {
                            val clamped = n.coerceIn(1, 299)
                            fromText = clamped.toString()
                            filters = filters.copy(fromRank = clamped)
                        }
                    },
                    label = { Text(Strings.fromRank(language)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = topCountText,
                    onValueChange = { text ->
                        val filtered = text.filter { it.isDigit() }
                        val n = filtered.toIntOrNull()
                        if (n == null || n == 0) {
                            topCountText = filtered
                        } else {
                            val clamped = n.coerceIn(2, 300)
                            topCountText = clamped.toString()
                            filters = filters.copy(count = clamped)
                        }
                    },
                    label = { Text(Strings.toRank(language)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        } else {
            OutlinedTextField(
                value = topCountText,
                onValueChange = { text ->
                    val filtered = text.filter { it.isDigit() }
                    val n = filtered.toIntOrNull()
                    if (n == null || n == 0) {
                        topCountText = filtered
                    } else {
                        val clamped = n.coerceIn(1, 300)
                        topCountText = clamped.toString()
                        filters = filters.copy(count = clamped)
                    }
                },
                label = { Text(Strings.topCount(language)) },
                supportingText = { Text(Strings.maxCount(language)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (advancedMode) {
                    val from = fromText.toIntOrNull()
                    val to = topCountText.toIntOrNull()
                    if (fromText.isBlank() || topCountText.isBlank() || from == null || to == null) {
                        showCountWarning = true
                        return@Button
                    }
                    if (from <= 0 || to <= 0 || from >= to || from > 300 || to > 300) {
                        showCountWarning = true
                        return@Button
                    }
                } else {
                    val n = topCountText.toIntOrNull()
                    if (topCountText.isBlank() || n == null || n <= 0) {
                        showCountWarning = true
                        return@Button
                    }
                    if (n > 300) {
                        showCountWarning = true
                        return@Button
                    }
                }
                scope.launch {
                    loading = true
                    error = null
                    showErrorDialog = false
                    val progressStages = Strings.pveProgress
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
                        advanceStage(); delay(50)

                        val rawResults = engine.getCachedResults(filters.count).let { cached ->
                            if (cached.isNotEmpty()) cached.also { progress = 0.5f }
                            else engine.compute(filters.count)
                        }

                        advanceStage(); delay(50)
                        val gm = PvPokeApi.fetchGameMaster()

                        advanceStage(); delay(50)
                        val processor = PvPDataProcessor(gm)
                        val pokemonByDex = gm.pokemon.groupBy { it.dex }.mapValues { (_, list) -> list.first() }
                        val filtered = rawResults.filter { entry ->
                            matchesPvEFilter(entry, filters, pokemonByDex[entry.id])
                        }
                        val sliced = if (filters.fromRank > 1) filtered.drop(filters.fromRank - 1) else filtered
                        results = sliced
                        cachedBaseDexes = sliced.map { it.id }
                            .distinct()
                            .mapNotNull { processor.traceBaseDexForDex(it) }
                        cachedFromRank = filters.fromRank

                        advanceStage(); delay(50)
                        val names = cachedBaseDexes.distinct().map { translator.getName(it, language) }
                        searchString = buildSearchString(names, language, filters.includeShadow)

                        advanceStage(); delay(50)
                        resultMessage = "Resultados: ${sliced.size} Pokémon, cadena de ${searchString.length} caracteres"
                        showResultMessage = true
                    } catch (e: Exception) {
                        error = "${e::class.simpleName}: ${e.message}\n\n${e.stackTraceToString()}"
                        showErrorDialog = true
                        resultMessage = "Error: ${e::class.simpleName}: ${e.message}"
                        showResultMessage = true
                    } finally {
                        delay(500)
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

        if (showResultMessage) {
            Text(
                text = resultMessage,
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        if (searchString.isNotBlank()) {
            val title = if (cachedFromRank > 1) {
                Strings.topPvERange(language, cachedFromRank, filters.count)
            } else {
                Strings.topPvE(language, results.size)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            SearchStringCard(searchString = searchString, language = language)
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(results) { index, entry ->
                PokemonRow(
                    rank = cachedFromRank + index,
                    name = cleanPvEName(entry.name, entry.form),
                    score = "%.2f".format(entry.rat),
                    subtitle = entry.tier?.let {
                        "Tier $it — ${entry.fm?.let { fm -> translator.getMoveName(fm, language) } ?: "-"}${if (entry.fmIsElite) "*" else ""}/${
                            entry.cm?.let { cm -> translator.getMoveName(cm, language) } ?: "-" }${if (entry.cmIsElite) "*" else ""}"
                    }
                        ?: "${entry.fm?.let { fm -> translator.getMoveName(fm, language) } ?: "-"}${if (entry.fmIsElite) "*" else ""}/${
                            entry.cm?.let { cm -> translator.getMoveName(cm, language) } ?: "-" }${if (entry.cmIsElite) "*" else ""}",
                    tags = listOfNotNull(
                        if (entry.shadow) Strings.tagShadow(language) else null,
                        if (entry.form.startsWith("Mega")) Strings.tagMega(language) else null
                    )
                )
            }
        }
    }

    if (showFilters) {
        PvEFilterBottomSheet(
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
            title = Strings.errorTitle("PvE", language),
            message = error!!,
            language = language,
            onDismiss = { showErrorDialog = false }
        )
    }

    if (showCountWarning) {
        AlertDialog(
            onDismissRequest = { showCountWarning = false },
            title = { Text(Strings.topCount(language)) },
            text = {
                Text(
                    if (advancedMode) Strings.enterPositiveRange(language)
                    else Strings.enterPositiveCount(language)
                )
            },
            confirmButton = {
                TextButton(onClick = { showCountWarning = false }) {
                    Text(Strings.close(language))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCountWarning = false }) {
                    Text(Strings.cancel(language))
                }
            }
        )
    }
}

private fun cleanPvEName(name: String, form: String): String {
    return when {
        form == "Normal" -> name
        name.startsWith("Mega ") || name.startsWith("Primal ") -> name
        form.startsWith("Mega") -> "Mega $name"
        else -> "$name ($form)"
    }
}

private val legendaryTags = setOf("legendary", "mythical", "ultrabeast")

private fun matchesPvEFilter(entry: PvERankingEntry, filters: PvEFilterParams, pokemon: Pokemon?): Boolean {
    if (filters.includeShadow && !entry.shadow) return false
    if (!filters.includeShadow && entry.shadow) return false

    if (!filters.mega) {
        val form = entry.form.lowercase()
        if (form.startsWith("mega") || form == "primal") return false
    }

    if (!filters.legendary) {
        val isLegendary = pokemon?.tags?.any { it.lowercase() in legendaryTags } ?: false
        if (isLegendary) return false
    }

    if (!filters.unreleased) {
        if (pokemon?.released == false) return false
    }

    return true
}
