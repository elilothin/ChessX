package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.OnlineMatch
import com.example.domain.model.UniverseId
import com.example.domain.model.UserProfile
import com.example.ui.theme.ImperialGold
import com.example.ui.theme.MutedSandText
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineLobbyScreen(
    profile: UserProfile,
    waitingMatches: List<OnlineMatch>,
    onBack: () -> Unit,
    onCreateMatch: (Int) -> Unit,
    onJoinMatch: (OnlineMatch) -> Unit
) {
    var selectedMinutes by remember { mutableIntStateOf(5) }
    val activeUniverse = UniverseId.fromId(profile.selectedUniverse)
    val activeBoard = UniverseId.fromId(profile.selectedBoard)

    Scaffold(
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mode En Ligne", style = MaterialTheme.typography.titleLarge, color = ImperialGold)
                        Text("Elo : ${profile.onlineElo} · ${activeUniverse.displayName}", style = MaterialTheme.typography.bodyMedium, color = MutedSandText)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("online_back_button")) {
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
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(1.5.dp, ImperialGold.copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Créer un salon en ligne", style = MaterialTheme.typography.titleLarge, color = ImperialGold)
                        Text(
                            text = "Pièces : ${activeUniverse.displayName} · Plateau : ${activeBoard.boardName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSandText
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1, 5, 10).forEach { mins ->
                                FilterChip(
                                    selected = selectedMinutes == mins,
                                    onClick = { selectedMinutes = mins },
                                    label = { Text("$mins min") },
                                    leadingIcon = { Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("cadence_${mins}_chip"),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ImperialGold,
                                        selectedLabelColor = ObsidianBg,
                                        selectedLeadingIconColor = ObsidianBg
                                    )
                                )
                            }
                        }

                        Text(
                            text = if (selectedMinutes == 1) {
                                "Blitz 1 min : partie ultra-rapide, aucune animation de combat."
                            } else {
                                "Cadence $selectedMinutes min : animations de capture actives, coupées automatiquement pour les deux joueurs dès 2:00 restantes."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSandText
                        )

                        Button(
                            onClick = { onCreateMatch(selectedMinutes) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("create_online_match_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ImperialGold,
                                contentColor = ObsidianBg
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Créer une partie ($selectedMinutes min)", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Public, contentDescription = null, tint = ImperialGold, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salons ouverts (${waitingMatches.size})", style = MaterialTheme.typography.titleMedium, color = ImperialGold)
                }
            }

            if (waitingMatches.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        border = BorderStroke(1.dp, ObsidianCardBorder),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Aucun salon ouvert pour le moment", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Créez une partie ci-dessus pour inviter un joueur en temps réel.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedSandText
                            )
                        }
                    }
                }
            } else {
                items(waitingMatches, key = { it.id }) { match ->
                    val hostUniv = UniverseId.fromId(match.whiteUniverse)
                    val isOwn = match.whitePlayerId == profile.userId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        border = BorderStroke(1.dp, hostUniv.primaryColor.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${match.whiteDisplayName} (${match.whiteElo} Elo)",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Thème : ${hostUniv.displayName} · Blitz ${match.timeControlMinutes} min",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedSandText
                                )
                            }
                            Button(
                                onClick = { onJoinMatch(match) },
                                modifier = Modifier.testTag("join_match_${match.id}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = hostUniv.primaryColor,
                                    contentColor = ObsidianBg
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(if (isOwn) "Reprendre" else "Rejoindre")
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
