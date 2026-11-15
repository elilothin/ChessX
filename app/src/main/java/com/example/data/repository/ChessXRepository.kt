package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.domain.chess.ChessEngine
import com.example.domain.chess.EloCalculator
import com.example.domain.model.MatchRecord
import com.example.domain.model.OnlineMatch
import com.example.domain.model.UniverseId
import com.example.domain.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChessXRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("L'utilisateur doit être connecté avec Google avant d'accéder à Firestore.")
    }

    suspend fun ensureUserProfile(defaultDisplayName: String): UserProfile {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                return snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: UserProfile(id = uid, userId = uid, displayName = defaultDisplayName.ifBlank { "Stratège" })
            }
            val initialProfile = UserProfile(
                userId = uid,
                displayName = defaultDisplayName.take(60).ifBlank { "Stratège" }
            )
            docRef.set(initialProfile.toCreateMap()).await()
            val created = docRef.get().await()
            return created.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: initialProfile.copy(id = uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeMatchHistory(userId: String): Flow<List<MatchRecord>> = flow {
        val path = "users/$userId/matchHistory"
        emitAll(
            db.collection("users").document(userId).collection("matchHistory")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(MatchRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun updateSelectedLoadout(universe: UniverseId, board: UniverseId) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update(
                mapOf(
                    "selectedUniverse" to universe.id,
                    "selectedBoard" to board.id,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update(
                mapOf(
                    "animationsEnabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    /**
     * Records a Campaign result:
     * - Victory: +100 Elo, increments wins (max 5) for the opponent universe.
     *   At 5 victories, unlocks that universe's pieces & board and levels up the player's island!
     * - Defeat / Draw: Elo and win counts remain untouched (per PDF page 4-5).
     */
    suspend fun recordCampaignResult(
        profile: UserProfile,
        opponentUniverse: UniverseId,
        isWin: Boolean,
        endReason: String,
        movesCount: Int
    ): UniverseId? {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        var newlyUnlocked: UniverseId? = null
        val newElo = if (isWin) (profile.campaignElo + 100L).coerceAtMost(3500L) else profile.campaignElo

        if (isWin) {
            val currentWins = profile.winsFor(opponentUniverse)
            val updatedWins = (currentWins + 1).coerceAtMost(5)
            val unlocked = profile.unlockedUniverses.toMutableList()
            var newIsland = profile.islandLevel
            if (updatedWins >= 5 && !unlocked.contains(opponentUniverse.id)) {
                unlocked.add(opponentUniverse.id)
                newlyUnlocked = opponentUniverse
                newIsland = maxOf(newIsland, opponentUniverse.campaignOrder.toLong())
            }

            val updates = mutableMapOf<String, Any>(
                "campaignElo" to newElo,
                "unlockedUniverses" to unlocked.distinct().take(6),
                "islandLevel" to newIsland,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            when (opponentUniverse) {
                UniverseId.WESTERN -> updates["westernWins"] = updatedWins.toLong()
                UniverseId.GRECE -> updates["greceWins"] = updatedWins.toLong()
                UniverseId.JAPON -> updates["japonWins"] = updatedWins.toLong()
                UniverseId.BASKET -> updates["basketWins"] = updatedWins.toLong()
                UniverseId.FUTURISTE -> updates["futuristeWins"] = updatedWins.toLong()
                UniverseId.CLASSIQUE -> Unit
            }
            try {
                userRef.update(updates).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, userRef.path)
                throw e
            }
        }

        val histId = "cmp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val histRef = userRef.collection("matchHistory").document(histId)
        val record = MatchRecord(
            mode = "CAMPAIGN",
            opponentName = opponentUniverse.aiBossName,
            opponentUniverse = opponentUniverse.id,
            playerUniverse = profile.selectedUniverse,
            result = if (isWin) "WIN" else if (endReason == "DRAW") "DRAW" else "LOSS",
            endReason = endReason,
            eloDelta = if (isWin) 100L else 0L,
            eloAfter = newElo,
            movesCount = movesCount.toLong().coerceAtLeast(0L),
            timeControlMinutes = 0L
        )
        try {
            histRef.set(record.toCreateMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, histRef.path)
            throw e
        }
        return newlyUnlocked
    }

    fun observeWaitingMatches(): Flow<List<OnlineMatch>> = flow {
        val path = "onlineMatches"
        emitAll(
            db.collection(path)
                .whereEqualTo("status", "waiting")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(OnlineMatch::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMatch(matchId: String): Flow<OnlineMatch?> = flow {
        val path = "onlineMatches/$matchId"
        emitAll(
            db.collection("onlineMatches").document(matchId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(OnlineMatch::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createOnlineMatch(
        profile: UserProfile,
        timeControlMinutes: Int
    ): String {
        val uid = requireUserId()
        require(timeControlMinutes in listOf(1, 5, 10)) { "Cadence invalide" }
        val matchId = "match_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("onlineMatches").document(matchId)
        val initialMs = timeControlMinutes * 60_000L
        val payload = mapOf(
            "whitePlayerId" to uid,
            "whiteDisplayName" to profile.displayName.take(60).ifBlank { "Joueur" },
            "whiteUniverse" to profile.selectedUniverse,
            "whiteElo" to profile.onlineElo,
            "boardUniverse" to profile.selectedBoard,
            "timeControlMinutes" to timeControlMinutes.toLong(),
            "whiteTimeMs" to initialMs,
            "blackTimeMs" to initialMs,
            "status" to "waiting",
            "fen" to ChessEngine.INITIAL_FEN,
            "turn" to "w",
            "isCheck" to false,
            "moveCount" to 0L,
            "memberUids" to listOf(uid),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return matchId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun joinOnlineMatch(match: OnlineMatch, profile: UserProfile) {
        val uid = requireUserId()
        require(match.id.isNotBlank()) { "Match ID requis" }
        val docRef = db.collection("onlineMatches").document(match.id)
        try {
            docRef.update(
                mapOf(
                    "blackPlayerId" to uid,
                    "blackDisplayName" to profile.displayName.take(60).ifBlank { "Adversaire" },
                    "blackUniverse" to profile.selectedUniverse,
                    "blackElo" to profile.onlineElo,
                    "status" to "active",
                    "memberUids" to listOf(match.whitePlayerId, uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun cancelWaitingMatch(matchId: String) {
        requireUserId()
        val docRef = db.collection("onlineMatches").document(matchId)
        try {
            docRef.update(
                mapOf(
                    "status" to "cancelled",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun submitOnlineMove(
        matchId: String,
        newFen: String,
        nextTurn: String,
        whiteTimeMs: Long,
        blackTimeMs: Long,
        lastMoveUci: String,
        lastCapturePiece: String?,
        lastCaptureVictim: String?,
        lastCaptureUniverse: String?,
        isCheck: Boolean,
        status: String,
        winnerId: String?,
        winnerUniverse: String?,
        moveCount: Long
    ) {
        requireUserId()
        val docRef = db.collection("onlineMatches").document(matchId)
        val payload = mutableMapOf<String, Any>(
            "status" to status,
            "fen" to newFen,
            "turn" to nextTurn,
            "whiteTimeMs" to whiteTimeMs.coerceIn(0L, 600_000L),
            "blackTimeMs" to blackTimeMs.coerceIn(0L, 600_000L),
            "lastMoveUci" to lastMoveUci,
            "isCheck" to isCheck,
            "moveCount" to moveCount,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (lastCapturePiece != null) payload["lastCapturePiece"] = lastCapturePiece
        if (lastCaptureVictim != null) payload["lastCaptureVictim"] = lastCaptureVictim
        if (lastCaptureUniverse != null) payload["lastCaptureUniverse"] = lastCaptureUniverse
        if (winnerId != null) payload["winnerId"] = winnerId
        if (winnerUniverse != null) payload["winnerUniverse"] = winnerUniverse

        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun finishOnlineByResignOrTimeout(
        matchId: String,
        status: String,
        winnerId: String,
        winnerUniverse: String
    ) {
        requireUserId()
        val docRef = db.collection("onlineMatches").document(matchId)
        try {
            docRef.update(
                mapOf(
                    "status" to status,
                    "winnerId" to winnerId,
                    "winnerUniverse" to winnerUniverse,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun applyOnlineEloResult(
        profile: UserProfile,
        opponentName: String,
        opponentUniverse: String,
        opponentElo: Int,
        score: Double, // 1.0 win, 0.5 draw, 0.0 loss
        endReason: String,
        movesCount: Int,
        timeControlMinutes: Int
    ): Pair<Int, Int> {
        val uid = requireUserId()
        val delta = EloCalculator.calculateOnlineDelta(profile.onlineElo.toInt(), opponentElo, score)
        val newElo = (profile.onlineElo + delta).coerceIn(100L, 4000L)
        val userRef = db.collection("users").document(uid)
        val updates = mutableMapOf<String, Any>(
            "onlineElo" to newElo,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        when (score) {
            1.0 -> updates["onlineWins"] = profile.onlineWins + 1L
            0.0 -> updates["onlineLosses"] = profile.onlineLosses + 1L
            else -> updates["onlineDraws"] = profile.onlineDraws + 1L
        }
        try {
            userRef.update(updates).await()
            val histId = "onl_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val histRef = userRef.collection("matchHistory").document(histId)
            val record = MatchRecord(
                mode = "ONLINE",
                opponentName = opponentName,
                opponentUniverse = opponentUniverse,
                playerUniverse = profile.selectedUniverse,
                result = when (score) {
                    1.0 -> "WIN"
                    0.0 -> "LOSS"
                    else -> "DRAW"
                },
                endReason = endReason,
                eloDelta = delta.toLong(),
                eloAfter = newElo,
                movesCount = movesCount.toLong().coerceAtLeast(0L),
                timeControlMinutes = timeControlMinutes.toLong()
            )
            histRef.set(record.toCreateMap()).await()
            return delta to newElo.toInt()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            throw e
        }
    }
}
