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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.MatchRecord
import com.example.domain.model.UniverseId
import com.example.ui.theme.CrimsonCheck
import com.example.ui.theme.EmeraldVictory
import com.example.ui.theme.ImperialGold
import com.example.ui.theme.MutedSandText
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    records: List<MatchRecord>,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                title = { Text("Historique des Parties", style = MaterialTheme.typography.titleLarge, color = ImperialGold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("history_back_button")) {
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (records.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp)
                            .padding(top = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        border = BorderStroke(1.dp, ObsidianCardBorder),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Aucune partie enregistrée", style = MaterialTheme.typography.titleMedium, color = ImperialGold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Terminez un duel en Campagne ou en Ligne pour voir vos résultats et variations Elo ici.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedSandText
                            )
                        }
                    }
                }
            } else {
                items(records, key = { it.id }) { rec ->
                    val oppUniv = UniverseId.fromId(rec.opponentUniverse)
                    val resultColor = when (rec.result) {
                        "WIN" -> EmeraldVictory
                        "LOSS" -> CrimsonCheck
                        else -> ImperialGold
                    }
                    val resultLabel = when (rec.result) {
                        "WIN" -> "Victoire"
                        "LOSS" -> "Défaite"
                        else -> "Nulle"
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        border = BorderStroke(1.dp, ObsidianCardBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${if (rec.mode == "CAMPAIGN") "Campagne" else "En ligne"} · vs ${rec.opponentName}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${oppUniv.displayName} · ${rec.movesCount} coups · ${rec.endReason}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedSandText
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(resultLabel, style = MaterialTheme.typography.labelLarge, color = resultColor)
                                val deltaStr = if (rec.eloDelta >= 0) "+${rec.eloDelta}" else "${rec.eloDelta}"
                                Text("$deltaStr Elo (${rec.eloAfter})", style = MaterialTheme.typography.labelSmall, color = ImperialGold)
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
