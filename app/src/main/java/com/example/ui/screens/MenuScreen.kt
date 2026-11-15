package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.model.UniverseId
import com.example.domain.model.UserProfile
import com.example.ui.theme.ImperialGold
import com.example.ui.theme.ImperialGoldLight
import com.example.ui.theme.MutedSandText
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant

@Composable
fun MenuScreen(
    profile: UserProfile,
    onOpenCampaign: () -> Unit,
    onOpenOnline: () -> Unit,
    onOpenArmory: () -> Unit,
    onOpenHistory: () -> Unit,
    onToggleAnimations: (Boolean) -> Unit,
    onSignOut: () -> Unit
) {
    val activeUniverse = UniverseId.fromId(profile.selectedUniverse)
    val totalCampaignWins = (profile.westernWins + profile.greceWins + profile.japonWins + profile.basketWins + profile.futuristeWins).toInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ObsidianBg, Color(0xFF14100B), ObsidianBg)
                )
            )
            .safeDrawingPadding()
    ) {
        // Subtle minimalist chessboard watermark at bottom
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.BottomCenter)
        ) {
            val cols = 8
            val rows = 2
            val cell = size.width / cols
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    if ((r + c) % 2 == 0) {
                        drawRect(
                            color = ImperialGold.copy(alpha = 0.045f),
                            topLeft = Offset(c * cell, size.height - (rows - r) * cell),
                            size = Size(cell, cell)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Top bar: Player & Sign Out
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = profile.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = ImperialGoldLight
                        )
                        Text(
                            text = "Équipé : ${activeUniverse.displayName} · Îlot Niv.${profile.islandLevel}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSandText
                        )
                    }
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Se déconnecter",
                            tint = MutedSandText
                        )
                    }
                }

                // Minimalist Chess X Hero Emblem
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CHESS X",
                        style = MaterialTheme.typography.displayLarge,
                        color = ImperialGold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "LE JEU D'ÉCHECS DES CINQ UNIVERS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSandText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Compact Elo status bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatBadge(
                            label = "ELO CAMPAGNE",
                            value = "${profile.campaignElo}",
                            sub = "$totalCampaignWins / 25 victoires",
                            modifier = Modifier.weight(1f)
                        )
                        StatBadge(
                            label = "ELO EN LIGNE",
                            value = "${profile.onlineElo}",
                            sub = "${profile.onlineWins}V · ${profile.onlineLosses}D",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Primary Menu Choice 1: Mode Campagne
                Card(
                    onClick = onOpenCampaign,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("menu_campaign_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(1.5.dp, ImperialGold)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = ImperialGold.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = null,
                                    tint = ImperialGold,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mode campagne",
                                style = MaterialTheme.typography.titleLarge,
                                color = ImperialGold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Affrontez l'IA à travers 5 univers (Western → Futuriste), débloquez pièces, plateaux et votre îlot.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedSandText
                            )
                        }
                    }
                }

                // Primary Menu Choice 2: Jouer en ligne
                Card(
                    onClick = onOpenOnline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("menu_online_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceVariant),
                    border = BorderStroke(1.5.dp, activeUniverse.primaryColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = activeUniverse.primaryColor.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Public,
                                    contentDescription = null,
                                    tint = activeUniverse.primaryColor,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Jouer en ligne",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Cadences 1, 5 ou 10 min · Thèmes mixtes en direct · Classement Elo Chess.com.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedSandText
                            )
                        }
                    }
                }

                // Secondary Actions: Armurerie & Îlot + Historique
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenArmory,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("menu_armory_button"),
                        border = BorderStroke(1.dp, ObsidianCardBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = ImperialGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pièces & Îlot", color = MaterialTheme.colorScheme.onSurface)
                    }
                    OutlinedButton(
                        onClick = onOpenHistory,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("menu_history_button"),
                        border = BorderStroke(1.dp, ObsidianCardBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.History, contentDescription = null, tint = ImperialGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Historique", color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Combat animations toggle (Question ouverte XI)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = ImperialGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Animations de capture manga", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    "Planches de combat (3 s) lors des prises",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedSandText
                                )
                            }
                        }
                        Switch(
                            checked = profile.animationsEnabled,
                            onCheckedChange = onToggleAnimations,
                            modifier = Modifier.testTag("toggle_animations_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBg,
                                checkedTrackColor = ImperialGold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBadge(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MutedSandText)
            Text(value, style = MaterialTheme.typography.headlineMedium, color = ImperialGold, fontWeight = FontWeight.Bold)
            Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
