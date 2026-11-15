package com.example.domain.chess

import com.example.domain.model.BoardState
import com.example.domain.model.ChessMove
import com.example.domain.model.PieceColor
import com.example.domain.model.PieceType
import kotlin.random.Random

object ChessAi {

    private val pawnTable = arrayOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(50, 50, 50, 50, 50, 50, 50, 50),
        intArrayOf(10, 10, 20, 30, 30, 20, 10, 10),
        intArrayOf(5, 5, 10, 27, 27, 10, 5, 5),
        intArrayOf(0, 0, 0, 24, 24, 0, 0, 0),
        intArrayOf(5, -5, -10, 0, 0, -10, -5, 5),
        intArrayOf(5, 10, 10, -22, -22, 10, 10, 5),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
    )

    private val knightTable = arrayOf(
        intArrayOf(-50, -40, -30, -30, -30, -30, -40, -50),
        intArrayOf(-40, -20, 0, 0, 0, 0, -20, -40),
        intArrayOf(-30, 0, 10, 15, 15, 10, 0, -30),
        intArrayOf(-30, 5, 15, 20, 20, 15, 5, -30),
        intArrayOf(-30, 0, 15, 20, 20, 15, 0, -30),
        intArrayOf(-30, 5, 10, 15, 15, 10, 5, -30),
        intArrayOf(-40, -20, 0, 5, 5, 0, -20, -40),
        intArrayOf(-50, -40, -30, -30, -30, -30, -40, -50)
    )

    private val bishopTable = arrayOf(
        intArrayOf(-20, -10, -10, -10, -10, -10, -10, -20),
        intArrayOf(-10, 0, 0, 0, 0, 0, 0, -10),
        intArrayOf(-10, 0, 5, 10, 10, 5, 0, -10),
        intArrayOf(-10, 5, 5, 10, 10, 5, 5, -10),
        intArrayOf(-10, 0, 10, 10, 10, 10, 0, -10),
        intArrayOf(-10, 10, 10, 10, 10, 10, 10, -10),
        intArrayOf(-10, 5, 0, 0, 0, 0, 5, -10),
        intArrayOf(-20, -10, -10, -10, -10, -10, -10, -20)
    )

    fun chooseMove(board: BoardState, depth: Int, randomness: Double): ChessMove? {
        val legal = ChessEngine.legalMoves(board)
        if (legal.isEmpty()) return null

        val ordered = orderMoves(legal)
        if (randomness > 0.0 && Random.nextDouble() < randomness) {
            val topSlice = ordered.take(minOf(4, ordered.size))
            return topSlice.random()
        }

        val maximizing = board.turn == PieceColor.WHITE
        var bestScore = if (maximizing) Int.MIN_VALUE else Int.MAX_VALUE
        val bestMoves = mutableListOf<ChessMove>()

        for (move in ordered) {
            val next = ChessEngine.applyMove(board, move)
            val score = minimax(next, (depth - 1).coerceAtLeast(0), Int.MIN_VALUE + 1, Int.MAX_VALUE - 1, !maximizing)
            if (maximizing) {
                if (score > bestScore) {
                    bestScore = score
                    bestMoves.clear()
                    bestMoves.add(move)
                } else if (score == bestScore) {
                    bestMoves.add(move)
                }
            } else {
                if (score < bestScore) {
                    bestScore = score
                    bestMoves.clear()
                    bestMoves.add(move)
                } else if (score == bestScore) {
                    bestMoves.add(move)
                }
            }
        }
        return bestMoves.randomOrNull() ?: ordered.first()
    }

    private fun minimax(board: BoardState, depth: Int, alphaInit: Int, betaInit: Int, maximizing: Boolean): Int {
        val moves = ChessEngine.legalMoves(board)
        if (moves.isEmpty()) {
            return if (ChessEngine.isInCheck(board, board.turn)) {
                if (board.turn == PieceColor.WHITE) -100_000 - depth else 100_000 + depth
            } else {
                0
            }
        }
        if (depth <= 0) {
            return evaluateBoard(board)
        }

        var alpha = alphaInit
        var beta = betaInit
        val ordered = orderMoves(moves)

        if (maximizing) {
            var maxEval = Int.MIN_VALUE + 1
            for (move in ordered) {
                val score = minimax(ChessEngine.applyMove(board, move), depth - 1, alpha, beta, false)
                if (score > maxEval) maxEval = score
                if (score > alpha) alpha = score
                if (beta <= alpha) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE - 1
            for (move in ordered) {
                val score = minimax(ChessEngine.applyMove(board, move), depth - 1, alpha, beta, true)
                if (score < minEval) minEval = score
                if (score < beta) beta = score
                if (beta <= alpha) break
            }
            return minEval
        }
    }

    private fun orderMoves(moves: List<ChessMove>): List<ChessMove> {
        return moves.sortedByDescending { move ->
            var score = 0
            if (move.capturedPiece != null) score += 1000 + move.capturedPiece.type.value
            if (move.promotion != null) score += 800 + move.promotion.value
            if (move.isCastleKingSide || move.isCastleQueenSide) score += 120
            score
        }
    }

    private fun evaluateBoard(board: BoardState): Int {
        var score = 0
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board.grid[r][c] ?: continue
                val tableRow = if (p.color == PieceColor.WHITE) r else 7 - r
                val posBonus = when (p.type) {
                    PieceType.PAWN -> pawnTable[tableRow][c]
                    PieceType.KNIGHT -> knightTable[tableRow][c]
                    PieceType.BISHOP -> bishopTable[tableRow][c]
                    PieceType.ROOK -> if (tableRow == 1) 15 else 0
                    PieceType.QUEEN -> if (r in 2..5 && c in 2..5) 10 else 0
                    PieceType.KING -> if (c in listOf(0, 1, 2, 6, 7)) 15 else -10
                }
                val total = p.type.value + posBonus
                if (p.color == PieceColor.WHITE) score += total else score -= total
            }
        }
        return score
    }
}
