package com.pokechain.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
            Spacer(Modifier.height(40.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueDropdown(
    selected: PvPLeague,
    language: AppLanguage,
    onSelect: (PvPLeague) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val standardLeagues = PvPLeague.entries.filter { it.cup == null }
    val cups = PvPLeague.entries.filter { it.cup != null }.sortedBy { Strings.leagueName(it, language) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = Strings.leagueName(selected, language),
            onValueChange = {},
            readOnly = true,
            label = { Text(when (language) { AppLanguage.EN -> "Leagues & Cups"; AppLanguage.ES -> "Ligas y copas" }) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        when (language) { AppLanguage.EN -> "Standard Leagues"; AppLanguage.ES -> "Ligas estándar" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {},
                enabled = false
            )
            standardLeagues.forEach { league ->
                DropdownMenuItem(
                    text = { Text(Strings.leagueName(league, language)) },
                    onClick = { onSelect(league); expanded = false },
                    leadingIcon = if (selected == league) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Text(
                        when (language) { AppLanguage.EN -> "Cups"; AppLanguage.ES -> "Copas" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {},
                enabled = false
            )
            cups.forEach { league ->
                DropdownMenuItem(
                    text = { Text(Strings.leagueName(league, language)) },
                    onClick = { onSelect(league); expanded = false },
                    leadingIcon = if (selected == league) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(
    filters: PvPFilterParams,
    language: AppLanguage,
    onApply: (PvPFilterParams) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val summary = buildList {
        if (filters.xlCandy) add(Strings.xlCandy(language))
        if (filters.includeShadow) add(Strings.shadowLabel(language))
        if (filters.includeElite) add(Strings.eliteMove(language))
    }.joinToString(", ").ifEmpty {
        when (language) { AppLanguage.EN -> "All"; AppLanguage.ES -> "Todo" }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = summary,
            onValueChange = {},
            readOnly = true,
            label = { Text(Strings.filters(language)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(Strings.xlCandy(language), modifier = Modifier.weight(1f))
                        Switch(checked = filters.xlCandy, onCheckedChange = null)
                    }
                },
                onClick = { onApply(filters.copy(xlCandy = !filters.xlCandy)) }
            )
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(Strings.shadowLabel(language), modifier = Modifier.weight(1f))
                        Switch(checked = filters.includeShadow, onCheckedChange = null)
                    }
                },
                onClick = { onApply(filters.copy(includeShadow = !filters.includeShadow)) }
            )
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(Strings.eliteMove(language), modifier = Modifier.weight(1f))
                        Switch(checked = filters.includeElite, onCheckedChange = null)
                    }
                },
                onClick = { onApply(filters.copy(includeElite = !filters.includeElite)) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PvEFilterDropdown(
    filters: PvEFilterParams,
    language: AppLanguage,
    onApply: (PvEFilterParams) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val summary = buildList {
        if (filters.unreleased) add(Strings.unreleased(language))
        if (filters.includeShadow) add(Strings.shadowLabel(language))
        if (filters.casualShadow) add(Strings.casualShadow(language))
        if (filters.legendary) add(Strings.legendary(language))
        if (filters.mega) add(Strings.megaPrimal(language))
    }.joinToString(", ").ifEmpty {
        when (language) { AppLanguage.EN -> "All"; AppLanguage.ES -> "Todo" }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = summary,
            onValueChange = {},
            readOnly = true,
            label = { Text(Strings.filters(language)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(Strings.unreleased(language), modifier = Modifier.weight(1f))
                        Switch(checked = filters.unreleased, onCheckedChange = null)
                    }
                },
                onClick = { onApply(filters.copy(unreleased = !filters.unreleased)) }
            )
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(Strings.shadowLabel(language), modifier = Modifier.weight(1f))
                        Switch(checked = filters.includeShadow, onCheckedChange = null)
                    }
                },
                onClick = {
                    val newShadow = !filters.includeShadow
                    onApply(filters.copy(
                        includeShadow = newShadow,
                        mega = if (newShadow) false else filters.mega,
                        casualShadow = if (newShadow) false else filters.casualShadow
                    ))
                }
            )
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(Strings.casualShadow(language), modifier = Modifier.weight(1f))
                        Switch(checked = filters.casualShadow, onCheckedChange = null)
                    }
                },
                onClick = {
                    val newCasual = !filters.casualShadow
                    onApply(filters.copy(
                        casualShadow = newCasual,
                        includeShadow = if (newCasual) false else filters.includeShadow,
                        mega = if (newCasual) false else filters.mega
                    ))
                }
            )
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(Strings.legendary(language), modifier = Modifier.weight(1f))
                        Switch(checked = filters.legendary, onCheckedChange = null)
                    }
                },
                onClick = { onApply(filters.copy(legendary = !filters.legendary)) }
            )
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(Strings.megaPrimal(language), modifier = Modifier.weight(1f))
                        Switch(checked = filters.mega, onCheckedChange = null)
                    }
                },
                onClick = {
                    val newMega = !filters.mega
                    onApply(filters.copy(
                        mega = newMega,
                        includeShadow = if (newMega) false else filters.includeShadow,
                        casualShadow = if (newMega) false else filters.casualShadow
                    ))
                }
            )
        }
    }
}