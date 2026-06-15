package com.pokechain.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pokechain.R
import com.pokechain.ui.pve.PvEScreen
import com.pokechain.ui.pvp.PvPScreen
import com.pokechain.ui.clean.CleanScreen
import com.pokechain.data.models.AppLanguage
import com.pokechain.data.models.Strings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChainScreen(
    language: AppLanguage,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("PvP", "PvE", Strings.cleanTab(language))

    var advancedMode by remember { mutableStateOf(false) }
    var tapCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(tapCount) {
        if (tapCount > 0) {
            delay(1500)
            tapCount = 0
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                title = {
                    if (advancedMode) {
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = Color(0xFFFFD600))) { append("Cadena") }
                                withStyle(SpanStyle(color = Color(0xFFFF1744))) { append(" Avanzada") }
                            }
                        )
                    } else {
                        Text(Strings.chainSection(language))
                    }
                },
                actions = {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = "PokeChain",
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                tapCount++
                                if (tapCount >= 5) {
                                    tapCount = 0
                                    advancedMode = !advancedMode
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (advancedMode) Strings.advancedModeOn(language)
                                            else Strings.advancedModeOff(language)
                                        )
                                    }
                                }
                            }
                    )
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            when (selectedTab) {
                0 -> PvPScreen(language = language, advancedMode = advancedMode)
                1 -> PvEScreen(language = language, advancedMode = advancedMode)
                2 -> CleanScreen(language = language)
            }
        }
    }
}
