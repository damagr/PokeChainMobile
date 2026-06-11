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
fun PvPScreen(language: com.pokechain.data.models.AppLanguage = com.pokechain.data.models.AppLanguage.ES) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var filters by remember { mutableStateOf(PvPFilterParams()) }
    var results by remember { mutableStateOf<List<PvPResult>>(emptyList()) }
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
        LeagueSelector(
            selected = filters.league,
            onSelect = { filters = filters.copy(league = it) }
        )

        Spacer(Modifier.height(8.dp))

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
                        progressMessage = "Descargando datos de PvPoke..."
                        val translator = NameTranslator(context)
                        val api = com.pokechain.data.pvpoke.PvPokeApi
                        val gameMaster = api.fetchGameMaster()

                        progress = 0.3f
                        progressMessage = "Descargando rankings..."
                        val rankings = api.fetchRankings(filters.league.cp)

                        progress = 0.5f
                        progressMessage = "Procesando rankings..."
                        val processor = PvPDataProcessor(gameMaster)
                        val useCase = PvPFilterUseCase(processor)
                        val filtered = useCase.execute(rankings, filters)
                        results = filtered

                        progress = 0.7f
                        progressMessage = "Resolviendo formas base..."
                        val baseDexes = filtered.mapNotNull { processor.traceBaseDex(it) }

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
                Text(
                    text = progressMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (searchString.isNotBlank()) {
            SearchStringCard(searchString = searchString)
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(results) { index, result ->
                PokemonRow(
                    rank = index + 1,
                    name = result.speciesName,
                    score = result.score.toString(),
                    subtitle = result.moveset.joinToString(", "),
                    tags = listOfNotNull(
                        if (result.isShadow) "Shadow" else null,
                        if (result.needsXL) "XL" else null,
                        if (result.hasEliteMove) "Elite" else null
                    )
                )
            }
        }
    }

    if (showFilters) {
        FilterBottomSheet(
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
            title = "Error PvP",
            message = error!!,
            onDismiss = { showErrorDialog = false }
        )
    }
}
