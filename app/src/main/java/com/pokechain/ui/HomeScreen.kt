package com.pokechain.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pokechain.R
import com.pokechain.data.models.AppLanguage
import com.pokechain.data.models.Strings
import com.pokechain.ui.components.LanguageSelector

import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun rememberVersionName(): String {
    val context = LocalContext.current
    return remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
        } catch (_: Exception) {
            "?"
        }
    }
}

@Composable
fun HomeScreen(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onChainClick: () -> Unit,
    onTypesClick: () -> Unit,
    onIvCalcClick: () -> Unit,
    onShowcaseClick: () -> Unit
) {
    val versionName = rememberVersionName()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                text = "PokeChain",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "v$versionName",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            LanguageSelector(
                selected = language,
                onSelect = onLanguageChange
            )

            Spacer(Modifier.height(48.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HomeCard(
                        icon = Icons.Default.Shield,
                        title = Strings.typesSection(language),
                        subtitle = when (language) {
                            AppLanguage.EN -> "Check types, weaknesses & rankings"
                            AppLanguage.ES -> "Consultar tipos, debilidades y rankings"
                        },
                        onClick = onTypesClick,
                        modifier = Modifier.weight(1f)
                    )
                    HomeCard(
                        icon = Icons.Default.Cable,
                        title = Strings.chainSection(language),
                        subtitle = when (language) {
                            AppLanguage.EN -> "Generate search strings"
                            AppLanguage.ES -> "Generar cadenas de búsqueda"
                        },
                        onClick = onChainClick,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HomeCard(
                        icon = Icons.Default.Star,
                        title = Strings.ivCalcSection(language),
                        subtitle = Strings.ivCheckStats(language),
                        onClick = onIvCalcClick,
                        modifier = Modifier.weight(1f)
                    )
                    HomeCard(
                        icon = Icons.Default.EmojiEvents,
                        title = Strings.showcaseSection(language),
                        subtitle = Strings.showcaseSubtitle(language),
                        onClick = onShowcaseClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // App icon at the bottom, subtle
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher),
                contentDescription = "PokeChain",
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .alpha(0.5f)
            )
        }
    }
}

@Composable
private fun HomeCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(160.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
