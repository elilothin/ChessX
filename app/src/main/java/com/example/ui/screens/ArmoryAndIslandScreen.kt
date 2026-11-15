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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.PieceColor
import com.example.domain.model.PieceType
import com.example.domain.model.UniverseCatalog
import com.example.domain.model.UniverseId
import com.example.domain.model.UserProfile
import com.example.ui.components.ThematicPieceGlyph
import com.example.ui.theme.EmeraldVictory
import com.example.ui.theme.ImperialGold
import com.example.ui.theme.MutedSandText
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArmoryAndIslandScreen(
    profile: UserProfile,
    onBack: () -> Unit,
    onSelectLoadout: (UniverseId, UniverseId) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var codexUniverse by remember { mutableStateOf(UniverseId.WESTERN) }

    val selectedPieces = UniverseId.fromId(profile.selectedUniverse)
    val selectedBoard = UniverseId.fromId(profile.selectedBoard)
    val islandUniverse = UniverseId.entries.firstOrNull { it.campaignOrder == profile.islandLevel.toInt() } ?: UniverseId.CLASSIQUE

    Scaffold(
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                title = { Text("Pièces, Plateaux & Îlot", style = MaterialTheme.typography.titleLarge, color = ImperialGold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("armory_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = ImperialGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = ObsidianSurface,
                contentColor = ImperialGold
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Équipement & Îlot") },
                    modifier = Modifier.testTag("tab_loadout")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Codex (30 Pouvoirs)") },
                    modifier = Modifier.testTag("tab_codex")
                )
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item { Spacer(modifier = Modifier.height(6.dp)) }

                    // Island Evolution Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp),
                            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                            border = BorderStroke(1.5.dp, islandUniverse.primaryColor),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Landscape, contentDescription = null, tint = islandUniverse.primaryColor)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Votre Îlot · Niveau ${profile.islandLevel} / 5",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = islandUniverse.accentColor
                                    )
                                }
                                Text(islandUniverse.islandTitle, style = MaterialTheme.typography.titleLarge, color = ImperialGold)
                                Text(islandUniverse.islandDescription, style = MaterialTheme.typography.bodyMedium, color = MutedSandText)
                            }
                        }
                    }

                    // Universe Equipment Cards
                    items(UniverseId.entries) { universe ->
                        val isUnlocked = profile.isUnlocked(universe)
                        val isPiecesEquipped = selectedPieces == universe
                        val isBoardEquipped = selectedBoard == universe

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp),
                            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                            border = BorderStroke(
                                width = if (isPiecesEquipped || isBoardEquipped) 1.5.dp else 1.dp,
                                color = if (isPiecesEquipped || isBoardEquipped) universe.primaryColor else ObsidianCardBorder
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(universe.displayName, style = MaterialTheme.typography.titleMedium, color = universe.accentColor)
                                        Text(universe.boardName, style = MaterialTheme.typography.bodyMedium, color = MutedSandText)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        ThematicPieceGlyph(PieceType.KING, PieceColor.WHITE, universe, size = 32.dp)
                                        ThematicPieceGlyph(PieceType.QUEEN, PieceColor.BLACK, universe, size = 32.dp)
                                    }
                                }

                                if (!isUnlocked) {
                                    Surface(
                                        color = Color.White.copy(alpha = 0.06f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Filled.Lock, contentDescription = null, tint = MutedSandText, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Débloquez avec 5 victoires contre ${universe.displayName} en Campagne.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MutedSandText
                                            )
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onSelectLoadout(universe, selectedBoard) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isPiecesEquipped) EmeraldVictory else universe.primaryColor,
                                                contentColor = ObsidianBg
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            if (isPiecesEquipped) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isPiecesEquipped) "Pièces actives" else "Équiper pièces")
                                        }
                                        OutlinedButton(
                                            onClick = { onSelectLoadout(selectedPieces, universe) },
                                            modifier = Modifier.weight(1f),
                                            border = BorderStroke(1.dp, if (isBoardEquipped) EmeraldVictory else universe.primaryColor),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = if (isBoardEquipped) "Plateau actif" else "Équiper plateau",
                                                color = if (isBoardEquipped) EmeraldVictory else universe.accentColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            } else {
                // Codex des 30 Pouvoirs & Fins de partie
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp)
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(UniverseId.campaignSequence) { univ ->
                                FilterChip(
                                    selected = codexUniverse == univ,
                                    onClick = { codexUniverse = univ },
                                    label = { Text(univ.displayName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = univ.primaryColor,
                                        selectedLabelColor = ObsidianBg
                                    )
                                )
                            }
                        }
                    }

                    items(PieceType.entries) { pieceType ->
                        val info = UniverseCatalog.getPieceIdentity(codexUniverse, pieceType)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp),
                            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                            border = BorderStroke(1.dp, codexUniverse.primaryColor.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ThematicPieceGlyph(
                                    type = pieceType,
                                    color = PieceColor.WHITE,
                                    universe = codexUniverse,
                                    size = 54.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "${pieceType.frenchName} · ${info.title}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = codexUniverse.accentColor
                                    )
                                    Text(info.identity, style = MaterialTheme.typography.bodyMedium, color = MutedSandText)
                                    Text(
                                        text = "${info.attackName} (${info.reactionFamily}) : ${info.captureDescription}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp),
                            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                            border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Cinématiques de fin de partie · ${codexUniverse.displayName}", style = MaterialTheme.typography.titleMedium, color = ImperialGold)
                                Text("Échec et mat : ${codexUniverse.checkmateTitle} — ${codexUniverse.checkmateDescription}", style = MaterialTheme.typography.bodyMedium)
                                Text("Abandon : ${codexUniverse.resignTitle} — ${codexUniverse.resignDescription}", style = MaterialTheme.typography.bodyMedium, color = MutedSandText)
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}
