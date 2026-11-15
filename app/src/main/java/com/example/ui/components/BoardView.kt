package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.chess.ChessEngine
import com.example.domain.model.BoardState
import com.example.domain.model.ChessMove
import com.example.domain.model.PieceColor
import com.example.domain.model.PieceType
import com.example.domain.model.Square
import com.example.domain.model.UniverseId
import com.example.ui.theme.CrimsonCheck
import com.example.ui.theme.ImperialGold

@Composable
fun BoardView(
    board: BoardState,
    playerPerspective: PieceColor,
    boardUniverse: UniverseId,
    whiteUniverse: UniverseId,
    blackUniverse: UniverseId,
    selectedSquare: Square?,
    legalMovesForSelected: List<ChessMove>,
    lastMoveSquares: Pair<Square, Square>?,
    onSquareTapped: (Square) -> Unit,
    modifier: Modifier = Modifier
) {
    val legalTargetSet = legalMovesForSelected.map { it.to }.toSet()
    val checkedKingSquare = if (ChessEngine.isInCheck(board, board.turn)) {
        ChessEngine.findKing(board, board.turn)
    } else null

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(14.dp),
        color = boardUniverse.boardFrameColor,
        border = BorderStroke(2.5.dp, boardUniverse.primaryColor.copy(alpha = 0.75f)),
        shadowElevation = 8.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            val cellSize = maxWidth / 8f

            // Universe-specific board surface backdrop watermark (parquet lines, holo grid, marble veins, sakura)
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Base grid squares
                for (displayRow in 0..7) {
                    for (displayCol in 0..7) {
                        val r = if (playerPerspective == PieceColor.WHITE) displayRow else 7 - displayRow
                        val c = if (playerPerspective == PieceColor.WHITE) displayCol else 7 - displayCol
                        val isLight = (r + c) % 2 == 0
                        val squareColor = if (isLight) boardUniverse.lightSquareColor else boardUniverse.darkSquareColor
                        drawRect(
                            color = squareColor,
                            topLeft = Offset(displayCol * cellSize.toPx(), displayRow * cellSize.toPx()),
                            size = Size(cellSize.toPx(), cellSize.toPx())
                        )
                    }
                }

                // Thematic board markings per PDF Section VI
                when (boardUniverse) {
                    UniverseId.BASKET -> {
                        val lineColor = Color.White.copy(alpha = 0.25f)
                        drawCircle(
                            color = lineColor,
                            radius = size.width * 0.16f,
                            center = center,
                            style = Stroke(width = 3f)
                        )
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, size.height / 2f),
                            end = Offset(size.width, size.height / 2f),
                            strokeWidth = 3f
                        )
                    }
                    UniverseId.FUTURISTE -> {
                        val neonColor = Color(0xFF00E5FF).copy(alpha = 0.30f)
                        for (i in 1..7) {
                            val pos = i * cellSize.toPx()
                            drawLine(neonColor, Offset(pos, 0f), Offset(pos, size.height), strokeWidth = 1.5f)
                            drawLine(neonColor, Offset(0f, pos), Offset(size.width, pos), strokeWidth = 1.5f)
                        }
                    }
                    UniverseId.JAPON -> {
                        // Subtle cherry blossom petals in corners
                        val petalColor = Color(0xFFFFB7C5).copy(alpha = 0.35f)
                        drawCircle(petalColor, radius = 10f, center = Offset(size.width * 0.12f, size.height * 0.14f))
                        drawCircle(petalColor, radius = 7f, center = Offset(size.width * 0.84f, size.height * 0.22f))
                        drawCircle(petalColor, radius = 9f, center = Offset(size.width * 0.20f, size.height * 0.85f))
                    }
                    else -> Unit
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                for (displayRow in 0..7) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        for (displayCol in 0..7) {
                            val r = if (playerPerspective == PieceColor.WHITE) displayRow else 7 - displayRow
                            val c = if (playerPerspective == PieceColor.WHITE) displayCol else 7 - displayCol
                            val sq = Square(r, c)
                            val piece = board.pieceAt(sq)
                            val isSelected = sq == selectedSquare
                            val isLastMove = lastMoveSquares?.let { it.first == sq || it.second == sq } == true
                            val isLegalTarget = sq in legalTargetSet
                            val isKingInCheck = sq == checkedKingSquare

                            val overlayColor = when {
                                isKingInCheck -> CrimsonCheck.copy(alpha = 0.55f)
                                isSelected -> ImperialGold.copy(alpha = 0.48f)
                                isLastMove -> boardUniverse.accentColor.copy(alpha = 0.26f)
                                else -> Color.Transparent
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .background(overlayColor)
                                    .clickable { onSquareTapped(sq) }
                                    .testTag("square_${sq.toAlgebraic()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                // Algebraic coordinates on outer edges
                                if (displayCol == 0) {
                                    Text(
                                        text = "${8 - r}",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if ((r + c) % 2 == 0) boardUniverse.darkSquareColor else boardUniverse.lightSquareColor,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(start = 2.dp, top = 1.dp)
                                    )
                                }
                                if (displayRow == 7) {
                                    Text(
                                        text = "${'a' + c}",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if ((r + c) % 2 == 0) boardUniverse.darkSquareColor else boardUniverse.lightSquareColor,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(end = 2.dp, bottom = 1.dp)
                                    )
                                }

                                if (piece != null) {
                                    val pieceUniverse = if (piece.color == PieceColor.WHITE) whiteUniverse else blackUniverse
                                    ThematicPieceGlyph(
                                        type = piece.type,
                                        color = piece.color,
                                        universe = pieceUniverse,
                                        size = cellSize * 0.84f
                                    )
                                }

                                if (isLegalTarget) {
                                    if (piece != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize(0.88f)
                                                .border(2.5.dp, ImperialGold, CircleShape)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(cellSize * 0.30f)
                                                .clip(CircleShape)
                                                .background(ImperialGold.copy(alpha = 0.72f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
