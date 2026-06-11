package com.pokechain.ui.pve

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pokechain.data.dialgadex.DialgaDexJsEngine
import com.pokechain.data.dialgadex.NameTranslator
import com.pokechain.data.models.*
import com.pokechain.data.pvpoke.PvPDataProcessor
import com.pokechain.data.pvpoke.PvPokeApi
import com.pokechain.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun PvEScreen(language: com.pokechain.data.models.AppLanguage = com.pokechain.data.models.AppLanguage.ES) {
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

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        OutlinedButton(
            onClick = { showFilters = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Filtros")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = filters.count.toString(),
            onValueChange = { filters = filters.copy(count = it.toIntOrNull() ?: 20) },
            label = { Text("Cantidad") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    loading = true
                    error = null
                    showErrorDialog = false
                    try {
                        progress = 0.1f
                        progressMessage = "Descargando datos del juego..."
                        val engine = DialgaDexJsEngine(context)

                        progress = 0.3f
                        progressMessage = "Inicializando calculadora PvE..."
                        val rawResults = engine.fetchPvERankings(filters)
                        results = rawResults

                        progress = 0.5f
                        progressMessage = "Descargando game master..."
                        val gm = PvPokeApi.fetchGameMaster()

                        progress = 0.7f
                        progressMessage = "Resolviendo formas base..."
                        val processor = PvPDataProcessor(gm)
                        val baseDexes = rawResults.map { it.id }
                            .distinct()
                            .mapNotNull { processor.traceBaseDexForDex(it) }

                        progress = 0.9f
                        progressMessage = "Generando cadena de búsqueda..."
                        val names = baseDexes.distinct().map { translator.getName(it, language) }
                        searchString = names.joinToString(";") { "+$it" }

                        progress = 1f
                        progressMessage = "Completado"
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
            Text("Generar")
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
            SearchStringCard(searchString = searchString)
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(results) { index, entry ->
                PokemonRow(
                    rank = index + 1,
                    name = cleanPvEName(entry.name, entry.form),
                    score = "%.2f".format(entry.rat),
                    subtitle = entry.tier?.let {
                            "Tier $it — ${entry.fm?.let { fm -> translator.getMoveName(fm, language) } ?: "-"}/${
                                entry.cm?.let { cm -> translator.getMoveName(cm, language) } ?: "-"
                            }"
                        }
                        ?: "${entry.fm?.let { fm -> translator.getMoveName(fm, language) } ?: "-"}/${
                            entry.cm?.let { cm -> translator.getMoveName(cm, language) } ?: "-"
                        }",
                    tags = listOfNotNull(
                        if (entry.shadow) "Shadow" else null,
                        if (entry.form.startsWith("Mega")) "Mega" else null
                    )
                )
            }
        }
    }

    if (showFilters) {
        PvEFilterBottomSheet(
            filters = filters,
            onDismiss = { showFilters = false },
            onApply = { newFilters ->
                filters = newFilters
                showFilters = false
            }
        )
    }

    if (showErrorDialog && error != null) {
        ErrorDialog(
            title = "Error PvE",
            message = error!!,
            onDismiss = { showErrorDialog = false }
        )
    }
}

private fun cleanPvEName(name: String, form: String): String {
    return if (form.startsWith("Mega") || form == "MegaY" || form == "MegaZ") {
        "Mega $name"
    } else if (form != "Normal") {
        "$name ($form)"
    } else name
}
