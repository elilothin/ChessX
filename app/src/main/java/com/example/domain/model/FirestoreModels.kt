package com.example.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Stratège",
    val campaignElo: Long = 500L,
    val onlineElo: Long = 800L,
    val selectedUniverse: String = UniverseId.CLASSIQUE.id,
    val selectedBoard: String = UniverseId.CLASSIQUE.id,
    val unlockedUniverses: List<String> = listOf(UniverseId.CLASSIQUE.id),
    val westernWins: Long = 0L,
    val greceWins: Long = 0L,
    val japonWins: Long = 0L,
    val basketWins: Long = 0L,
    val futuristeWins: Long = 0L,
    val islandLevel: Long = 0L,
    val animationsEnabled: Boolean = true,
    val onlineWins: Long = 0L,
    val onlineLosses: Long = 0L,
    val onlineDraws: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun winsFor(universe: UniverseId): Int = when (universe) {
        UniverseId.WESTERN -> westernWins.toInt()
        UniverseId.GRECE -> greceWins.toInt()
        UniverseId.JAPON -> japonWins.toInt()
        UniverseId.BASKET -> basketWins.toInt()
        UniverseId.FUTURISTE -> futuristeWins.toInt()
        UniverseId.CLASSIQUE -> 5
    }

    fun isUnlocked(universe: UniverseId): Boolean =
        universe == UniverseId.CLASSIQUE || unlockedUniverses.contains(universe.id)

    fun isCampaignTargetAvailable(universe: UniverseId): Boolean = when (universe) {
        UniverseId.CLASSIQUE -> false
        UniverseId.WESTERN -> true
        UniverseId.GRECE -> westernWins >= 5L
        UniverseId.JAPON -> greceWins >= 5L
        UniverseId.BASKET -> japonWins >= 5L
        UniverseId.FUTURISTE -> basketWins >= 5L
    }

    fun currentCampaignTarget(): UniverseId = when {
        westernWins < 5L -> UniverseId.WESTERN
        greceWins < 5L -> UniverseId.GRECE
        japonWins < 5L -> UniverseId.JAPON
        basketWins < 5L -> UniverseId.BASKET
        else -> UniverseId.FUTURISTE
    }

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName.take(60).ifBlank { "Stratège" },
        "campaignElo" to campaignElo,
        "onlineElo" to onlineElo,
        "selectedUniverse" to selectedUniverse,
        "selectedBoard" to selectedBoard,
        "unlockedUniverses" to unlockedUniverses,
        "westernWins" to westernWins,
        "greceWins" to greceWins,
        "japonWins" to japonWins,
        "basketWins" to basketWins,
        "futuristeWins" to futuristeWins,
        "islandLevel" to islandLevel,
        "animationsEnabled" to animationsEnabled,
        "onlineWins" to onlineWins,
        "onlineLosses" to onlineLosses,
        "onlineDraws" to onlineDraws,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class OnlineMatch(
    @DocumentId val id: String = "",
    val whitePlayerId: String = "",
    val whiteDisplayName: String = "",
    val whiteUniverse: String = UniverseId.CLASSIQUE.id,
    val whiteElo: Long = 800L,
    val blackPlayerId: String? = null,
    val blackDisplayName: String? = null,
    val blackUniverse: String? = null,
    val blackElo: Long? = null,
    val boardUniverse: String = UniverseId.CLASSIQUE.id,
    val timeControlMinutes: Long = 5L,
    val whiteTimeMs: Long = 300_000L,
    val blackTimeMs: Long = 300_000L,
    val status: String = "waiting",
    val fen: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
    val turn: String = "w",
    val lastMoveUci: String? = null,
    val lastCapturePiece: String? = null,
    val lastCaptureVictim: String? = null,
    val lastCaptureUniverse: String? = null,
    val isCheck: Boolean = false,
    val winnerId: String? = null,
    val winnerUniverse: String? = null,
    val moveCount: Long = 0L,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MatchRecord(
    @DocumentId val id: String = "",
    val mode: String = "CAMPAIGN",
    val opponentName: String = "",
    val opponentUniverse: String = UniverseId.WESTERN.id,
    val playerUniverse: String = UniverseId.CLASSIQUE.id,
    val result: String = "WIN",
    val endReason: String = "CHECKMATE",
    val eloDelta: Long = 100L,
    val eloAfter: Long = 600L,
    val movesCount: Long = 0L,
    val timeControlMinutes: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "mode" to mode,
        "opponentName" to opponentName.take(60).ifBlank { "Adversaire" },
        "opponentUniverse" to opponentUniverse,
        "playerUniverse" to playerUniverse,
        "result" to result,
        "endReason" to endReason,
        "eloDelta" to eloDelta,
        "eloAfter" to eloAfter,
        "movesCount" to movesCount,
        "timeControlMinutes" to timeControlMinutes,
        "createdAt" to FieldValue.serverTimestamp()
    )
}
