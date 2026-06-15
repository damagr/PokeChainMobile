package com.pokechain.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pokechain.data.models.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    filters: PvPFilterParams,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onApply: (PvPFilterParams) -> Unit
) {
    var xl by remember { mutableStateOf(filters.xlCandy) }
    var includeShadow by remember { mutableStateOf(filters.includeShadow) }
    var includeElite by remember { mutableStateOf(filters.includeElite) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
            ToggleRow(label = Strings.xlCandy(language), checked = xl, onCheckedChange = { xl = it })
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ToggleRow(label = Strings.shadowLabel(language), checked = includeShadow, onCheckedChange = { includeShadow = it })
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ToggleRow(label = Strings.eliteMove(language), checked = includeElite, onCheckedChange = { includeElite = it })

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    onApply(filters.copy(
                        xlCandy = xl,
                        includeShadow = includeShadow,
                        includeElite = includeElite
                    ))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Strings.apply(language))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PvEFilterBottomSheet(
    filters: PvEFilterParams,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onApply: (PvEFilterParams) -> Unit
) {
    var unreleased by remember { mutableStateOf(filters.unreleased) }
    var includeShadow by remember { mutableStateOf(filters.includeShadow) }
    var legendary by remember { mutableStateOf(filters.legendary) }
    var mega by remember { mutableStateOf(filters.mega) }
    var casualShadow by remember { mutableStateOf(filters.casualShadow) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.navigationBarsPadding()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ToggleRow(label = Strings.unreleased(language), checked = unreleased, onCheckedChange = { unreleased = it })
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ToggleRow(label = Strings.shadowLabel(language), checked = includeShadow, onCheckedChange = {
                includeShadow = it
                if (it) {
                    mega = false
                    casualShadow = false
                }
            })
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ToggleRow(label = Strings.casualShadow(language), checked = casualShadow, onCheckedChange = {
                casualShadow = it
                if (it) {
                    includeShadow = false
                    mega = false
                }
            })
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ToggleRow(label = Strings.legendary(language), checked = legendary, onCheckedChange = { legendary = it })
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ToggleRow(label = Strings.megaPrimal(language), checked = mega, onCheckedChange = {
                mega = it
                if (it) {
                    includeShadow = false
                    casualShadow = false
                }
            })

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (unreleased && !includeShadow && !legendary && !mega) {
                        legendary = true
                        mega = true
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                when (language) {
                                    AppLanguage.EN -> "Unreleased requires Legendary and Mega/Primal enabled"
                                    AppLanguage.ES -> "Inédito requiere Legendario y Mega/Primal activos"
                                }
                            )
                        }
                    }
                    onApply(filters.copy(
                        unreleased = unreleased,
                        includeShadow = includeShadow,
                        legendary = legendary,
                        mega = mega,
                        casualShadow = casualShadow
                    ))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Strings.apply(language))
            }
            SnackbarHost(snackbarHostState, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun ToggleRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun LanguageSelector(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppLanguage.entries.forEach { lang ->
            FilterChip(
                selected = selected == lang,
                onClick = { onSelect(lang) },
                label = {
                    Text(
                        when (lang) {
                            AppLanguage.EN -> "EN"
                            AppLanguage.ES -> "ES"
                        }
                    )
                }
            )
        }
    }
}

@Composable
fun LeagueSelector(
    selected: PvPLeague,
    language: AppLanguage,
    onSelect: (PvPLeague) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PvPLeague.entries.forEach { league ->
            FilterChip(
                selected = selected == league,
                onClick = { onSelect(league) },
                label = { Text(Strings.leagueName(league, language)) }
            )
        }
    }
}
