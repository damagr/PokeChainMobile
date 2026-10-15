package com.pokechain.ui.iv

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ScreenshotMonitor
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
import com.pokechain.data.iv.IvFormat
import com.pokechain.data.iv.IvResult
import com.pokechain.data.models.AppLanguage
import com.pokechain.data.models.BaseStats
import com.pokechain.data.models.PokemonType
import com.pokechain.data.models.Strings
import com.pokechain.data.scanner.ScannerState
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

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

    // ── Escáner en vivo (lectura de pantalla de Pokémon GO) ───────
    val scanResult by ScannerState.lastResult.collectAsState()
    val scanRunning by ScannerState.isRunning.collectAsState()

    val projectionManager = context.getSystemService(MediaProjectionManager::class.java)

    val overlaySettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* el usuario vuelve de Ajustes: retry manual con el icono */ }

    val projectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ScannerState.language = language
            ScannerState.reset()
            com.pokechain.data.scanner.ScreenScannerService.start(
                context, result.resultCode, result.data!!
            )
        }
    }

    fun startScannerFlow() {
        if (!Settings.canDrawOverlays(context)) {
            // El overlay flotante necesita el permiso especial: guiar a Ajustes
            overlaySettingsLauncher.launch(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
            )
        } else {
            projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
        }
    }

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

    /** Rellena los campos de la calculadora con el resultado del escáner. */
    fun fillFromScan(scan: com.pokechain.data.scanner.ScanResult) {
        scan.cp?.let { cpText = it.toString() }
        scan.ivAtk?.let { atkIvText = it.toString() }
        scan.ivDef?.let { defIvText = it.toString() }
        scan.ivSta?.let { staIvText = it.toString() }
        hasCalculated = false
        errorMessage = null

        val dex = scan.dex
        val name = scan.speciesName
        if (dex != null || name != null) {
            scope.launch {
                try {
                    typeProvider.ensureLoaded()
                    val entry = typeProvider.searchAll(name ?: "", translator, language)
                        .firstOrNull { dex == null || it.dex == dex }
                    entry?.let { e ->
                        selectedEntry = e
                        val dn = e.displayName(language, translator)
                        searchText = TextFieldValue(dn, selection = TextRange(dn.length))
                    }
                } catch (_: Exception) {
                    // La selección manual del Pokémon siempre está disponible
                }
            }
        }
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
                title = { Text(Strings.ivCalculator(language)) },
                actions = {
                    // Escáner en vivo: lee PC e IVs de la pantalla de Pokémon GO
                    IconButton(
                        onClick = { startScannerFlow() },
                        enabled = !isLoadingTypes
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenshotMonitor,
                            contentDescription = Strings.ivScanStart(language)
                        )
                    }
                }
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
                        if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.toIntOrNull() in 0..15)) {
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
                        if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.toIntOrNull() in 0..15)) {
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
                        if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.toIntOrNull() in 0..15)) {
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

            // ── Último escaneo (escáner en vivo) ──────────────────
            scanResult?.let { scan ->
                if (scan.ivAtk != null && scan.ivDef != null && scan.ivSta != null) {
                    val pct = scan.perfection?.roundToInt() ?: 0
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Strings.ivScanLast(language),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(IvFormat.superscript(pct)))
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = Strings.ivScanCopy(language),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${IvFormat.superscript(pct)}  ${scan.ivAtk}/${scan.ivDef}/${scan.ivSta}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Button(
                                onClick = { fillFromScan(scan) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(Strings.ivScanFill(language))
                            }
                        }
                    }
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
    // Máx de la tabla CPM (51.0 = best buddy); evita minLevel > maxLevel
    // cuando findLevel devuelve niveles 50.5/51.0 (crash de rango invertido)
    val maxLevel = 51.0
    val minLevel = result.level
    val hasRange = maxLevel > minLevel
    var targetLevel by remember { mutableStateOf(minLevel) }

    // ── Shadow cost state ────────────────────────────────────────
    var isShadow by remember { mutableStateOf(false) }

    // Ensure target stays in bounds when result.level changes
    LaunchedEffect(minLevel) {
        if (targetLevel < minLevel) targetLevel = minLevel
    }

    val projection = remember(targetLevel, baseStats) {
        if (baseStats == null) null
        else IvCalculator.calculateAtLevel(baseStats.atk, baseStats.def, baseStats.hp, atkIv, defIv, staIv, targetLevel)
    }
    val cost = remember(result.level, targetLevel, isShadow) {
        IvCalculator.getPowerUpCost(result.level, targetLevel, isShadow)
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

            // Target level slider (solo si hay rango; nivel 51 = rango degenerado)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hasRange) {
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

            // ── Shadow checkbox: siempre visible en la card ───────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Strings.ivShadowCostTitle(language),
                    style = MaterialTheme.typography.labelMedium,
                    color = PokemonType.POISON.color
                )
                Checkbox(
                    checked = isShadow,
                    onCheckedChange = { isShadow = it },
                    enabled = cost.dust > 0
                )
            }

            // ── Cost table (chips, solo si hay coste) ─────────────
            if (cost.dust > 0) {
                Text(
                    text = "${Strings.ivCost(language)} (${formatLevel(result.level)} → ${formatLevel(targetLevel)}):",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isShadow) PokemonType.POISON.color
                           else MaterialTheme.colorScheme.onSurfaceVariant
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
    val superScript = IvFormat.superscript(perfection)
    val maxLen = 12
    var name = pokemonName
    while (name.length + superScript.length > maxLen && name.length > 1) name = name.dropLast(1)
    return name + superScript
}

/**
 * Formats an integer with locale-aware grouping (e.g. 58000 → "58,000" / "58.000").
 */
private fun formatNumber(value: Int): String {
    return String.format("%,d", value)
}
