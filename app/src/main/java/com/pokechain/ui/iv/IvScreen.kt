package com.pokechain.ui.iv

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pokechain.data.dialgadex.NameTranslator
import com.pokechain.data.dialgadex.PokemonTypeEntry
import com.pokechain.data.dialgadex.PokemonTypeProvider
import com.pokechain.data.iv.IvCalculator
import com.pokechain.data.iv.IvResult
import com.pokechain.data.models.AppLanguage
import com.pokechain.data.models.BaseStats
import com.pokechain.data.models.Strings
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IvScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val translator = remember { NameTranslator(context) }
    val typeProvider = remember { PokemonTypeProvider() }

    // ── Pokémon search state ──────────────────────────────────────
    var searchText by remember { mutableStateOf(TextFieldValue("")) }
    var showDropdown by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<PokemonTypeEntry?>(null) }
    var isLoadingTypes by remember { mutableStateOf(true) }
    var typeLoadError by remember { mutableStateOf<String?>(null) }

    // ── IV input state ────────────────────────────────────────────
    var cpText by remember { mutableStateOf("") }
    var atkIvText by remember { mutableStateOf("") }
    var defIvText by remember { mutableStateOf("") }
    var staIvText by remember { mutableStateOf("") }

    // ── Results state ─────────────────────────────────────────────
    var results by remember { mutableStateOf<List<IvResult>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasCalculated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            typeProvider.ensureLoaded()
        } catch (e: Exception) {
            typeLoadError = when (language) {
                AppLanguage.EN -> "Failed to load Pokémon data: ${e.message}"
                AppLanguage.ES -> "Error al cargar datos Pokémon: ${e.message}"
            }
        } finally {
            isLoadingTypes = false
        }
    }

    val suggestions = remember(searchText.text, typeProvider.isLoaded) {
        if (!typeProvider.isLoaded) emptyList()
        else typeProvider.searchAll(searchText.text, translator, language).take(30)
    }

    fun performCalculation() {
        errorMessage = null
        results = emptyList()

        val entry = selectedEntry
        if (entry == null) {
            errorMessage = Strings.ivSelectFirst(language)
            return
        }

        val baseStats = entry.baseStats
        if (baseStats == null) {
            errorMessage = when (language) {
                AppLanguage.EN -> "Base stats not available for this Pokémon"
                AppLanguage.ES -> "Estadísticas base no disponibles para este Pokémon"
            }
            return
        }

        val cp = cpText.toIntOrNull()
        val atk = atkIvText.toIntOrNull()
        val def = defIvText.toIntOrNull()
        val sta = staIvText.toIntOrNull()

        if (cp == null || atk == null || def == null || sta == null ||
            atk !in 0..15 || def !in 0..15 || sta !in 0..15 || cp < 10
        ) {
            errorMessage = Strings.ivInsertValid(language)
            return
        }

        results = IvCalculator.findLevel(
            baseAtk = baseStats.atk,
            baseDef = baseStats.def,
            baseSta = baseStats.hp,
            atkIv = atk,
            defIv = def,
            staIv = sta,
            targetCp = cp
        )
        hasCalculated = true
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = Strings.back(language)
                        )
                    }
                },
                title = { Text(Strings.ivCalculator(language)) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Pokémon selector ──────────────────────────────────
            ExposedDropdownMenuBox(
                expanded = showDropdown && suggestions.isNotEmpty() && selectedEntry == null,
                onExpandedChange = { showDropdown = it }
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { newValue ->
                        searchText = newValue
                        showDropdown = true
                        selectedEntry = null
                        cpText = ""
                        atkIvText = ""
                        defIvText = ""
                        staIvText = ""
                        results = emptyList()
                        hasCalculated = false
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable),
                    placeholder = { Text(Strings.ivSelectPokemon(language)) },
                    singleLine = true,
                    trailingIcon = {
                        if (searchText.text.isNotEmpty()) {
                            IconButton(onClick = {
                                searchText = TextFieldValue("")
                                selectedEntry = null
                                cpText = ""
                                atkIvText = ""
                                defIvText = ""
                                staIvText = ""
                                results = emptyList()
                                hasCalculated = false
                                errorMessage = null
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )

                ExposedDropdownMenu(
                    expanded = showDropdown && suggestions.isNotEmpty() && selectedEntry == null,
                    onDismissRequest = { showDropdown = false }
                ) {
                    suggestions.forEach { entry ->
                        DropdownMenuItem(
                            text = {
                                val displayName = entry.displayName(language, translator)
                                Text(
                                    text = "$displayName  #${entry.dex}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            onClick = {
                                val name = entry.displayName(language, translator)
                                searchText = TextFieldValue(name, selection = TextRange(name.length))
                                selectedEntry = entry
                                showDropdown = false
                                cpText = ""
                                atkIvText = ""
                                defIvText = ""
                                staIvText = ""
                                results = emptyList()
                                hasCalculated = false
                                errorMessage = null
                            }
                        )
                    }
                }
            }

            if (isLoadingTypes) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            typeLoadError?.let { err ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = err,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // ── CP input ──────────────────────────────────────────
            OutlinedTextField(
                value = cpText,
                onValueChange = {
                    cpText = it
                    hasCalculated = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(Strings.ivCpLabel(language)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // ── IV inputs ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = atkIvText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.toInt() in 0..15)) {
                            atkIvText = newValue
                            hasCalculated = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text(Strings.ivAtkLabel(language)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = defIvText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.toInt() in 0..15)) {
                            defIvText = newValue
                            hasCalculated = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text(Strings.ivDefLabel(language)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = staIvText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.toInt() in 0..15)) {
                            staIvText = newValue
                            hasCalculated = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text(Strings.ivHpLabel(language)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // ── Calculate button ──────────────────────────────────
            Button(
                onClick = { performCalculation() },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedEntry != null && !isLoadingTypes
            ) {
                Text(Strings.ivCalculate(language))
            }

            // ── Error message ─────────────────────────────────────
            errorMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // ── Results ───────────────────────────────────────────
            if (hasCalculated) {
                if (results.isEmpty() && errorMessage == null) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = Strings.ivNoResults(language),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (results.isNotEmpty()) {
                    Text(
                        text = Strings.ivPerfection(language),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    val bs = selectedEntry?.baseStats
                    val atk = atkIvText.toIntOrNull() ?: 0
                    val def = defIvText.toIntOrNull() ?: 0
                    val sta = staIvText.toIntOrNull() ?: 0
                    val pkmName = selectedEntry?.displayName(language, translator) ?: ""

                    results.forEach { result ->
                        IvResultCard(
                            result = result,
                            baseStats = bs,
                            atkIv = atk,
                            defIv = def,
                            staIv = sta,
                            pokemonName = pkmName,
                            language = language
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun IvResultCard(
    result: IvResult,
    baseStats: BaseStats?,
    atkIv: Int,
    defIv: Int,
    staIv: Int,
    pokemonName: String,
    language: AppLanguage
) {
    val perfectionColor = when {
        result.perfection >= 97.78 -> Color(0xFF4CAF50)
        result.perfection >= 91.11 -> Color(0xFF8BC34A)
        result.perfection >= 80.0  -> Color(0xFFFFEB3B)
        else -> Color(0xFFFF9800)
    }

    // ── Projection state ──────────────────────────────────────────
    val maxLevel = 50.0
    val minLevel = result.level
    var targetLevel by remember { mutableStateOf(minLevel) }

    // Ensure target stays in bounds when result.level changes
    LaunchedEffect(minLevel) {
        if (targetLevel < minLevel) targetLevel = minLevel
    }

    val projection = remember(targetLevel, baseStats) {
        if (baseStats == null) null
        else IvCalculator.calculateAtLevel(baseStats.atk, baseStats.def, baseStats.hp, atkIv, defIv, staIv, targetLevel)
    }
    val cost = remember(result.level, targetLevel) {
        IvCalculator.getPowerUpCost(result.level, targetLevel)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            // ── Header: level + HP ────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${Strings.ivLevel(language)} ${formatLevel(result.level)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${Strings.ivEstimatedHp(language)}: ${result.hp}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Perfection bar ────────────────────────────────────
            LinearProgressIndicator(
                progress = { (result.perfection / 100.0).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = perfectionColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = formatPerfection(result.perfection, language),
                style = MaterialTheme.typography.labelMedium,
                color = perfectionColor,
                fontWeight = FontWeight.Bold
            )

            // ── Divider ───────────────────────────────────────────
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // ── Projection section ────────────────────────────────
            Text(
                text = Strings.ivProjection(language),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(6.dp))

            // Target level slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatLevel(minLevel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = targetLevel.toFloat(),
                    onValueChange = {
                        // Step by 0.5: round to nearest 0.5
                        val stepped = (it * 2).roundToInt() / 2.0
                        targetLevel = stepped.coerceIn(minLevel, maxLevel)
                    },
                    valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatLevel(maxLevel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Selected target level
            Text(
                text = "${Strings.ivTargetLevel(language)}: ${formatLevel(targetLevel)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(6.dp))

            // Projected CP / HP
            if (projection != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = "${Strings.ivProjectedCp(language)}: ${projection.first}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${Strings.ivProjectedHp(language)}: ${projection.second}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ── Cost table ────────────────────────────────────────
            if (cost.dust > 0) {
                Spacer(Modifier.height(8.dp))

                Text(
                    text = "${Strings.ivCost(language)} (${formatLevel(result.level)} → ${formatLevel(targetLevel)}):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CostChip(
                        label = Strings.ivDust(language),
                        value = formatNumber(cost.dust),
                        modifier = Modifier.weight(1f)
                    )
                    CostChip(
                        label = Strings.ivCandyLabel(language),
                        value = cost.candy.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    CostChip(
                        label = Strings.ivXlCandyLabel(language),
                        value = cost.xlCandy.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── GO Name (copyable) ─────────────────────────────────
            val roundedPct = result.perfection.roundToInt()
            val goName = buildGoName(pokemonName, roundedPct)
            val clipboardManager = LocalClipboardManager.current

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = goName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(goName))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Small chip displaying a label + value for cost breakdown.
 */
@Composable
private fun CostChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Formats level for display: integer levels show as "20", half-levels as "20.5".
 */
private fun formatLevel(level: Double): String {
    return if (level == level.toLong().toDouble()) {
        level.toLong().toString()
    } else {
        String.format("%.1f", level)
    }
}

private fun formatPerfection(perfection: Double, language: AppLanguage): String {
    val rounded = perfection.roundToInt()
    return when (language) {
        AppLanguage.EN -> "$rounded% perfect"
        AppLanguage.ES -> "$rounded% perfecto"
    }
}

/**
 * Builds a Pokémon GO–style name string: PokémonName + superíndice %.
 * Truncates the Pokémon name by 2 chars if the result exceeds 12 characters.
 *
 * Example: "Mewtwo" + 93% → "Mewtwo⁹³"
 *          "Fletchinder" + 93% → "Fletchind⁹³" (13→11, truncated)
 */
private fun buildGoName(pokemonName: String, perfection: Int): String {
    val superScript = perfection.toString().map {
        when (it) {
            '0' -> '⁰'
            '1' -> '¹'
            '2' -> '²'
            '3' -> '³'
            '4' -> '⁴'
            '5' -> '⁵'
            '6' -> '⁶'
            '7' -> '⁷'
            '8' -> '⁸'
            '9' -> '⁹'
            else -> it
        }
    }.joinToString("")

    val maxLen = 12
    var name = pokemonName

    while (name.length + superScript.length > maxLen && name.length > 1) {
        name = name.dropLast(1)
    }

    return name + superScript
}

/**
 * Formats an integer with locale-aware grouping (e.g. 58000 → "58,000" / "58.000").
 */
private fun formatNumber(value: Int): String {
    return String.format("%,d", value)
}
