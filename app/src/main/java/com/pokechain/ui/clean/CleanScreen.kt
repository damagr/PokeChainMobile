package com.pokechain.ui.clean

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pokechain.data.models.AppLanguage
import com.pokechain.data.models.Strings
import com.pokechain.ui.components.SearchStringCard
import com.pokechain.ui.components.ToggleRow

private data class CleanAttribute(
    val labelEs: String,
    val labelEn: String,
    val searchEs: String,
    val searchEn: String,
)

private val cleanAttributes = listOf(
    CleanAttribute("4★", "4★", "4*", "4*"),
    CleanAttribute("3★", "3★", "3*", "3*"),
    CleanAttribute("Variocolor", "Shiny", "variocolor", "shiny"),
    CleanAttribute("Suerte", "Lucky", "suerte", "lucky"),
    CleanAttribute("Favoritos", "Favorite", "favoritos", "favorite"),
    CleanAttribute("Disfraz", "Costume", "disfraz", "costume"),
    CleanAttribute("Fondo Lugar", "Location background", "fondodelugar", "locationbackground"),
    CleanAttribute("Legendario", "Legendary", "legendario", "legendary"),
    CleanAttribute("Singular", "Mythical", "singular", "mythical"),
    CleanAttribute("Ultraente", "Ultra Beast", "ultraente", "ultrabeast"),
    CleanAttribute("Gigamax", "Gigantamax", "gigamax", "gigantamax"),
)

@Composable
fun CleanScreen(language: AppLanguage = AppLanguage.ES) {
    val checked = remember { mutableStateListOf(*BooleanArray(cleanAttributes.size) { false }.toTypedArray()) }
    var showResult by remember { mutableStateOf(false) }

    val searchString = remember(checked.toList(), language) {
        val terms = mutableListOf<String>()
        cleanAttributes.forEachIndexed { index, attr ->
            if (checked[index]) {
                terms.add(
                    when (language) {
                        AppLanguage.ES -> attr.searchEs
                        AppLanguage.EN -> attr.searchEn
                    }
                )
            }
        }
        if (terms.isNotEmpty()) terms.joinToString("&") { "!$it" } + "&!#" else ""
    }

    fun generate() {
        showResult = true
    }

    val allChecked = remember(checked.toList()) { checked.all { it } }

    fun selectAllLabel() = when (language) {
        AppLanguage.ES -> if (allChecked) "Deseleccionar todo" else "Seleccionar todo"
        AppLanguage.EN -> if (allChecked) "Deselect all" else "Select all"
    }
    fun editLabel() = when (language) { AppLanguage.ES -> "Editar"; AppLanguage.EN -> "Edit" }
    fun resetLabel() = when (language) { AppLanguage.ES -> "Reiniciar"; AppLanguage.EN -> "Reset" }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).navigationBarsPadding()
    ) {
        if (!showResult) {
            val leftAttrs = cleanAttributes.take(6)
            val rightAttrs = cleanAttributes.drop(6)

            OutlinedButton(
                onClick = {
                    val newValue = !allChecked
                    cleanAttributes.indices.forEach { checked[it] = newValue }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectAllLabel())
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    leftAttrs.forEachIndexed { colIndex, attr ->
                        val label = when (language) {
                            AppLanguage.ES -> attr.labelEs
                            AppLanguage.EN -> attr.labelEn
                        }
                        ToggleRow(
                            label = label,
                            checked = checked[colIndex],
                            onCheckedChange = { checked[colIndex] = it }
                        )
                        if (colIndex < leftAttrs.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    rightAttrs.forEachIndexed { colIndex, attr ->
                        val globalIndex = 6 + colIndex
                        val label = when (language) {
                            AppLanguage.ES -> attr.labelEs
                            AppLanguage.EN -> attr.labelEn
                        }
                        ToggleRow(
                            label = label,
                            checked = checked[globalIndex],
                            onCheckedChange = { checked[globalIndex] = it }
                        )
                        if (colIndex < rightAttrs.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { generate() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Strings.generate(language))
            }
        }

        if (showResult && searchString.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            SearchStringCard(searchString = searchString, language = language)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showResult = false },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(editLabel())
                }
                OutlinedButton(
                    onClick = {
                        cleanAttributes.indices.forEach { checked[it] = false }
                        showResult = false
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(resetLabel())
                }
            }
        }
    }
}
