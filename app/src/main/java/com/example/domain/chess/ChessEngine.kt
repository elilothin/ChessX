package com.example.domain.chess

import com.example.domain.model.BoardState
import com.example.domain.model.CastlingRights
import com.example.domain.model.ChessMove
import com.example.domain.model.ChessPiece
import com.example.domain.model.GameOutcome
import com.example.domain.model.PieceColor
import com.example.domain.model.PieceType
import com.example.domain.model.Square
import kotlin.math.abs

object ChessEngine {
    const val INITIAL_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    fun initialBoard(): BoardState = fromFen(INITIAL_FEN)

    fun fromFen(fen: String): BoardState {
        val parts = fen.trim().split(Regex("\\s+"))
        val rows = parts[0].split("/")
        val grid = MutableList(8) { MutableList<ChessPiece?>(8) { null } }
        for (r in 0 until minOf(8, rows.size)) {
            var c = 0
            for (ch in rows[r]) {
                if (ch.isDigit()) {
                    c += ch.digitToInt()
                } else if (c < 8) {
                    val type = PieceType.fromChar(ch)
                    if (type != null) {
                        val color = if (ch.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
                        grid[r][c] = ChessPiece(type, color)
                    }
                    c++
                }
            }
        }
        val turn = if (parts.getOrNull(1) == "b") PieceColor.BLACK else PieceColor.WHITE
        val castleStr = parts.getOrNull(2) ?: "-"
        val castling = CastlingRights(
            whiteKingSide = 'K' in castleStr,
            whiteQueenSide = 'Q' in castleStr,
            blackKingSide = 'k' in castleStr,
            blackQueenSide = 'q' in castleStr
        )
        val ep = parts.getOrNull(3)?.let { if (it == "-") null else Square.fromAlgebraic(it) }
        val half = parts.getOrNull(4)?.toIntOrNull() ?: 0
        val full = parts.getOrNull(5)?.toIntOrNull() ?: 1
        return BoardState(grid.map { it.toList() }, turn, castling, ep, half, full)
    }

    fun toFen(board: BoardState): String {
        val sb = StringBuilder()
        for (r in 0..7) {
            var empty = 0
            for (c in 0..7) {
                val p = board.grid[r][c]
                if (p == null) {
                    empty++
                } else {
                    if (empty > 0) {
                        sb.append(empty)
                        empty = 0
                    }
                    sb.append(p.fenChar())
                }
            }
            if (empty > 0) sb.append(empty)
            if (r < 7) sb.append('/')
        }
        sb.append(' ').append(board.turn.fenChar())
        sb.append(' ').append(board.castling.toFen())
        sb.append(' ').append(board.enPassantTarget?.toAlgebraic() ?: "-")
        sb.append(' ').append(board.halfMoveClock)
        sb.append(' ').append(board.fullMoveNumber)
        return sb.toString()
    }

    fun legalMoves(board: BoardState): List<ChessMove> {
        val pseudo = generatePseudoLegalMoves(board, board.turn)
        return pseudo.filter { move ->
            val next = applyMoveUnchecked(board, move)
            !isInCheck(next, board.turn)
        }
    }

    fun legalMovesFrom(board: BoardState, from: Square): List<ChessMove> {
        val piece = board.pieceAt(from) ?: return emptyList()
        if (piece.color != board.turn) return emptyList()
        return legalMoves(board).filter { it.from == from }
    }

    fun applyMove(board: BoardState, move: ChessMove): BoardState {
        return applyMoveUnchecked(board, move)
    }

    fun isInCheck(board: BoardState, color: PieceColor): Boolean {
        val kingSq = findKing(board, color) ?: return false
        return isSquareAttacked(board, kingSq, color.opposite())
    }

    fun evaluateOutcome(board: BoardState): GameOutcome {
        val moves = legalMoves(board)
        if (moves.isEmpty()) {
            return if (isInCheck(board, board.turn)) {
                if (board.turn == PieceColor.WHITE) GameOutcome.CHECKMATE_BLACK_WINS
                else GameOutcome.CHECKMATE_WHITE_WINS
            } else {
                GameOutcome.STALEMATE
            }
        }
        if (board.halfMoveClock >= 100) return GameOutcome.DRAW_FIFTY_MOVE
        if (isInsufficientMaterial(board)) return GameOutcome.DRAW_INSUFFICIENT_MATERIAL
        return GameOutcome.ONGOING
    }

    fun findKing(board: BoardState, color: PieceColor): Square? {
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board.grid[r][c]
                if (p != null && p.color == color && p.type == PieceType.KING) {
                    return Square(r, c)
                }
            }
        }
        return null
    }

    private fun isInsufficientMaterial(board: BoardState): Boolean {
        val whitePieces = mutableListOf<Pair<PieceType, Square>>()
        val blackPieces = mutableListOf<Pair<PieceType, Square>>()
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board.grid[r][c] ?: continue
                if (p.type != PieceType.KING) {
                    if (p.color == PieceColor.WHITE) whitePieces.add(p.type to Square(r, c))
                    else blackPieces.add(p.type to Square(r, c))
                }
            }
        }
        if (whitePieces.isEmpty() && blackPieces.isEmpty()) return true
        if (whitePieces.size == 1 && blackPieces.isEmpty() &&
            (whitePieces[0].first == PieceType.BISHOP || whitePieces[0].first == PieceType.KNIGHT)
        ) return true
        if (blackPieces.size == 1 && whitePieces.isEmpty() &&
            (blackPieces[0].first == PieceType.BISHOP || blackPieces[0].first == PieceType.KNIGHT)
        ) return true
        if (whitePieces.size == 1 && blackPieces.size == 1 &&
            whitePieces[0].first == PieceType.BISHOP && blackPieces[0].first == PieceType.BISHOP
        ) {
            val wSq = whitePieces[0].second
            val bSq = blackPieces[0].second
            if ((wSq.row + wSq.col) % 2 == (bSq.row + bSq.col) % 2) return true
        }
        return false
    }

    private fun isSquareAttacked(board: BoardState, target: Square, byColor: PieceColor): Boolean {
        // Pawn attacks
        val pawnDir = if (byColor == PieceColor.WHITE) 1 else -1
        for (dc in listOf(-1, 1)) {
            val r = target.row + pawnDir
            val c = target.col + dc
            if (r in 0..7 && c in 0..7) {
                val p = board.grid[r][c]
                if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
            }
        }
        // Knight attacks
        val knightOffsets = listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
        for ((dr, dc) in knightOffsets) {
            val r = target.row + dr
            val c = target.col + dc
            if (r in 0..7 && c in 0..7) {
                val p = board.grid[r][c]
                if (p != null && p.color == byColor && p.type == PieceType.KNIGHT) return true
            }
        }
        // King attacks
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val r = target.row + dr
                val c = target.col + dc
                if (r in 0..7 && c in 0..7) {
                    val p = board.grid[r][c]
                    if (p != null && p.color == byColor && p.type == PieceType.KING) return true
                }
            }
        }
        // Sliding orthogonal (Rook / Queen)
        val orthoDirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        for ((dr, dc) in orthoDirs) {
            var r = target.row + dr
            var c = target.col + dc
            while (r in 0..7 && c in 0..7) {
                val p = board.grid[r][c]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) return true
                    break
                }
                r += dr
                c += dc
            }
        }
        // Sliding diagonal (Bishop / Queen)
        val diagDirs = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
        for ((dr, dc) in diagDirs) {
            var r = target.row + dr
            var c = target.col + dc
            while (r in 0..7 && c in 0..7) {
                val p = board.grid[r][c]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) return true
                    break
                }
                r += dr
                c += dc
            }
        }
        return false
    }

    private fun generatePseudoLegalMoves(board: BoardState, color: PieceColor): List<ChessMove> {
        val moves = mutableListOf<ChessMove>()
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board.grid[r][c] ?: continue
                if (piece.color != color) continue
                val from = Square(r, c)
                when (piece.type) {
                    PieceType.PAWN -> addPawnMoves(board, from, color, moves)
                    PieceType.KNIGHT -> addKnightMoves(board, from, color, moves)
                    PieceType.BISHOP -> addSlidingMoves(board, from, color, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1), moves)
                    PieceType.ROOK -> addSlidingMoves(board, from, color, listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1), moves)
                    PieceType.QUEEN -> addSlidingMoves(board, from, color, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1, -1 to 0, 1 to 0, 0 to -1, 0 to 1), moves)
                    PieceType.KING -> addKingMoves(board, from, color, moves)
                }
            }
        }
        return moves
    }

    private fun addPawnMoves(board: BoardState, from: Square, color: PieceColor, out: MutableList<ChessMove>) {
        val dir = if (color == PieceColor.WHITE) -1 else 1
        val startRow = if (color == PieceColor.WHITE) 6 else 1
        val promoRow = if (color == PieceColor.WHITE) 0 else 7
        val promos = listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)

        val oneStep = Square(from.row + dir, from.col)
        if (oneStep.isValid() && board.pieceAt(oneStep) == null) {
            if (oneStep.row == promoRow) {
                for (pr in promos) out.add(ChessMove(from, oneStep, promotion = pr))
            } else {
                out.add(ChessMove(from, oneStep))
                if (from.row == startRow) {
                    val twoStep = Square(from.row + 2 * dir, from.col)
                    if (twoStep.isValid() && board.pieceAt(twoStep) == null) {
                        out.add(ChessMove(from, twoStep))
                    }
                }
            }
        }

        for (dc in listOf(-1, 1)) {
            val capSq = Square(from.row + dir, from.col + dc)
            if (!capSq.isValid()) continue
            val target = board.pieceAt(capSq)
            if (target != null && target.color != color) {
                if (capSq.row == promoRow) {
                    for (pr in promos) out.add(ChessMove(from, capSq, promotion = pr, capturedPiece = target))
                } else {
                    out.add(ChessMove(from, capSq, capturedPiece = target))
                }
            } else if (capSq == board.enPassantTarget) {
                val epVictimSq = Square(from.row, capSq.col)
                val victim = board.pieceAt(epVictimSq)
                if (victim != null && victim.color != color && victim.type == PieceType.PAWN) {
                    out.add(ChessMove(from, capSq, isEnPassant = true, capturedPiece = victim))
                }
            }
        }
    }

    private fun addKnightMoves(board: BoardState, from: Square, color: PieceColor, out: MutableList<ChessMove>) {
        val offsets = listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
        for ((dr, dc) in offsets) {
            val to = Square(from.row + dr, from.col + dc)
            if (!to.isValid()) continue
            val target = board.pieceAt(to)
            if (target == null || target.color != color) {
                out.add(ChessMove(from, to, capturedPiece = target))
            }
        }
    }

    private fun addSlidingMoves(
        board: BoardState,
        from: Square,
        color: PieceColor,
        dirs: List<Pair<Int, Int>>,
        out: MutableList<ChessMove>
    ) {
        for ((dr, dc) in dirs) {
            var r = from.row + dr
            var c = from.col + dc
            while (r in 0..7 && c in 0..7) {
                val to = Square(r, c)
                val target = board.pieceAt(to)
                if (target == null) {
                    out.add(ChessMove(from, to))
                } else {
                    if (target.color != color) {
                        out.add(ChessMove(from, to, capturedPiece = target))
                    }
                    break
                }
                r += dr
                c += dc
            }
        }
    }

    private fun addKingMoves(board: BoardState, from: Square, color: PieceColor, out: MutableList<ChessMove>) {
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val to = Square(from.row + dr, from.col + dc)
                if (!to.isValid()) continue
                val target = board.pieceAt(to)
                if (target == null || target.color != color) {
                    out.add(ChessMove(from, to, capturedPiece = target))
                }
            }
        }
        // Castling
        val homeRow = if (color == PieceColor.WHITE) 7 else 0
        if (from.row == homeRow && from.col == 4 && !isSquareAttacked(board, from, color.opposite())) {
            val canKingSide = if (color == PieceColor.WHITE) board.castling.whiteKingSide else board.castling.blackKingSide
            if (canKingSide &&
                board.grid[homeRow][5] == null &&
                board.grid[homeRow][6] == null &&
                board.grid[homeRow][7]?.let { it.type == PieceType.ROOK && it.color == color } == true &&
                !isSquareAttacked(board, Square(homeRow, 5), color.opposite()) &&
                !isSquareAttacked(board, Square(homeRow, 6), color.opposite())
            ) {
                out.add(ChessMove(from, Square(homeRow, 6), isCastleKingSide = true))
            }

            val canQueenSide = if (color == PieceColor.WHITE) board.castling.whiteQueenSide else board.castling.blackQueenSide
            if (canQueenSide &&
                board.grid[homeRow][3] == null &&
                board.grid[homeRow][2] == null &&
                board.grid[homeRow][1] == null &&
                board.grid[homeRow][0]?.let { it.type == PieceType.ROOK && it.color == color } == true &&
                !isSquareAttacked(board, Square(homeRow, 3), color.opposite()) &&
                !isSquareAttacked(board, Square(homeRow, 2), color.opposite())
            ) {
                out.add(ChessMove(from, Square(homeRow, 2), isCastleQueenSide = true))
            }
        }
    }

    private fun applyMoveUnchecked(board: BoardState, move: ChessMove): BoardState {
        val newGrid = board.grid.map { it.toMutableList() }.toMutableList()
        val moving = newGrid[move.from.row][move.from.col]!!
        newGrid[move.from.row][move.from.col] = null

        if (move.isEnPassant) {
            newGrid[move.from.row][move.to.col] = null
        }

        val placed = if (move.promotion != null) {
            ChessPiece(move.promotion, moving.color)
        } else {
            moving
        }
        newGrid[move.to.row][move.to.col] = placed

        if (move.isCastleKingSide) {
            val row = move.from.row
            val rook = newGrid[row][7]
            newGrid[row][7] = null
            newGrid[row][5] = rook
        } else if (move.isCastleQueenSide) {
            val row = move.from.row
            val rook = newGrid[row][0]
            newGrid[row][0] = null
            newGrid[row][3] = rook
        }

        var wK = board.castling.whiteKingSide
        var wQ = board.castling.whiteQueenSide
        var bK = board.castling.blackKingSide
        var bQ = board.castling.blackQueenSide

        if (moving.type == PieceType.KING) {
            if (moving.color == PieceColor.WHITE) { wK = false; wQ = false }
            else { bK = false; bQ = false }
        }
        if (moving.type == PieceType.ROOK) {
            if (move.from == Square(7, 7)) wK = false
            if (move.from == Square(7, 0)) wQ = false
            if (move.from == Square(0, 7)) bK = false
            if (move.from == Square(0, 0)) bQ = false
        }
        if (move.to == Square(7, 7)) wK = false
        if (move.to == Square(7, 0)) wQ = false
        if (move.to == Square(0, 7)) bK = false
        if (move.to == Square(0, 0)) bQ = false

        val newEp = if (moving.type == PieceType.PAWN && abs(move.to.row - move.from.row) == 2) {
            Square((move.from.row + move.to.row) / 2, move.from.col)
        } else {
            null
        }

        val newHalf = if (moving.type == PieceType.PAWN || move.capturedPiece != null) 0 else board.halfMoveClock + 1
        val newFull = if (board.turn == PieceColor.BLACK) board.fullMoveNumber + 1 else board.fullMoveNumber

        return BoardState(
            grid = newGrid.map { it.toList() },
            turn = board.turn.opposite(),
            castling = CastlingRights(wK, wQ, bK, bQ),
            enPassantTarget = newEp,
            halfMoveClock = newHalf,
            fullMoveNumber = newFull
        )
    }

    fun formatMoveLabel(move: ChessMove, movingPiece: ChessPiece?, isCheck: Boolean, isCheckmate: Boolean): String {
        if (move.isCastleKingSide) return if (isCheckmate) "O-O#" else if (isCheck) "O-O+" else "O-O"
        if (move.isCastleQueenSide) return if (isCheckmate) "O-O-O#" else if (isCheck) "O-O-O+" else "O-O-O"
        val pieceLetter = when (movingPiece?.type) {
            PieceType.PAWN -> if (move.capturedPiece != null) "${'a' + move.from.col}" else ""
            PieceType.KNIGHT -> "C"
            PieceType.BISHOP -> "F"
            PieceType.ROOK -> "T"
            PieceType.QUEEN -> "D"
            PieceType.KING -> "R"
            null -> ""
        }
        val capture = if (move.capturedPiece != null) "x" else ""
        val promo = move.promotion?.let { "=${it.symbol}" } ?: ""
        val suffix = if (isCheckmate) "#" else if (isCheck) "+" else ""
        return "$pieceLetter$capture${move.to.toAlgebraic()}$promo$suffix"
    }
}
