package com.pokechain.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.pokechain.data.models.AppLanguage
import com.pokechain.data.models.Strings

@Composable
fun SearchStringCard(searchString: String, language: AppLanguage) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = Strings.searchString(language),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(searchString))
                    copied = true
                }) {
                    Text(
                        if (copied) Strings.copied(language) else Strings.copy(language),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            if (expanded) {
                Box(modifier = Modifier.heightIn(max = 60.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = searchString,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
