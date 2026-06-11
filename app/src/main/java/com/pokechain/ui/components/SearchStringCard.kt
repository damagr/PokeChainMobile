package com.pokechain.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = Strings.searchString(language),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = searchString,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    clipboard.setText(AnnotatedString(searchString))
                    copied = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (copied) Strings.copied(language) else Strings.copy(language))
            }
        }
    }
}
