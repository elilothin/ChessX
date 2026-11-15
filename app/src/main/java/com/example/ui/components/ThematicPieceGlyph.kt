package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.PieceColor
import com.example.domain.model.PieceType
import com.example.domain.model.UniverseId

/**
 * Renders crisp, unmistakable chess silhouettes fused with each universe's visual accent
 * (Western star/revolver aura, Greece bronze/laurel, Japan katana/sakura crest,
 * Basketball jersey/ball glow, Futuristic cyan/violet holo lines).
 */
@Composable
fun ThematicPieceGlyph(
    type: PieceType,
    color: PieceColor,
    universe: UniverseId,
    size: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    val fill = if (color == PieceColor.WHITE) Color(0xFFFDF8EC) else Color(0xFF1A1714)
    val outline = if (color == PieceColor.WHITE) Color(0xFF3E3018) else universe.primaryColor
    val detail = if (color == PieceColor.WHITE) universe.primaryColor else universe.accentColor

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // Subtle thematic aura ring/emblem behind the piece
            when (universe) {
                UniverseId.CLASSIQUE -> Unit
                UniverseId.WESTERN -> {
                    drawCircle(
                        color = detail.copy(alpha = 0.22f),
                        radius = w * 0.42f,
                        style = Stroke(width = w * 0.04f)
                    )
                }
                UniverseId.GRECE -> {
                    drawArc(
                        color = detail.copy(alpha = 0.45f),
                        startAngle = 140f,
                        sweepAngle = 260f,
                        useCenter = false,
                        topLeft = Offset(w * 0.08f, h * 0.08f),
                        size = Size(w * 0.84f, h * 0.84f),
                        style = Stroke(width = w * 0.055f, cap = StrokeCap.Round)
                    )
                }
                UniverseId.JAPON -> {
                    // Twin diagonal katana/sakura slash lines behind silhouette
                    drawLine(
                        color = detail.copy(alpha = 0.42f),
                        start = Offset(w * 0.14f, h * 0.82f),
                        end = Offset(w * 0.86f, h * 0.18f),
                        strokeWidth = w * 0.05f,
                        cap = StrokeCap.Round
                    )
                }
                UniverseId.BASKET -> {
                    drawCircle(
                        color = detail.copy(alpha = 0.25f),
                        radius = w * 0.39f
                    )
                }
                UniverseId.FUTURISTE -> {
                    // Hexagonal holo-frame
                    val hex = Path().apply {
                        moveTo(w * 0.5f, h * 0.06f)
                        lineTo(w * 0.90f, h * 0.28f)
                        lineTo(w * 0.90f, h * 0.72f)
                        lineTo(w * 0.5f, h * 0.94f)
                        lineTo(w * 0.10f, h * 0.72f)
                        lineTo(w * 0.10f, h * 0.28f)
                        close()
                    }
                    drawPath(hex, color = detail.copy(alpha = 0.36f), style = Stroke(width = w * 0.045f))
                }
            }

            drawChessPiecePath(type, w, h, fill, outline, detail)
        }
    }
}

private fun DrawScope.drawChessPiecePath(
    type: PieceType,
    w: Float,
    h: Float,
    fill: Color,
    outline: Color,
    accent: Color
) {
    val strokeWidth = (w * 0.06f).coerceAtLeast(1.5f)
    when (type) {
        PieceType.PAWN -> {
            val base = Path().apply {
                moveTo(w * 0.26f, h * 0.86f)
                lineTo(w * 0.74f, h * 0.86f)
                lineTo(w * 0.68f, h * 0.72f)
                lineTo(w * 0.58f, h * 0.48f)
                lineTo(w * 0.42f, h * 0.48f)
                lineTo(w * 0.32f, h * 0.72f)
                close()
            }
            drawPath(base, fill)
            drawPath(base, outline, style = Stroke(strokeWidth, join = StrokeJoin.Round))
            drawCircle(fill, radius = w * 0.17f, center = Offset(w * 0.5f, h * 0.31f))
            drawCircle(outline, radius = w * 0.17f, center = Offset(w * 0.5f, h * 0.31f), style = Stroke(strokeWidth))
            drawCircle(accent, radius = w * 0.055f, center = Offset(w * 0.5f, h * 0.31f))
        }
        PieceType.ROOK -> {
            val path = Path().apply {
                moveTo(w * 0.22f, h * 0.86f)
                lineTo(w * 0.78f, h * 0.86f)
                lineTo(w * 0.72f, h * 0.74f)
                lineTo(w * 0.68f, h * 0.36f)
                lineTo(w * 0.74f, h * 0.28f)
                lineTo(w * 0.74f, h * 0.16f)
                lineTo(w * 0.62f, h * 0.16f)
                lineTo(w * 0.62f, h * 0.24f)
                lineTo(w * 0.55f, h * 0.24f)
                lineTo(w * 0.55f, h * 0.16f)
                lineTo(w * 0.45f, h * 0.16f)
                lineTo(w * 0.45f, h * 0.24f)
                lineTo(w * 0.38f, h * 0.24f)
                lineTo(w * 0.38f, h * 0.16f)
                lineTo(w * 0.26f, h * 0.16f)
                lineTo(w * 0.26f, h * 0.28f)
                lineTo(w * 0.32f, h * 0.36f)
                lineTo(w * 0.28f, h * 0.74f)
                close()
            }
            drawPath(path, fill)
            drawPath(path, outline, style = Stroke(strokeWidth, join = StrokeJoin.Round))
            drawLine(accent, Offset(w * 0.35f, h * 0.55f), Offset(w * 0.65f, h * 0.55f), strokeWidth = strokeWidth)
        }
        PieceType.KNIGHT -> {
            val path = Path().apply {
                moveTo(w * 0.24f, h * 0.86f)
                lineTo(w * 0.78f, h * 0.86f)
                lineTo(w * 0.74f, h * 0.68f)
                lineTo(w * 0.66f, h * 0.44f)
                lineTo(w * 0.72f, h * 0.26f)
                lineTo(w * 0.58f, h * 0.16f)
                lineTo(w * 0.46f, h * 0.22f)
                lineTo(w * 0.24f, h * 0.38f)
                lineTo(w * 0.30f, h * 0.50f)
                lineTo(w * 0.48f, h * 0.44f)
                lineTo(w * 0.36f, h * 0.68f)
                close()
            }
            drawPath(path, fill)
            drawPath(path, outline, style = Stroke(strokeWidth, join = StrokeJoin.Round))
            drawCircle(accent, radius = w * 0.055f, center = Offset(w * 0.46f, h * 0.31f))
        }
        PieceType.BISHOP -> {
            val path = Path().apply {
                moveTo(w * 0.26f, h * 0.86f)
                lineTo(w * 0.74f, h * 0.86f)
                lineTo(w * 0.66f, h * 0.72f)
                lineTo(w * 0.62f, h * 0.46f)
                lineTo(w * 0.50f, h * 0.20f)
                lineTo(w * 0.38f, h * 0.46f)
                lineTo(w * 0.34f, h * 0.72f)
                close()
            }
            drawPath(path, fill)
            drawPath(path, outline, style = Stroke(strokeWidth, join = StrokeJoin.Round))
            drawCircle(fill, radius = w * 0.065f, center = Offset(w * 0.50f, h * 0.15f))
            drawCircle(outline, radius = w * 0.065f, center = Offset(w * 0.50f, h * 0.15f), style = Stroke(strokeWidth * 0.8f))
            drawLine(accent, Offset(w * 0.44f, h * 0.38f), Offset(w * 0.56f, h * 0.50f), strokeWidth = strokeWidth)
        }
        PieceType.QUEEN -> {
            val path = Path().apply {
                moveTo(w * 0.22f, h * 0.86f)
                lineTo(w * 0.78f, h * 0.86f)
                lineTo(w * 0.72f, h * 0.68f)
                lineTo(w * 0.82f, h * 0.30f)
                lineTo(w * 0.64f, h * 0.48f)
                lineTo(w * 0.50f, h * 0.22f)
                lineTo(w * 0.36f, h * 0.48f)
                lineTo(w * 0.18f, h * 0.30f)
                lineTo(w * 0.28f, h * 0.68f)
                close()
            }
            drawPath(path, fill)
            drawPath(path, outline, style = Stroke(strokeWidth, join = StrokeJoin.Round))
            for (pt in listOf(Offset(w * 0.18f, h * 0.26f), Offset(w * 0.50f, h * 0.17f), Offset(w * 0.82f, h * 0.26f))) {
                drawCircle(accent, radius = w * 0.05f, center = pt)
            }
        }
        PieceType.KING -> {
            val path = Path().apply {
                moveTo(w * 0.22f, h * 0.86f)
                lineTo(w * 0.78f, h * 0.86f)
                lineTo(w * 0.72f, h * 0.66f)
                lineTo(w * 0.78f, h * 0.40f)
                lineTo(w * 0.60f, h * 0.46f)
                lineTo(w * 0.50f, h * 0.30f)
                lineTo(w * 0.40f, h * 0.46f)
                lineTo(w * 0.22f, h * 0.40f)
                lineTo(w * 0.28f, h * 0.66f)
                close()
            }
            drawPath(path, fill)
            drawPath(path, outline, style = Stroke(strokeWidth, join = StrokeJoin.Round))
            // Royal cross
            drawLine(accent, Offset(w * 0.50f, h * 0.10f), Offset(w * 0.50f, h * 0.28f), strokeWidth = strokeWidth * 1.2f, cap = StrokeCap.Round)
            drawLine(accent, Offset(w * 0.41f, h * 0.17f), Offset(w * 0.59f, h * 0.17f), strokeWidth = strokeWidth * 1.2f, cap = StrokeCap.Round)
        }
    }
}
