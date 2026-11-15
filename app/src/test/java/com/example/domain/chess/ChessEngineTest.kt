package com.example.domain.chess

import com.example.domain.model.GameOutcome
import com.example.domain.model.PieceColor
import com.example.domain.model.PieceType
import com.example.domain.model.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessEngineTest {

    @Test
    fun initialPosition_has20LegalMoves() {
        val board = ChessEngine.initialBoard()
        val moves = ChessEngine.legalMoves(board)
        assertEquals(20, moves.size)
        assertEquals(GameOutcome.ONGOING, ChessEngine.evaluateOutcome(board))
    }

    @Test
    fun foolsMate_detectsCheckmateBlackWins() {
        var board = ChessEngine.initialBoard()
        val sequence = listOf("f2f3", "e7e5", "g2g4", "d8h4")
        for (uci in sequence) {
            val from = Square.fromAlgebraic(uci.substring(0, 2))!!
            val to = Square.fromAlgebraic(uci.substring(2, 4))!!
            val move = ChessEngine.legalMovesFrom(board, from).first { it.to == to }
            board = ChessEngine.applyMove(board, move)
        }
        assertTrue(ChessEngine.isInCheck(board, PieceColor.WHITE))
        assertEquals(GameOutcome.CHECKMATE_BLACK_WINS, ChessEngine.evaluateOutcome(board))
    }

    @Test
    fun castling_and_enPassant_areSupported() {
        val castleFen = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"
        val board = ChessEngine.fromFen(castleFen)
        val kingMoves = ChessEngine.legalMovesFrom(board, Square.fromAlgebraic("e1")!!)
        assertTrue(kingMoves.any { it.isCastleKingSide && it.to == Square.fromAlgebraic("g1") })
        assertTrue(kingMoves.any { it.isCastleQueenSide && it.to == Square.fromAlgebraic("c1") })

        val epFen = "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"
        val epBoard = ChessEngine.fromFen(epFen)
        val epMoves = ChessEngine.legalMovesFrom(epBoard, Square.fromAlgebraic("e5")!!)
        assertTrue(epMoves.any { it.isEnPassant && it.to == Square.fromAlgebraic("d6") })
    }

    @Test
    fun pawnPromotion_generatesFourPromotionChoices() {
        val promoFen = "4k3/P7/8/8/8/8/8/4K3 w - - 0 1"
        val board = ChessEngine.fromFen(promoFen)
        val moves = ChessEngine.legalMovesFrom(board, Square.fromAlgebraic("a7")!!)
            .filter { it.to == Square.fromAlgebraic("a8") }
        assertEquals(4, moves.size)
        assertEquals(
            setOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT),
            moves.mapNotNull { it.promotion }.toSet()
        )
    }

    @Test
    fun eloCalculator_chessComFormula_worksCorrectly() {
        val gain = EloCalculator.calculateOnlineDelta(800, 800, 1.0)
        assertEquals(16, gain)
        val loss = EloCalculator.calculateOnlineDelta(800, 800, 0.0)
        assertEquals(-16, loss)
    }
}
