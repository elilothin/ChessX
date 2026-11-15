package com.example.domain.chess

import kotlin.math.pow
import kotlin.math.roundToInt

object EloCalculator {
    /**
     * Modélisé sur le système Elo de Chess.com :
     * K = 32 (< 2100), K = 24 (2100..2399), K = 16 (>= 2400).
     * score = 1.0 (victoire), 0.5 (nulle), 0.0 (défaite).
     */
    fun calculateOnlineDelta(playerElo: Int, opponentElo: Int, score: Double): Int {
        val kFactor = when {
            playerElo < 2100 -> 32.0
            playerElo < 2400 -> 24.0
            else -> 16.0
        }
        val expected = 1.0 / (1.0 + 10.0.pow((opponentElo - playerElo) / 400.0))
        return (kFactor * (score - expected)).roundToInt()
    }
}
