package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ChessXRepository
import com.example.domain.chess.ChessAi
import com.example.domain.chess.ChessEngine
import com.example.domain.model.BoardState
import com.example.domain.model.CaptureEvent
import com.example.domain.model.ChessMove
import com.example.domain.model.EndgameEvent
import com.example.domain.model.EndgameType
import com.example.domain.model.GameOutcome
import com.example.domain.model.MatchRecord
import com.example.domain.model.OnlineMatch
import com.example.domain.model.PieceColor
import com.example.domain.model.PieceType
import com.example.domain.model.Square
import com.example.domain.model.UniverseId
import com.example.domain.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class ActiveGameState(
    val isActive: Boolean = false,
    val isOnline: Boolean = false,
    val onlineMatchId: String? = null,
    val isWaitingForOpponent: Boolean = false,
    val board: BoardState = ChessEngine.initialBoard(),
    val playerColor: PieceColor = PieceColor.WHITE,
    val whiteUniverse: UniverseId = UniverseId.CLASSIQUE,
    val blackUniverse: UniverseId = UniverseId.WESTERN,
    val boardUniverse: UniverseId = UniverseId.WESTERN,
    val campaignTargetUniverse: UniverseId = UniverseId.WESTERN,
    val whiteName: String = "Vous",
    val blackName: String = "Adversaire",
    val whiteElo: Int = 500,
    val blackElo: Int = 500,
    val timeControlMinutes: Int = 0, // 0 = untimed campaign; 1, 5, 10 = online blitz
    val whiteTimeMs: Long = 0L,
    val blackTimeMs: Long = 0L,
    val selectedSquare: Square? = null,
    val legalMovesForSelected: List<ChessMove> = emptyList(),
    val pendingPromotionMoves: List<ChessMove> = emptyList(),
    val lastMoveSquares: Pair<Square, Square>? = null,
    val moveHistory: List<String> = emptyList(),
    val isInCheck: Boolean = false,
    val isAiThinking: Boolean = false,
    val activeCapture: CaptureEvent? = null,
    val activeEndgame: EndgameEvent? = null,
    val showCheckBanner: Boolean = false,
    val showPromotionFlash: Boolean = false,
    val animationsCutoffByTwoMinRule: Boolean = false
)

class ChessXViewModel(
    private val repository: ChessXRepository,
    private val currentUserId: String,
    private val initialDisplayName: String
) : ViewModel() {

    val profileState: StateFlow<UiState<UserProfile>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile>> { profile ->
            if (profile != null) UiState.Success(profile) else UiState.Loading
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing profile", rootError)
            emit(UiState.Error(rootError.message ?: "Impossible de charger le profil."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val matchHistory: StateFlow<List<MatchRecord>> = repository.observeMatchHistory(currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing match history", error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val waitingMatches: StateFlow<List<OnlineMatch>> = repository.observeWaitingMatches()
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing waiting matches", error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _gameState = MutableStateFlow(ActiveGameState())
    val gameState: StateFlow<ActiveGameState> = _gameState.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var onlineMatchJob: Job? = null
    private var clockTickerJob: Job? = null
    private var captureDismissJob: Job? = null
    private var onlineEloSettledForMatchId: String? = null
    private var lastProcessedMoveCount: Long = -1L

    init {
        viewModelScope.launch {
            try {
                repository.ensureUserProfile(initialDisplayName)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _statusMessage.value = e.localizedMessage ?: "Erreur d'initialisation du profil."
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun selectLoadout(universe: UniverseId, board: UniverseId) {
        viewModelScope.launch {
            try {
                repository.updateSelectedLoadout(universe, board)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _statusMessage.value = "Impossible d'équiper cet univers."
            }
        }
    }

    fun toggleAnimations(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.setAnimationsEnabled(enabled)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _statusMessage.value = "Erreur de sauvegarde du paramètre."
            }
        }
    }

    fun startCampaignMatch(opponentUniverse: UniverseId) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        stopOnlineListeners()
        val playerUniverse = UniverseId.fromId(profile.selectedUniverse)

        _gameState.value = ActiveGameState(
            isActive = true,
            isOnline = false,
            onlineMatchId = null,
            isWaitingForOpponent = false,
            board = ChessEngine.initialBoard(),
            playerColor = PieceColor.WHITE,
            whiteUniverse = playerUniverse,
            blackUniverse = opponentUniverse,
            boardUniverse = opponentUniverse, // PDF: "Chaque étape intègre le plateau... spécifiques à l'univers affronté"
            campaignTargetUniverse = opponentUniverse,
            whiteName = profile.displayName,
            blackName = opponentUniverse.aiBossName,
            whiteElo = profile.campaignElo.toInt(),
            blackElo = 450 + opponentUniverse.campaignOrder * 450,
            timeControlMinutes = 0
        )
    }

    fun createOnlineMatch(timeControlMinutes: Int) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            try {
                val matchId = repository.createOnlineMatch(profile, timeControlMinutes)
                observeAndAttachOnlineMatch(matchId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _statusMessage.value = "Impossible de créer la partie en ligne."
            }
        }
    }

    fun joinOnlineMatch(match: OnlineMatch) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        if (match.whitePlayerId == currentUserId) {
            observeAndAttachOnlineMatch(match.id)
            return
        }
        viewModelScope.launch {
            try {
                repository.joinOnlineMatch(match, profile)
                observeAndAttachOnlineMatch(match.id)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _statusMessage.value = "Cette partie n'est plus disponible."
            }
        }
    }

    fun cancelOnlineWaiting() {
        val matchId = _gameState.value.onlineMatchId ?: return
        viewModelScope.launch {
            try {
                repository.cancelWaitingMatch(matchId)
                closeGame()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                closeGame()
            }
        }
    }

    private fun observeAndAttachOnlineMatch(matchId: String) {
        stopOnlineListeners()
        onlineEloSettledForMatchId = null
        lastProcessedMoveCount = -1L

        onlineMatchJob = viewModelScope.launch {
            repository.observeMatch(matchId).collect { match ->
                if (match == null) return@collect
                val isWhite = match.whitePlayerId == currentUserId
                val playerColor = if (isWhite) PieceColor.WHITE else PieceColor.BLACK
                val board = ChessEngine.fromFen(match.fen)
                val wUniv = UniverseId.fromId(match.whiteUniverse)
                val bUniv = UniverseId.fromId(match.blackUniverse ?: UniverseId.CLASSIQUE.id)
                val boardUniv = UniverseId.fromId(match.boardUniverse)
                val cutoff = match.timeControlMinutes <= 1L ||
                    match.whiteTimeMs <= TWO_MINUTES_MS ||
                    match.blackTimeMs <= TWO_MINUTES_MS

                val lastSquares = match.lastMoveUci?.takeIf { it.length >= 4 }?.let { uci ->
                    val from = Square.fromAlgebraic(uci.substring(0, 2))
                    val to = Square.fromAlgebraic(uci.substring(2, 4))
                    if (from != null && to != null) from to to else null
                }

                val isNewMove = match.moveCount > lastProcessedMoveCount && lastProcessedMoveCount >= 0L
                lastProcessedMoveCount = match.moveCount

                val prev = _gameState.value
                _gameState.value = prev.copy(
                    isActive = true,
                    isOnline = true,
                    onlineMatchId = match.id,
                    isWaitingForOpponent = match.status == "waiting",
                    board = board,
                    playerColor = playerColor,
                    whiteUniverse = wUniv,
                    blackUniverse = bUniv,
                    boardUniverse = boardUniv,
                    whiteName = match.whiteDisplayName,
                    blackName = match.blackDisplayName ?: "En attente d'un adversaire...",
                    whiteElo = match.whiteElo.toInt(),
                    blackElo = (match.blackElo ?: 800L).toInt(),
                    timeControlMinutes = match.timeControlMinutes.toInt(),
                    whiteTimeMs = match.whiteTimeMs,
                    blackTimeMs = match.blackTimeMs,
                    selectedSquare = if (board.turn != playerColor) null else prev.selectedSquare,
                    legalMovesForSelected = if (board.turn != playerColor) emptyList() else prev.legalMovesForSelected,
                    lastMoveSquares = lastSquares,
                    isInCheck = match.isCheck,
                    animationsCutoffByTwoMinRule = cutoff
                )

                // Sync remote capture animation for both players in real time
                if (isNewMove && match.lastCapturePiece != null && match.lastCaptureVictim != null) {
                    val attackerType = PieceType.fromChar(match.lastCapturePiece.first())
                    val victimType = PieceType.fromChar(match.lastCaptureVictim.first())
                    val captureUniv = UniverseId.fromId(match.lastCaptureUniverse)
                    val attackerColor = board.turn.opposite()
                    if (attackerType != null && victimType != null) {
                        triggerCaptureAnimationIfAllowed(
                            attackerUniverse = captureUniv,
                            attackerType = attackerType,
                            attackerColor = attackerColor,
                            victimType = victimType,
                            victimColor = attackerColor.opposite(),
                            isOnline = true,
                            timeControlMinutes = match.timeControlMinutes.toInt(),
                            whiteTimeMs = match.whiteTimeMs,
                            blackTimeMs = match.blackTimeMs
                        )
                    }
                }
                if (isNewMove && match.isCheck && match.status == "active") {
                    triggerCheckBanner()
                }

                if (match.status == "active") {
                    ensureClockTickerRunning(match)
                } else {
                    clockTickerJob?.cancel()
                }

                if (match.status in listOf("checkmate", "resigned", "timeout", "draw") &&
                    onlineEloSettledForMatchId != match.id
                ) {
                    onlineEloSettledForMatchId = match.id
                    settleOnlineEndgame(match, playerColor)
                }
            }
        }
    }

    private fun ensureClockTickerRunning(match: OnlineMatch) {
        if (clockTickerJob?.isActive == true) return
        clockTickerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val state = _gameState.value
                if (!state.isOnline || state.isWaitingForOpponent || state.activeEndgame != null) break
                val isWhiteTurn = state.board.turn == PieceColor.WHITE
                val newWhite = if (isWhiteTurn) (state.whiteTimeMs - 1000L).coerceAtLeast(0L) else state.whiteTimeMs
                val newBlack = if (!isWhiteTurn) (state.blackTimeMs - 1000L).coerceAtLeast(0L) else state.blackTimeMs
                val cutoff = state.timeControlMinutes <= 1 ||
                    newWhite <= TWO_MINUTES_MS ||
                    newBlack <= TWO_MINUTES_MS

                _gameState.update {
                    it.copy(
                        whiteTimeMs = newWhite,
                        blackTimeMs = newBlack,
                        animationsCutoffByTwoMinRule = cutoff
                    )
                }

                // If our own clock hits 0, report timeout cleanly
                if ((isWhiteTurn && state.playerColor == PieceColor.WHITE && newWhite <= 0L) ||
                    (!isWhiteTurn && state.playerColor == PieceColor.BLACK && newBlack <= 0L)
                ) {
                    val winnerId = if (state.playerColor == PieceColor.WHITE) match.blackPlayerId else match.whitePlayerId
                    val winnerUniv = if (state.playerColor == PieceColor.WHITE) state.blackUniverse.id else state.whiteUniverse.id
                    if (winnerId != null) {
                        repository.finishOnlineByResignOrTimeout(match.id, "timeout", winnerId, winnerUniv)
                    }
                    break
                }
            }
        }
    }

    private fun settleOnlineEndgame(match: OnlineMatch, playerColor: PieceColor) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val isDraw = match.status == "draw"
            val didWin = !isDraw && match.winnerId == currentUserId
            val score = when {
                isDraw -> 0.5
                didWin -> 1.0
                else -> 0.0
            }
            val opponentName = if (playerColor == PieceColor.WHITE) match.blackDisplayName ?: "Adversaire" else match.whiteDisplayName
            val opponentUniv = if (playerColor == PieceColor.WHITE) match.blackUniverse ?: "CLASSIQUE" else match.whiteUniverse
            val opponentElo = if (playerColor == PieceColor.WHITE) (match.blackElo ?: 800L).toInt() else match.whiteElo.toInt()

            val endReason = when (match.status) {
                "checkmate" -> "CHECKMATE"
                "resigned" -> "RESIGN"
                "timeout" -> "TIMEOUT"
                else -> "DRAW"
            }

            val (delta, newElo) = try {
                repository.applyOnlineEloResult(
                    profile = profile,
                    opponentName = opponentName,
                    opponentUniverse = opponentUniv,
                    opponentElo = opponentElo,
                    score = score,
                    endReason = endReason,
                    movesCount = match.moveCount.toInt(),
                    timeControlMinutes = match.timeControlMinutes.toInt()
                )
            } catch (e: Exception) {
                0 to profile.onlineElo.toInt()
            }

            val winnerColor = when {
                isDraw -> null
                match.winnerId == match.whitePlayerId -> PieceColor.WHITE
                else -> PieceColor.BLACK
            }
            val winnerUniverse = UniverseId.fromId(match.winnerUniverse)
            val endgameType = when (match.status) {
                "checkmate" -> EndgameType.CHECKMATE
                "resigned" -> EndgameType.RESIGN
                "timeout" -> EndgameType.TIMEOUT
                else -> EndgameType.STALEMATE_OR_DRAW
            }
            val title = when (endgameType) {
                EndgameType.CHECKMATE -> winnerUniverse.checkmateTitle
                EndgameType.RESIGN -> winnerUniverse.resignTitle
                EndgameType.TIMEOUT -> "Temps écoulé !"
                EndgameType.STALEMATE_OR_DRAW -> "Partie Nulle"
            }
            val description = when (endgameType) {
                EndgameType.CHECKMATE -> winnerUniverse.checkmateDescription
                EndgameType.RESIGN -> winnerUniverse.resignDescription
                EndgameType.TIMEOUT -> "L'horloge de blitz a atteint 00:00."
                EndgameType.STALEMATE_OR_DRAW -> "Égalité parfaite sur l'échiquier."
            }

            _gameState.update {
                it.copy(
                    activeEndgame = EndgameEvent(
                        type = endgameType,
                        winnerColor = winnerColor,
                        winnerUniverse = winnerUniverse,
                        title = title,
                        description = description,
                        eloDelta = delta,
                        newElo = newElo
                    )
                )
            }
        }
    }

    fun onSquareTapped(square: Square) {
        val state = _gameState.value
        if (!state.isActive || state.isWaitingForOpponent || state.isAiThinking || state.activeEndgame != null) return
        if (state.board.turn != state.playerColor) return

        // Check if tapped square is a legal destination for the selected piece
        val matchingMoves = state.legalMovesForSelected.filter { it.to == square }
        if (matchingMoves.isNotEmpty()) {
            if (matchingMoves.size > 1 && matchingMoves.any { it.promotion != null }) {
                _gameState.update { it.copy(pendingPromotionMoves = matchingMoves) }
            } else {
                executePlayerMove(matchingMoves.first())
            }
            return
        }

        // Otherwise select own piece
        val piece = state.board.pieceAt(square)
        if (piece != null && piece.color == state.playerColor) {
            val moves = ChessEngine.legalMovesFrom(state.board, square)
            _gameState.update {
                it.copy(
                    selectedSquare = square,
                    legalMovesForSelected = moves
                )
            }
        } else {
            _gameState.update {
                it.copy(selectedSquare = null, legalMovesForSelected = emptyList())
            }
        }
    }

    fun selectPromotion(pieceType: PieceType) {
        val move = _gameState.value.pendingPromotionMoves.firstOrNull { it.promotion == pieceType } ?: return
        _gameState.update { it.copy(pendingPromotionMoves = emptyList()) }
        executePlayerMove(move)
    }

    fun cancelPromotion() {
        _gameState.update { it.copy(pendingPromotionMoves = emptyList()) }
    }

    private fun executePlayerMove(move: ChessMove) {
        val state = _gameState.value
        val movingPiece = state.board.pieceAt(move.from) ?: return
        val nextBoard = ChessEngine.applyMove(state.board, move)
        val outcome = ChessEngine.evaluateOutcome(nextBoard)
        val isCheck = ChessEngine.isInCheck(nextBoard, nextBoard.turn)
        val isCheckmate = outcome == GameOutcome.CHECKMATE_WHITE_WINS || outcome == GameOutcome.CHECKMATE_BLACK_WINS
        val notation = ChessEngine.formatMoveLabel(move, movingPiece, isCheck, isCheckmate)
        val attackerUniverse = if (movingPiece.color == PieceColor.WHITE) state.whiteUniverse else state.blackUniverse

        if (move.promotion != null) {
            triggerPromotionFlash()
        }

        if (state.isOnline) {
            val matchId = state.onlineMatchId ?: return
            val statusStr = when (outcome) {
                GameOutcome.ONGOING -> "active"
                GameOutcome.CHECKMATE_WHITE_WINS, GameOutcome.CHECKMATE_BLACK_WINS -> "checkmate"
                GameOutcome.STALEMATE, GameOutcome.DRAW_INSUFFICIENT_MATERIAL, GameOutcome.DRAW_FIFTY_MOVE -> "draw"
            }
            val winnerId = if (isCheckmate) currentUserId else if (statusStr == "draw") "draw" else null
            val winnerUniv = if (isCheckmate) attackerUniverse.id else null

            _gameState.update {
                it.copy(
                    board = nextBoard,
                    selectedSquare = null,
                    legalMovesForSelected = emptyList(),
                    lastMoveSquares = move.from to move.to,
                    moveHistory = it.moveHistory + notation,
                    isInCheck = isCheck
                )
            }

            if (move.capturedPiece != null) {
                triggerCaptureAnimationIfAllowed(
                    attackerUniverse = attackerUniverse,
                    attackerType = movingPiece.type,
                    attackerColor = movingPiece.color,
                    victimType = move.capturedPiece.type,
                    victimColor = move.capturedPiece.color,
                    isOnline = true,
                    timeControlMinutes = state.timeControlMinutes,
                    whiteTimeMs = state.whiteTimeMs,
                    blackTimeMs = state.blackTimeMs
                )
            } else if (isCheck && !isCheckmate) {
                triggerCheckBanner()
            }

            viewModelScope.launch {
                try {
                    repository.submitOnlineMove(
                        matchId = matchId,
                        newFen = ChessEngine.toFen(nextBoard),
                        nextTurn = nextBoard.turn.fenChar().toString(),
                        whiteTimeMs = state.whiteTimeMs,
                        blackTimeMs = state.blackTimeMs,
                        lastMoveUci = move.toUci(),
                        lastCapturePiece = move.capturedPiece?.let { movingPiece.type.symbol.toString() },
                        lastCaptureVictim = move.capturedPiece?.type?.symbol?.toString(),
                        lastCaptureUniverse = if (move.capturedPiece != null) attackerUniverse.id else null,
                        isCheck = isCheck,
                        status = statusStr,
                        winnerId = winnerId,
                        winnerUniverse = winnerUniv,
                        moveCount = (state.moveHistory.size + 1).toLong()
                    )
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    _statusMessage.value = "Erreur de synchronisation du coup."
                }
            }
            return
        }

        // Offline Campaign mode
        _gameState.update {
            it.copy(
                board = nextBoard,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                lastMoveSquares = move.from to move.to,
                moveHistory = it.moveHistory + notation,
                isInCheck = isCheck
            )
        }

        if (move.capturedPiece != null) {
            triggerCaptureAnimationIfAllowed(
                attackerUniverse = attackerUniverse,
                attackerType = movingPiece.type,
                attackerColor = movingPiece.color,
                victimType = move.capturedPiece.type,
                victimColor = move.capturedPiece.color,
                isOnline = false,
                timeControlMinutes = 0,
                whiteTimeMs = 0L,
                blackTimeMs = 0L
            )
        } else if (isCheck && !isCheckmate) {
            triggerCheckBanner()
        }

        if (outcome != GameOutcome.ONGOING) {
            handleCampaignEndgame(outcome)
        } else {
            runCampaignAiTurn()
        }
    }

    private fun runCampaignAiTurn() {
        val state = _gameState.value
        val targetUniverse = state.campaignTargetUniverse
        _gameState.update { it.copy(isAiThinking = true) }

        viewModelScope.launch {
            // Let capture animation breathe slightly if active
            if (_gameState.value.activeCapture != null) delay(1200L)
            else delay(350L)

            val currentBoard = _gameState.value.board
            val aiMove = withContext(Dispatchers.Default) {
                ChessAi.chooseMove(currentBoard, targetUniverse.aiDepth, targetUniverse.aiRandomness)
            }

            if (aiMove == null) {
                _gameState.update { it.copy(isAiThinking = false) }
                return@launch
            }

            val aiPiece = currentBoard.pieceAt(aiMove.from)
            val nextBoard = ChessEngine.applyMove(currentBoard, aiMove)
            val outcome = ChessEngine.evaluateOutcome(nextBoard)
            val isCheck = ChessEngine.isInCheck(nextBoard, nextBoard.turn)
            val isCheckmate = outcome == GameOutcome.CHECKMATE_WHITE_WINS || outcome == GameOutcome.CHECKMATE_BLACK_WINS
            val notation = ChessEngine.formatMoveLabel(aiMove, aiPiece, isCheck, isCheckmate)

            _gameState.update {
                it.copy(
                    board = nextBoard,
                    lastMoveSquares = aiMove.from to aiMove.to,
                    moveHistory = it.moveHistory + notation,
                    isInCheck = isCheck,
                    isAiThinking = false
                )
            }

            if (aiMove.capturedPiece != null && aiPiece != null) {
                triggerCaptureAnimationIfAllowed(
                    attackerUniverse = state.blackUniverse,
                    attackerType = aiPiece.type,
                    attackerColor = aiPiece.color,
                    victimType = aiMove.capturedPiece.type,
                    victimColor = aiMove.capturedPiece.color,
                    isOnline = false,
                    timeControlMinutes = 0,
                    whiteTimeMs = 0L,
                    blackTimeMs = 0L
                )
            } else if (isCheck && !isCheckmate) {
                triggerCheckBanner()
            }

            if (outcome != GameOutcome.ONGOING) {
                handleCampaignEndgame(outcome)
            }
        }
    }

    fun resignCurrentMatch() {
        val state = _gameState.value
        if (!state.isActive || state.activeEndgame != null) return

        if (state.isOnline) {
            val matchId = state.onlineMatchId ?: return
            val winnerUniverse = if (state.playerColor == PieceColor.WHITE) state.blackUniverse.id else state.whiteUniverse.id
            resignOnlineMatchInternal(matchId, winnerUniverse)
            return
        }

        // Campaign resign (défaite -> Elo ne baisse pas)
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        val winnerUniv = state.blackUniverse
        viewModelScope.launch {
            repository.recordCampaignResult(
                profile = profile,
                opponentUniverse = state.campaignTargetUniverse,
                isWin = false,
                endReason = "RESIGN",
                movesCount = state.moveHistory.size
            )
            _gameState.update {
                it.copy(
                    activeEndgame = EndgameEvent(
                        type = EndgameType.RESIGN,
                        winnerColor = PieceColor.BLACK,
                        winnerUniverse = winnerUniv,
                        title = winnerUniv.resignTitle,
                        description = winnerUniv.resignDescription,
                        eloDelta = 0,
                        newElo = profile.campaignElo.toInt()
                    )
                )
            }
        }
    }

    private fun resignOnlineMatchInternal(matchId: String, winnerUniverse: String) {
        viewModelScope.launch {
            try {
                // Observe once to read exact opponent UID
                var targetWinnerId: String? = null
                repository.observeMatch(matchId).collect { m ->
                    if (m != null && targetWinnerId == null) {
                        targetWinnerId = if (m.whitePlayerId == currentUserId) m.blackPlayerId else m.whitePlayerId
                        val wId = targetWinnerId
                        if (wId != null) {
                            repository.finishOnlineByResignOrTimeout(matchId, "resigned", wId, winnerUniverse)
                        }
                        throw CancellationException("Done")
                    }
                }
            } catch (_: CancellationException) {
            } catch (e: Exception) {
                _statusMessage.value = "Impossible d'abandonner la partie."
            }
        }
    }

    private fun handleCampaignEndgame(outcome: GameOutcome) {
        val state = _gameState.value
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        val isWin = outcome == GameOutcome.CHECKMATE_WHITE_WINS
        val isDraw = outcome == GameOutcome.STALEMATE ||
            outcome == GameOutcome.DRAW_INSUFFICIENT_MATERIAL ||
            outcome == GameOutcome.DRAW_FIFTY_MOVE

        val winnerColor = when {
            isWin -> PieceColor.WHITE
            isDraw -> null
            else -> PieceColor.BLACK
        }
        val winnerUniverse = when (winnerColor) {
            PieceColor.WHITE -> state.whiteUniverse
            PieceColor.BLACK -> state.blackUniverse
            null -> state.boardUniverse
        }
        val endReason = when {
            isWin || outcome == GameOutcome.CHECKMATE_BLACK_WINS -> "CHECKMATE"
            else -> "DRAW"
        }

        viewModelScope.launch {
            val unlocked = try {
                repository.recordCampaignResult(
                    profile = profile,
                    opponentUniverse = state.campaignTargetUniverse,
                    isWin = isWin,
                    endReason = endReason,
                    movesCount = state.moveHistory.size
                )
            } catch (e: Exception) {
                null
            }

            val delta = if (isWin) 100 else 0
            val newElo = profile.campaignElo.toInt() + delta
            _gameState.update {
                it.copy(
                    activeEndgame = EndgameEvent(
                        type = if (isDraw) EndgameType.STALEMATE_OR_DRAW else EndgameType.CHECKMATE,
                        winnerColor = winnerColor,
                        winnerUniverse = winnerUniverse,
                        title = if (isDraw) "Pat / Partie Nulle" else winnerUniverse.checkmateTitle,
                        description = if (isDraw) "Aucun coup légal disponible sans échec." else winnerUniverse.checkmateDescription,
                        eloDelta = delta,
                        newElo = newElo,
                        unlockedUniverse = unlocked
                    )
                )
            }
        }
    }

    fun dismissCaptureAnimation() {
        captureDismissJob?.cancel()
        _gameState.update { it.copy(activeCapture = null) }
    }

    private fun triggerCaptureAnimationIfAllowed(
        attackerUniverse: UniverseId,
        attackerType: PieceType,
        attackerColor: PieceColor,
        victimType: PieceType,
        victimColor: PieceColor,
        isOnline: Boolean,
        timeControlMinutes: Int,
        whiteTimeMs: Long,
        blackTimeMs: Long
    ) {
        val profile = (profileState.value as? UiState.Success)?.data
        val enabled = profile?.animationsEnabled ?: true
        if (!enabled) return
        if (isOnline && (timeControlMinutes <= 1 || whiteTimeMs <= TWO_MINUTES_MS || blackTimeMs <= TWO_MINUTES_MS)) {
            return
        }

        val event = CaptureEvent(
            id = System.currentTimeMillis(),
            attackerUniverse = attackerUniverse,
            attackerType = attackerType,
            attackerColor = attackerColor,
            victimType = victimType,
            victimColor = victimColor
        )
        _gameState.update { it.copy(activeCapture = event) }
        captureDismissJob?.cancel()
        captureDismissJob = viewModelScope.launch {
            delay(3000L) // ~3 secondes selon le PDF page 10
            _gameState.update { current ->
                if (current.activeCapture?.id == event.id) current.copy(activeCapture = null) else current
            }
        }
    }

    private fun triggerCheckBanner() {
        viewModelScope.launch {
            _gameState.update { it.copy(showCheckBanner = true) }
            delay(1400L)
            _gameState.update { it.copy(showCheckBanner = false) }
        }
    }

    private fun triggerPromotionFlash() {
        viewModelScope.launch {
            _gameState.update { it.copy(showPromotionFlash = true) }
            delay(1000L)
            _gameState.update { it.copy(showPromotionFlash = false) }
        }
    }

    fun closeGame() {
        stopOnlineListeners()
        _gameState.value = ActiveGameState(isActive = false)
    }

    private fun stopOnlineListeners() {
        onlineMatchJob?.cancel()
        onlineMatchJob = null
        clockTickerJob?.cancel()
        clockTickerJob = null
        captureDismissJob?.cancel()
        captureDismissJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopOnlineListeners()
    }

    private companion object {
        const val TAG = "ChessXVM"
        const val TWO_MINUTES_MS = 120_000L
    }
}
