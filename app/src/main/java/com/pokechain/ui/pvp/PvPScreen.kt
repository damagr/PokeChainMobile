package com.pokechain.ui.pvp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pokechain.data.models.*
import com.pokechain.data.pvpoke.*
import com.pokechain.domain.PvPFilterUseCase
import com.pokechain.domain.SearchStringUseCase
import com.pokechain.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun PvPScreen() {
    val scope = rememberCoroutineScope()
    var filters by remember { mutableStateOf(PvPFilterParams()) }
    var results by remember { mutableStateOf<List<PvPResult>>(emptyList()) }
    var searchString by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
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
            Text("Filters")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = filters.count.toString(),
            onValueChange = { filters = filters.copy(count = it.toIntOrNull() ?: 20) },
            label = { Text("Count") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    loading = true
                    error = null
                    try {
                        val api = com.pokechain.data.pvpoke.PvPokeApi
                        val gameMaster = api.fetchGameMaster()
                        val rankings = api.fetchRankings(filters.league.cp)
                        val processor = PvPDataProcessor(gameMaster)
                        val useCase = PvPFilterUseCase(processor)
                        val filtered = useCase.execute(rankings, filters)
                        results = filtered
                        val baseForms = filtered.map { useCase.resolveBaseForm(it) }
                        val searchUseCase = SearchStringUseCase()
                        searchString = searchUseCase.generate(baseForms).formatted
                    } catch (e: Exception) {
                        error = e.message ?: "Unknown error"
                    } finally {
                        loading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generate")
        }

        Spacer(Modifier.height(8.dp))

        if (loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
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
}
