package com.pokechain.ui.pve

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pokechain.data.dialgadex.NameTranslator
import com.pokechain.data.dialgadex.PvEJSEngine
import com.pokechain.data.models.*
import com.pokechain.data.pvpoke.PvPDataProcessor
import com.pokechain.data.pvpoke.PvPokeApi
import com.pokechain.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PvEScreen(language: AppLanguage = AppLanguage.ES) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val translator = remember { NameTranslator(context) }
    var filters by remember { mutableStateOf(PvEFilterParams()) }
    var results by remember { mutableStateOf<List<PvERankingEntry>>(emptyList()) }
    var searchString by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var progressMessage by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var topCountText by remember { mutableStateOf(filters.count.toString()) }
    var cachedBaseDexes by remember { mutableStateOf<List<Int>>(emptyList()) }
    var resultMessage by remember { mutableStateOf("") }
    var showResultMessage by remember { mutableStateOf(false) }

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
                        val engine = PvEJSEngine(context)

                        advanceStage(); delay(50)
                        val rawResults = engine.compute(filters)
                        results = rawResults

                        advanceStage(); delay(50)
                        val gm = PvPokeApi.fetchGameMaster()

                        advanceStage(); delay(50)
                        val processor = PvPDataProcessor(gm)
                        cachedBaseDexes = rawResults.map { it.id }
                            .distinct()
                            .mapNotNull { processor.traceBaseDexForDex(it) }

                        advanceStage(); delay(50)
                        val names = cachedBaseDexes.distinct().map { translator.getName(it, language) }
                        searchString = buildSearchString(names, language, filters.includeShadow)

                        advanceStage(); delay(50)
                        resultMessage = "Resultados: ${results.size} Pokémon, cadena de ${searchString.length} caracteres"
                        showResultMessage = true
                    } catch (e: Exception) {
                        error = "${e::class.simpleName}: ${e.message}\n\n${e.stackTraceToString()}"
                        showErrorDialog = true
                        resultMessage = "Error: ${e::class.simpleName}: ${e.message}"
                        showResultMessage = true
                    } finally {
                        kotlinx.coroutines.delay(500)
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
            Text(
                text = Strings.topPvE(language, results.size),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            SearchStringCard(searchString = searchString, language = language)
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(results) { index, entry ->
                PokemonRow(
                    rank = index + 1,
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
}

private fun cleanPvEName(name: String, form: String): String {
    return when {
        form == "Normal" -> name
        name.startsWith("Mega ") || name.startsWith("Primal ") -> name
        form.startsWith("Mega") -> "Mega $name"
        else -> "$name ($form)"
    }
}
