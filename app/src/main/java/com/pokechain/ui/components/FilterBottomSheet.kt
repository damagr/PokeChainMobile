package com.pokechain.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pokechain.data.models.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    filters: PvPFilterParams,
    onDismiss: () -> Unit,
    onApply: (PvPFilterParams) -> Unit
) {
    var xl by remember { mutableStateOf(filters.xlCandy) }
    var shadow by remember { mutableStateOf(filters.shadowFilter) }
    var elite by remember { mutableStateOf(filters.eliteFilter) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("XL Candy", style = MaterialTheme.typography.titleSmall)
            Row {
                FilterChip(selected = xl, onClick = { xl = true }, label = { Text("Yes") })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = !xl, onClick = { xl = false }, label = { Text("No") })
            }

            Spacer(Modifier.height(16.dp))
            Text("Shadow", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ShadowFilter.entries.forEach { value ->
                    FilterChip(
                        selected = shadow == value,
                        onClick = { shadow = value },
                        label = { Text(value.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Elite Move", style = MaterialTheme.typography.titleSmall)
            Row {
                FilterChip(
                    selected = elite == EliteFilter.IMPORTANT,
                    onClick = { elite = EliteFilter.IMPORTANT },
                    label = { Text("Important") }
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = elite == EliteFilter.NOT_IMPORTANT,
                    onClick = { elite = EliteFilter.NOT_IMPORTANT },
                    label = { Text("Not Important") }
                )
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onApply(filters.copy(xlCandy = xl, shadowFilter = shadow, eliteFilter = elite)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PvEFilterBottomSheet(
    filters: PvEFilterParams,
    onDismiss: () -> Unit,
    onApply: (PvEFilterParams) -> Unit
) {
    var unreleased by remember { mutableStateOf(filters.unreleased) }
    var shadow by remember { mutableStateOf(filters.shadowFilter) }
    var legendary by remember { mutableStateOf(filters.legendary) }
    var mega by remember { mutableStateOf(filters.mega) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Unreleased", modifier = Modifier.weight(1f))
                Switch(checked = unreleased, onCheckedChange = { unreleased = it })
            }

            Spacer(Modifier.height(12.dp))
            Text("Shadow", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ShadowFilter.entries.forEach { value ->
                    FilterChip(
                        selected = shadow == value,
                        onClick = { shadow = value },
                        label = { Text(value.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Legendary", modifier = Modifier.weight(1f))
                Switch(checked = legendary, onCheckedChange = { legendary = it })
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Mega/Primal", modifier = Modifier.weight(1f))
                Switch(checked = mega, onCheckedChange = { mega = it })
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    onApply(filters.copy(
                        unreleased = unreleased,
                        shadowFilter = shadow,
                        legendary = legendary,
                        mega = mega
                    ))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun LeagueSelector(
    selected: PvPLeague,
    onSelect: (PvPLeague) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PvPLeague.entries.forEach { league ->
            FilterChip(
                selected = selected == league,
                onClick = { onSelect(league) },
                label = {
                    Text(
                        when (league) {
                            PvPLeague.GREAT -> "Great"
                            PvPLeague.ULTRA -> "Ultra"
                            PvPLeague.MASTER -> "Master"
                        }
                    )
                }
            )
        }
    }
}
