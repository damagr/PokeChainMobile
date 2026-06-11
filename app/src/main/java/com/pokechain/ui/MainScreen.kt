package com.pokechain.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.pokechain.ui.pve.PvEScreen
import com.pokechain.ui.pvp.PvPScreen

import com.pokechain.ui.components.LanguageSelector
import com.pokechain.data.models.AppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    var language by remember { mutableStateOf(AppLanguage.EN) }
    val tabs = listOf("PvP", "PvE")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("PokeChain") },
                actions = {
                    LanguageSelector(
                        selected = language,
                        onSelect = { language = it }
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
                0 -> PvPScreen(language = language)
                1 -> PvEScreen(language = language)
            }
        }
    }
}
