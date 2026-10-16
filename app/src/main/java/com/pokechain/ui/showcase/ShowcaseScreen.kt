package com.pokechain.ui.showcase

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
import com.pokechain.data.models.AppLanguage
import com.pokechain.data.models.Strings
import com.pokechain.data.scanner.ScanResult
import com.pokechain.data.scanner.ScannerMode
import com.pokechain.data.scanner.ScannerState
import com.pokechain.data.scanner.ScreenScannerService
import com.pokechain.data.showcase.ShowcaseCalculator
import com.pokechain.data.showcase.ShowcaseDataProvider
import com.pokechain.data.showcase.ShowcaseResult
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowcaseScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val translator = remember { NameTranslator(context) }
    val typeProvider = remember { PokemonTypeProvider() }
    val showcaseProvider = remember { ShowcaseDataProvider(context) }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // ── Pokémon search state ──────────────────────────────────────
    var searchText by remember { mutableStateOf(TextFieldValue("")) }
    var showDropdown by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<PokemonTypeEntry?>(null) }
    var isLoadingTypes by remember { mutableStateOf(true) }
    var typeLoadError by remember { mutableStateOf<String?>(null) }

    // ── Input state ───────────────────────────────────────────────
    var heightText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    var atkIvText by remember { mutableStateOf("") }
    var defIvText by remember { mutableStateOf("") }
    var hpIvText by remember { mutableStateOf("") }

    // ── Results state ─────────────────────────────────────────────
    var result by remember { mutableStateOf<ShowcaseResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasCalculated by remember { mutableStateOf(false) }
    var speciesName by remember { mutableStateOf("") }

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

    // ── Escáner en vivo (lectura de pantalla de Pokémon GO) ───────
    LaunchedEffect(Unit) { ScannerState.mode = ScannerMode.SHOWCASE }
    val scanResult by ScannerState.lastResult.collectAsState()

    val projectionManager = context.getSystemService(MediaProjectionManager::class.java)

    val overlaySettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* vuelve de Ajustes: retry manual con el icono */ }

    val projectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ScannerState.language = language
            ScannerState.mode = ScannerMode.SHOWCASE
            ScannerState.reset()
            ScreenScannerService.start(context, result.resultCode, result.data!!)
        }
    }

    fun startScannerFlow() {
        if (!Settings.canDrawOverlays(context)) {
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

    /** Rellena altura/peso/IVs y selecciona la especie escaneada. */
    fun fillFromScan(scan: ScanResult) {
        scan.heightM?.let { heightText = String.format(java.util.Locale.US, "%.2f", it) }
        scan.weightKg?.let { weightText = String.format(java.util.Locale.US, "%.2f", it) }
        scan.ivAtk?.let { atkIvText = it.toString() }
        scan.ivDef?.let { defIvText = it.toString() }
        scan.ivSta?.let { hpIvText = it.toString() }
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
        result = null

        val entry = selectedEntry
        if (entry == null) {
            errorMessage = Strings.showcaseEnterValid(language)
            return
        }

        val height = heightText.toDoubleOrNull()
        val weight = weightText.toDoubleOrNull()
        if (height == null || weight == null || height <= 0 || weight <= 0) {
            errorMessage = Strings.showcaseEnterValid(language)
            return
        }

        // Extract form suffix from speciesId (e.g. "pikachu_libre" → "libre")
        val formSuffix = entry.speciesId.substringAfter("_", "").ifEmpty { null }
        val sizeEntry = showcaseProvider.getSize(entry.dex, formSuffix)
        if (sizeEntry == null) {
            errorMessage = Strings.showcaseNoData(language)
            return
        }

        speciesName = sizeEntry.name

        val atk = atkIvText.toIntOrNull()
        val def = defIvText.toIntOrNull()
        val hp = hpIvText.toIntOrNull()
        val haveAllIvs = atk != null && def != null && hp != null

        result = ShowcaseCalculator.calculate(
            size = sizeEntry.size,
            inputHeight = height,
            inputWeight = weight,
            atkIv = if (haveAllIvs) atk else null,
            defIv = if (haveAllIvs) def else null,
            hpIv = if (haveAllIvs) hp else null
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
                title = { Text(Strings.showcaseCalculator(language)) },
                actions = {
                    // Escáner en vivo: lee nombre, peso y altura de Pokémon GO
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
                        heightText = ""
                        weightText = ""
                        atkIvText = ""
                        defIvText = ""
                        hpIvText = ""
                        result = null
                        hasCalculated = false
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable),
                    placeholder = { Text(Strings.showcaseSelectPokemon(language)) },
                    singleLine = true,
                    trailingIcon = {
                        if (searchText.text.isNotEmpty()) {
                            IconButton(onClick = {
                                searchText = TextFieldValue("")
                                selectedEntry = null
                                heightText = ""
                                weightText = ""
                                atkIvText = ""
                                defIvText = ""
                                hpIvText = ""
                                result = null
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
                                heightText = ""
                                weightText = ""
                                atkIvText = ""
                                defIvText = ""
                                hpIvText = ""
                                result = null
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

            // ── Height input ──────────────────────────────────────
            OutlinedTextField(
                value = heightText,
                onValueChange = {
                    heightText = it.replace(',', '.')
                    hasCalculated = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(Strings.showcaseHeight(language)) },
                placeholder = { Text("e.g. 2.10") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            // ── Weight input ──────────────────────────────────────
            OutlinedTextField(
                value = weightText,
                onValueChange = {
                    weightText = it.replace(',', '.')
                    hasCalculated = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(Strings.showcaseWeight(language)) },
                placeholder = { Text("e.g. 120.5") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            // ── IV inputs (optional) ──────────────────────────────
            Text(
                text = Strings.showcaseIvsOptional(language),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                    value = hpIvText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.toIntOrNull() in 0..15)) {
                            hpIvText = newValue
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
                Text(Strings.showcaseCalculate(language))
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
                if (scan.heightM != null && scan.weightKg != null) {
                    val copyText = scan.showcaseTotal?.let { "$it/1178" }
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
                                if (copyText != null) {
                                    IconButton(onClick = {
                                        clipboardManager.setText(AnnotatedString(copyText))
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = Strings.ivScanCopy(language),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${scan.heightM} m · ${scan.weightKg} kg" +
                                    (scan.showcaseTotal?.let { "  →  $it/1178" } ?: ""),
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

            // ── Result ────────────────────────────────────────────
            if (hasCalculated && result != null) {
                ShowcaseResultCard(result = result!!, speciesName = speciesName, language = language)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ShowcaseResultCard(
    result: ShowcaseResult,
    speciesName: String,
    language: AppLanguage
) {
    val tier = when {
        result.total >= 1100 -> Pair(
            Strings.showcaseTrophy(language),
            Color(0xFF34D399) // emerald-400
        )
        result.total >= 1000 -> Pair(
            Strings.showcaseExcellent(language),
            Color(0xFF34D399)
        )
        result.total >= 850 -> Pair(
            Strings.showcaseGood(language),
            MaterialTheme.colorScheme.primary
        )
        else -> Pair(
            Strings.showcaseBelow(language),
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Header: species name + total score ─────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = speciesName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = result.total.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = Strings.showcaseMax(language),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Badges row ────────────────────────────────────────
            Row(
                modifier = Modifier.padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (result.isXXL) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "XXL",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                if (result.isXXS) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "XXS",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Tier label ────────────────────────────────────────
            Text(
                text = tier.first,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = tier.second
            )

            Spacer(Modifier.height(8.dp))

            // ── Breakdown ─────────────────────────────────────────
            Text(
                text = Strings.showcaseBreakdown(language),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))

            BreakdownRow(
                label = when (language) { AppLanguage.EN -> "Height"; AppLanguage.ES -> "Altura" },
                value = result.heightPts.roundToInt().toString(),
                modifier = Modifier.fillMaxWidth()
            )
            BreakdownRow(
                label = when (language) { AppLanguage.EN -> "Weight"; AppLanguage.ES -> "Peso" },
                value = result.weightPts.roundToInt().toString(),
                modifier = Modifier.fillMaxWidth()
            )
            BreakdownRow(
                label = Strings.showcaseIvLabel(language),
                value = if (result.haveIvs) result.ivPts.roundToInt().toString() else "—",
                modifier = Modifier.fillMaxWidth()
            )
            BreakdownRow(
                label = Strings.showcaseXxlBonus(language),
                value = if (result.xxlPts > 0) result.xxlPts.toString() else "—",
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${"%.2f".format(result.ratio)} ${Strings.showcaseRatio(language)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (result.capped) {
                    Text(
                        text = when (language) {
                            AppLanguage.EN -> "Capped at species max"
                            AppLanguage.ES -> "Limitado al máximo de la especie"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFBBF24) // amber-400
                    )
                }
            }

            // ── Copy score (GO name) ────────────────────────────────
            val goName = "${result.total}/1178"
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

@Composable
private fun BreakdownRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}
