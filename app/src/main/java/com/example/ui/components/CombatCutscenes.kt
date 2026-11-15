package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CaptureEvent
import com.example.domain.model.EndgameEvent
import com.example.domain.model.EndgameType
import com.example.domain.model.PieceColor
import com.example.domain.model.UniverseCatalog
import com.example.domain.model.UniverseId
import com.example.ui.theme.CrimsonCheck
import com.example.ui.theme.EmeraldVictory
import com.example.ui.theme.ImperialGold
import com.example.ui.theme.MutedSandText
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import kotlin.math.roundToInt

/**
 * Manga-panel layered 3-second capture combat cutscene (PDF pages 10-12):
 * - Planche 1 (Foreground): Close-up of capturing piece in its player's universe.
 * - Planche 2 (Background/Rear): Captured piece ALWAYS rendered as classical ("lambda") model,
 *   reacting according to its reaction family (Tranché, Abattu, Écrasé, Brûlé, Entraîné).
 */
@Composable
fun CaptureCombatOverlay(
    capture: CaptureEvent?,
    onSkip: () -> Unit
) {
    AnimatedVisibility(
        visible = capture != null,
        enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220)),
        exit = fadeOut(tween(200))
    ) {
        if (capture == null) return@AnimatedVisibility
        val pieceIdentity = UniverseCatalog.getPieceIdentity(capture.attackerUniverse, capture.attackerType)
        val slideProgress = remember(capture.id) { Animatable(0f) }

        LaunchedEffect(capture.id) {
            slideProgress.animateTo(1f, animationSpec = tween(550, easing = FastOutSlowInEasing))
        }

        val impactOnomatopoeia = when (pieceIdentity.reactionFamily) {
            "Tranché" -> "ZASH!!"
            "Abattu" -> "BANG!!"
            "Écrasé" -> "DOOM!!"
            "Brûlé" -> "BOOM!!"
            else -> "WHOOSH!!"
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.80f))
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(2.dp, capture.attackerUniverse.primaryColor)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Manga speed-lines canvas behind the layered panels
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val lineColor = capture.attackerUniverse.primaryColor.copy(alpha = 0.16f)
                        for (i in 0..14) {
                            val y = (i / 14f) * size.height
                            drawLine(
                                color = lineColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y - 36f),
                                strokeWidth = if (i % 3 == 0) 4f else 2f
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = capture.attackerUniverse.primaryColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, capture.attackerUniverse.primaryColor)
                            ) {
                                Text(
                                    text = "${capture.attackerUniverse.displayName.uppercase()} · COMBAT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = capture.attackerUniverse.accentColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            TextButton(
                                onClick = onSkip,
                                modifier = Modifier.testTag("skip_capture_animation_button")
                            ) {
                                Text("Passer", color = MutedSandText)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Filled.SkipNext, contentDescription = "Passer l'animation", tint = MutedSandText, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Planche Manga: Gros plan capturante devant, victime classique derrière
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(168.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            capture.attackerUniverse.boardFrameColor,
                                            Color(0xFF0A0908)
                                        )
                                    )
                                )
                                .padding(14.dp)
                        ) {
                            // Background Layer: Classical ("lambda") victim reacting behind
                            Column(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .offset {
                                        IntOffset(
                                            x = ((1f - slideProgress.value) * -28f).roundToInt(),
                                            y = (slideProgress.value * 8f).roundToInt()
                                        )
                                    }
                                    .alpha(0.85f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ThematicPieceGlyph(
                                    type = capture.victimType,
                                    color = capture.victimColor,
                                    universe = UniverseId.CLASSIQUE, // Toujours modèle classique ("lambda")
                                    size = 70.dp
                                )
                                Text(
                                    text = "${capture.victimType.frenchName} (${pieceIdentity.reactionFamily})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFF8A80)
                                )
                            }

                            // Manga SFX burst in center
                            Text(
                                text = impactOnomatopoeia,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontStyle = FontStyle.Italic,
                                color = capture.attackerUniverse.accentColor,
                                modifier = Modifier.align(Alignment.Center)
                            )

                            // Foreground Layer: Capturing warrior close-up
                            Column(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .offset {
                                        IntOffset(
                                            x = ((1f - slideProgress.value) * -60f).roundToInt(),
                                            y = 0
                                        )
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ThematicPieceGlyph(
                                    type = capture.attackerType,
                                    color = capture.attackerColor,
                                    universe = capture.attackerUniverse,
                                    size = 92.dp
                                )
                                Text(
                                    text = pieceIdentity.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = capture.attackerUniverse.accentColor
                                )
                            }
                        }

                        // Narrative manga caption
                        Text(
                            text = pieceIdentity.attackName,
                            style = MaterialTheme.typography.titleLarge,
                            color = ImperialGold
                        )
                        Text(
                            text = pieceIdentity.captureDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EndgameCutsceneDialog(
    event: EndgameEvent?,
    playerColor: PieceColor,
    onPlayAgain: (() -> Unit)?,
    onReturnToMenu: () -> Unit
) {
    if (event == null) return
    val isVictory = event.winnerColor == playerColor
    val isDraw = event.winnerColor == null
    val bannerColor = when {
        isDraw -> ImperialGold
        isVictory -> EmeraldVictory
        else -> CrimsonCheck
    }
    val headline = when {
        isDraw -> "MATCH NUL"
        isVictory -> "VICTOIRE !"
        else -> "DÉFAITE"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.86f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = BorderStroke(2.dp, event.winnerUniverse.primaryColor)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                event.winnerUniverse.bannerRes?.let { resId ->
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = event.winnerUniverse.displayName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Icon(
                    imageVector = if (isVictory) Icons.Filled.EmojiEvents else Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = bannerColor,
                    modifier = Modifier.size(40.dp)
                )

                Text(
                    text = headline,
                    style = MaterialTheme.typography.headlineMedium,
                    color = bannerColor
                )

                Text(
                    text = "${event.winnerUniverse.displayName} · ${event.title}",
                    style = MaterialTheme.typography.titleMedium,
                    color = event.winnerUniverse.accentColor,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("VARIATION ELO", style = MaterialTheme.typography.labelSmall, color = MutedSandText)
                        val deltaStr = if (event.eloDelta >= 0) "+${event.eloDelta}" else "${event.eloDelta}"
                        Text(deltaStr, style = MaterialTheme.typography.titleLarge, color = if (event.eloDelta >= 0) EmeraldVictory else CrimsonCheck)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NOUVEL ELO", style = MaterialTheme.typography.labelSmall, color = MutedSandText)
                        Text("${event.newElo}", style = MaterialTheme.typography.titleLarge, color = ImperialGold)
                    }
                }

                if (event.unlockedUniverse != null) {
                    Surface(
                        color = ImperialGold.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, ImperialGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "NOUVEL UNIVERS DÉBLOQUÉ !",
                                style = MaterialTheme.typography.labelLarge,
                                color = ImperialGold
                            )
                            Text(
                                text = "Pièces & plateau ${event.unlockedUniverse.displayName} + évolution de l'Îlot : ${event.unlockedUniverse.islandTitle}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (onPlayAgain != null) {
                    Button(
                        onClick = onPlayAgain,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("endgame_play_again_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ImperialGold,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Rejouer", style = MaterialTheme.typography.labelLarge)
                    }
                }

                OutlinedButton(
                    onClick = onReturnToMenu,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("endgame_return_button"),
                    border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Retour", color = ImperialGold)
                }
            }
        }
    }
}
