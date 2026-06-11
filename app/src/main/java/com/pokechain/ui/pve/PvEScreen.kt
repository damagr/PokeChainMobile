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
import com.pokechain.data.models.*
import com.pokechain.data.pvpoke.PvPokeApi
import com.pokechain.domain.PvEFilterUseCase
import com.pokechain.domain.SearchStringUseCase
import com.pokechain.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun PvEScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var filters by remember { mutableStateOf(PvEFilterParams()) }
    var results by remember { mutableStateOf<List<PvERankingEntry>>(emptyList()) }
    var searchString by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showFilters by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
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
                        val engine = DialgaDexJsEngine(context)
                        val rawResults = engine.fetchPvERankings(filters)
                        results = rawResults

                        val gm = PvPokeApi.fetchGameMaster()
                        val processor = com.pokechain.data.pvpoke.PvPDataProcessor(gm)
                        val resolver: (Int, String) -> String? = { dex, _ ->
                            processor.traceBaseFormForDex(dex)
                        }
                        val pveUseCase = PvEFilterUseCase(resolver)
                        val baseForms = pveUseCase.dedupAndResolve(rawResults)
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
            itemsIndexed(results) { index, entry ->
                PokemonRow(
                    rank = index + 1,
                    name = cleanPvEName(entry.name, entry.form),
                    score = "%.2f".format(entry.rat),
                    subtitle = entry.tier?.let { "Tier $it — ${entry.fm ?: "-"}/${entry.cm ?: "-"}" }
                        ?: "${entry.fm ?: "-"}/${entry.cm ?: "-"}",
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
}

private fun cleanPvEName(name: String, form: String): String {
    return if (form.startsWith("Mega") || form == "MegaY" || form == "MegaZ") {
        "Mega $name"
    } else if (form != "Normal") {
        "$name ($form)"
    } else name
}
