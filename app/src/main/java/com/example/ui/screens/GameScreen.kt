package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.PieceColor
import com.example.domain.model.PieceType
import com.example.domain.model.Square
import com.example.domain.model.UniverseId
import com.example.ui.ActiveGameState
import com.example.ui.components.BoardView
import com.example.ui.components.CaptureCombatOverlay
import com.example.ui.components.EndgameCutsceneDialog
import com.example.ui.components.ThematicPieceGlyph
import com.example.ui.theme.CrimsonCheck
import com.example.ui.theme.ImperialGold
import com.example.ui.theme.MutedSandText
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface

@Composable
fun GameScreen(
    gameState: ActiveGameState,
    onSquareTapped: (Square) -> Unit,
    onSelectPromotion: (PieceType) -> Unit,
    onCancelPromotion: () -> Unit,
    onResign: () -> Unit,
    onCancelWaiting: () -> Unit,
    onSkipCaptureAnimation: () -> Unit,
    onPlayAgainCampaign: ((UniverseId) -> Unit)?,
    onExitToMenu: () -> Unit
) {
    var showResignDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (gameState.activeEndgame != null) {
            onExitToMenu()
        } else if (gameState.isWaitingForOpponent) {
            onCancelWaiting()
        } else {
            showResignDialog = true
        }
    }

    val topIsWhite = gameState.playerColor == PieceColor.BLACK
    val topName = if (topIsWhite) gameState.whiteName else gameState.blackName
    val topUniverse = if (topIsWhite) gameState.whiteUniverse else gameState.blackUniverse
    val topElo = if (topIsWhite) gameState.whiteElo else gameState.blackElo
    val topTimeMs = if (topIsWhite) gameState.whiteTimeMs else gameState.blackTimeMs
    val isTopTurn = gameState.board.turn == (if (topIsWhite) PieceColor.WHITE else PieceColor.BLACK)

    val bottomName = if (topIsWhite) gameState.blackName else gameState.whiteName
    val bottomUniverse = if (topIsWhite) gameState.blackUniverse else gameState.whiteUniverse
    val bottomElo = if (topIsWhite) gameState.blackElo else gameState.whiteElo
    val bottomTimeMs = if (topIsWhite) gameState.blackTimeMs else gameState.whiteTimeMs
    val isBottomTurn = gameState.board.turn == gameState.playerColor

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (gameState.activeEndgame != null) onExitToMenu()
                        else if (gameState.isWaitingForOpponent) onCancelWaiting()
                        else showResignDialog = true
                    },
                    modifier = Modifier.testTag("game_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quitter", tint = ImperialGold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (gameState.isOnline) "EN LIGNE · ${gameState.timeControlMinutes} MIN" else "CAMPAGNE · ${gameState.campaignTargetUniverse.displayName.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ImperialGold
                    )
                    Text(
                        text = gameState.boardUniverse.boardName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSandText
                    )
                }

                IconButton(
                    onClick = { showResignDialog = true },
                    enabled = !gameState.isWaitingForOpponent && gameState.activeEndgame == null,
                    modifier = Modifier.testTag("resign_button")
                ) {
                    Icon(Icons.Filled.Flag, contentDescription = "Abandonner", tint = CrimsonCheck)
                }
            }

            if (gameState.isOnline && gameState.animationsCutoffByTwoMinRule) {
                Surface(
                    color = ImperialGold.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                ) {
                    Text(
                        text = if (gameState.timeControlMinutes <= 1) {
                            "Blitz 1 min : animations de combat désactivées"
                        } else {
                            "Seuil des 2 minutes atteint : animations coupées pour la fin de partie"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = ImperialGold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Opponent Banner
            PlayerClockCard(
                name = topName,
                universe = topUniverse,
                elo = topElo,
                isActiveTurn = isTopTurn,
                isTimed = gameState.isOnline,
                timeMs = topTimeMs,
                isThinking = !gameState.isOnline && gameState.isAiThinking,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
            )

            // Chessboard
            BoardView(
                board = gameState.board,
                playerPerspective = gameState.playerColor,
                boardUniverse = gameState.boardUniverse,
                whiteUniverse = gameState.whiteUniverse,
                blackUniverse = gameState.blackUniverse,
                selectedSquare = gameState.selectedSquare,
                legalMovesForSelected = gameState.legalMovesForSelected,
                lastMoveSquares = gameState.lastMoveSquares,
                onSquareTapped = onSquareTapped,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
            )

            // Player Banner
            PlayerClockCard(
                name = bottomName,
                universe = bottomUniverse,
                elo = bottomElo,
                isActiveTurn = isBottomTurn,
                isTimed = gameState.isOnline,
                timeMs = bottomTimeMs,
                isThinking = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
            )

            // Move History Strip
            if (gameState.moveHistory.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(gameState.moveHistory) { idx, moveStr ->
                            val prefix = if (idx % 2 == 0) "${idx / 2 + 1}." else ""
                            Text(
                                text = "$prefix$moveStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (idx == gameState.moveHistory.lastIndex) ImperialGold else MutedSandText
                            )
                        }
                    }
                }
            }
        }

        // Shared Check Banner
        AnimatedVisibility(
            visible = gameState.showCheckBanner,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 74.dp)
        ) {
            Surface(
                color = CrimsonCheck,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 6.dp
            ) {
                Text(
                    text = "ÉCHEC AU ROI !",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }

        // Promotion Reveal Flash
        AnimatedVisibility(
            visible = gameState.showPromotionFlash,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 74.dp)
        ) {
            Surface(
                color = ImperialGold,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 6.dp
            ) {
                Text(
                    text = "PROMOTION !",
                    style = MaterialTheme.typography.titleMedium,
                    color = ObsidianBg,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }

        // Waiting for Online Opponent Overlay
        if (gameState.isWaitingForOpponent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.widthIn(max = 400.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(1.5.dp, ImperialGold),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(color = ImperialGold)
                        Text("En attente d'un adversaire...", style = MaterialTheme.typography.titleLarge, color = ImperialGold)
                        Text(
                            "Votre salon (${gameState.timeControlMinutes} min · ${gameState.whiteUniverse.displayName}) est ouvert en direct.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSandText
                        )
                        OutlinedButton(
                            onClick = onCancelWaiting,
                            border = BorderStroke(1.dp, CrimsonCheck),
                            modifier = Modifier.testTag("cancel_waiting_button")
                        ) {
                            Text("Annuler le salon", color = CrimsonCheck)
                        }
                    }
                }
            }
        }

        // Pawn Promotion Selection Dialog
        if (gameState.pendingPromotionMoves.isNotEmpty()) {
            val ownUniverse = if (gameState.playerColor == PieceColor.WHITE) gameState.whiteUniverse else gameState.blackUniverse
            AlertDialog(
                onDismissRequest = onCancelPromotion,
                containerColor = ObsidianSurface,
                title = { Text("Promotion du Pion", color = ImperialGold) },
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach { pt ->
                            Card(
                                onClick = { onSelectPromotion(pt) },
                                colors = CardDefaults.cardColors(containerColor = ObsidianBg),
                                border = BorderStroke(1.dp, ownUniverse.primaryColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("promote_${pt.name.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    ThematicPieceGlyph(pt, gameState.playerColor, ownUniverse, size = 40.dp)
                                    Text(pt.frenchName, style = MaterialTheme.typography.labelSmall, color = ImperialGold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }

        // Resign Confirmation Dialog
        if (showResignDialog) {
            AlertDialog(
                onDismissRequest = { showResignDialog = false },
                containerColor = ObsidianSurface,
                title = { Text("Abandonner la partie ?", color = ImperialGold) },
                text = {
                    Text(
                        if (gameState.isOnline) "Votre adversaire remportera la partie et la cinématique d'abandon sera jouée."
                        else "En mode campagne, votre Elo ne baisse pas lors d'une défaite.",
                        color = MutedSandText
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showResignDialog = false
                            onResign()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonCheck),
                        modifier = Modifier.testTag("confirm_resign_button")
                    ) {
                        Text("Abandonner")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResignDialog = false }) {
                        Text("Continuer", color = ImperialGold)
                    }
                }
            )
        }

        // 3-Second Manga Layered Capture Cutscene
        CaptureCombatOverlay(
            capture = gameState.activeCapture,
            onSkip = onSkipCaptureAnimation
        )

        // Checkmate / Resign / Timeout / Draw Cutscene
        EndgameCutsceneDialog(
            event = gameState.activeEndgame,
            playerColor = gameState.playerColor,
            onPlayAgain = if (!gameState.isOnline && onPlayAgainCampaign != null) {
                { onPlayAgainCampaign(gameState.campaignTargetUniverse) }
            } else null,
            onReturnToMenu = onExitToMenu
        )
    }
}

@Composable
private fun PlayerClockCard(
    name: String,
    universe: UniverseId,
    elo: Int,
    isActiveTurn: Boolean,
    isTimed: Boolean,
    timeMs: Long,
    isThinking: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(
            width = if (isActiveTurn) 1.5.dp else 1.dp,
            color = if (isActiveTurn) universe.primaryColor else ObsidianCardBorder
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                ThematicPieceGlyph(
                    type = PieceType.KING,
                    color = if (isActiveTurn) PieceColor.WHITE else PieceColor.BLACK,
                    universe = universe,
                    size = 34.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${universe.displayName} · $elo Elo${if (isThinking) " · Réflexion..." else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSandText
                    )
                }
            }

            if (isTimed) {
                val totalSecs = (timeMs / 1000L).coerceAtLeast(0L)
                val mins = totalSecs / 60L
                val secs = totalSecs % 60L
                val isLow = totalSecs <= 120L
                Surface(
                    color = if (isLow) CrimsonCheck.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, if (isLow) CrimsonCheck else ObsidianCardBorder),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Timer, contentDescription = null, tint = if (isLow) CrimsonCheck else ImperialGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "%02d:%02d".format(mins, secs),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isLow) CrimsonCheck else ImperialGold
                        )
                    }
                }
            }
        }
    }
}
