package com.pokechain.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pokechain.data.models.*

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
        Column(modifier = Modifier.padding(16.dp)) {
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

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            ToggleRow(label = Strings.unreleased(language), checked = unreleased, onCheckedChange = { unreleased = it })
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ToggleRow(label = Strings.shadowLabel(language), checked = includeShadow, onCheckedChange = { includeShadow = it })
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ToggleRow(label = Strings.legendary(language), checked = legendary, onCheckedChange = { legendary = it })
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ToggleRow(label = Strings.megaPrimal(language), checked = mega, onCheckedChange = { mega = it })

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    onApply(filters.copy(
                        unreleased = unreleased,
                        includeShadow = includeShadow,
                        legendary = legendary,
                        mega = mega
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

@Composable
fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
