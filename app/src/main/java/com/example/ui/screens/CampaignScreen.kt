package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.domain.model.UniverseId
import com.example.domain.model.UserProfile
import com.example.ui.theme.EmeraldVictory
import com.example.ui.theme.ImperialGold
import com.example.ui.theme.MutedSandText
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignScreen(
    profile: UserProfile,
    onBack: () -> Unit,
    onStartCampaignMatch: (UniverseId) -> Unit
) {
    val activeUniverse = UniverseId.fromId(profile.selectedUniverse)

    Scaffold(
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mode Campagne", style = MaterialTheme.typography.titleLarge, color = ImperialGold)
                        Text("Elo : ${profile.campaignElo} · +100 Elo / victoire", style = MaterialTheme.typography.bodyMedium, color = MutedSandText)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("campaign_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = ImperialGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Votre point de départ : Pièces ${activeUniverse.displayName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = ImperialGold
                        )
                        Text(
                            text = "5 victoires (non consécutives) par univers débloquent ses pièces, son plateau et font évoluer votre Îlot. Une défaite ne fait pas baisser votre Elo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSandText
                        )
                    }
                }
            }

            itemsIndexed(UniverseId.campaignSequence) { index, universe ->
                val wins = profile.winsFor(universe)
                val isCompleted = wins >= 5
                val isUnlockedToFight = profile.isCampaignTargetAvailable(universe)
                val isPiecesUnlocked = profile.isUnlocked(universe)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(
                        width = if (isUnlockedToFight && !isCompleted) 2.dp else 1.dp,
                        color = if (isCompleted) EmeraldVictory else if (isUnlockedToFight) universe.primaryColor else ObsidianCardBorder
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (universe.bannerRes != null) {
                            Image(
                                painter = painterResource(id = universe.bannerRes),
                                contentDescription = universe.displayName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(124.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Bespoke Basket-ball hardwood arena header
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(92.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF4E270E), universe.primaryColor.copy(alpha = 0.75f), Color(0xFF1F2433))
                                        )
                                    )
                                    .padding(14.dp),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                Text(
                                    text = "ARÈNE PARQUET ALL-STAR · BUZZER-BEATER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Étape ${index + 1} · ${universe.displayName}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = universe.accentColor
                                )
                                Text(
                                    text = "${universe.aiBossName} — ${universe.subtitle}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedSandText
                                )
                            }
                            if (isCompleted) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "Conquis", tint = EmeraldVictory)
                            } else if (!isUnlockedToFight) {
                                Icon(Icons.Filled.Lock, contentDescription = "Verrouillé", tint = MutedSandText)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Progression : $wins / 5 victoires",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isPiecesUnlocked) "Pièces & plateau débloqués" else "Récompense : Pièces + Plateau",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPiecesUnlocked) EmeraldVictory else ImperialGold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (wins / 5f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (isCompleted) EmeraldVictory else universe.primaryColor,
                                trackColor = Color.White.copy(alpha = 0.10f)
                            )
                        }

                        Button(
                            onClick = { onStartCampaignMatch(universe) },
                            enabled = isUnlockedToFight,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("campaign_play_${universe.id.lowercase()}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = universe.primaryColor,
                                contentColor = ObsidianBg,
                                disabledContainerColor = Color.White.copy(alpha = 0.08f),
                                disabledContentColor = MutedSandText
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isUnlockedToFight) Icons.Filled.PlayArrow else Icons.Filled.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    !isUnlockedToFight -> "Terminez l'univers précédent"
                                    isCompleted -> "Réaffronter ${universe.displayName}"
                                    else -> "Affronter ${universe.aiBossName}"
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
