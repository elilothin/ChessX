package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.domain.model.UniverseId
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ChessXRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserProfile_and_recordCampaignVictory_updatesEloAndUnlocks() = runBlocking {
        val uid = signInTestUser("alice_chessx@test.com")
        val repo = ChessXRepository(firestore, auth)

        val profile = withTimeout(TIMEOUT_MS) {
            repo.ensureUserProfile("Alice")
        }
        assertEquals(uid, profile.userId)
        assertEquals(500L, profile.campaignElo)

        // Simulate 5th win against Western after 4 prior wins
        val preProfile = profile.copy(westernWins = 4L)
        val unlocked = withTimeout(TIMEOUT_MS) {
            repo.recordCampaignResult(
                profile = preProfile,
                opponentUniverse = UniverseId.WESTERN,
                isWin = true,
                endReason = "CHECKMATE",
                movesCount = 18
            )
        }
        assertEquals(UniverseId.WESTERN, unlocked)

        val updated = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(uid).first { it != null && it.westernWins == 5L }
        }
        assertNotNull(updated)
        assertEquals(600L, updated!!.campaignElo)
        assertTrue(updated.unlockedUniverses.contains("WESTERN"))
        assertEquals(1L, updated.islandLevel)
    }

    @Test
    fun onlineMatch_createJoinMoveAndCheckmate_enforcesSecurityAndSync() = runBlocking {
        val aliceUid = signInTestUser("alice_online@test.com")
        val aliceRepo = ChessXRepository(firestore, auth)
        val aliceProfile = withTimeout(TIMEOUT_MS) { aliceRepo.ensureUserProfile("Alice") }

        val matchId = withTimeout(TIMEOUT_MS) {
            aliceRepo.createOnlineMatch(aliceProfile, 5)
        }

        // Bob lists waiting lobbies and joins
        val bobUid = signInTestUser("bob_online@test.com")
        val bobRepo = ChessXRepository(firestore, auth)
        val bobProfile = withTimeout(TIMEOUT_MS) { bobRepo.ensureUserProfile("Bob") }

        val lobbies = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeWaitingMatches().first { list -> list.any { it.id == matchId } }
        }
        val lobby = lobbies.first { it.id == matchId }
        withTimeout(TIMEOUT_MS) { bobRepo.joinOnlineMatch(lobby, bobProfile) }

        // Alice plays checkmate move
        withTimeout(TIMEOUT_MS) {
            aliceRepo.submitOnlineMove(
                matchId = matchId,
                newFen = "rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3",
                nextTurn = "b",
                whiteTimeMs = 295_000L,
                blackTimeMs = 298_000L,
                lastMoveUci = "d8h4",
                lastCapturePiece = "Q",
                lastCaptureVictim = "p",
                lastCaptureUniverse = UniverseId.CLASSIQUE.id,
                isCheck = true,
                status = "checkmate",
                winnerId = aliceUid,
                winnerUniverse = aliceProfile.selectedUniverse,
                moveCount = 4L
            )
        }

        // Charlie cannot read an active/completed match between Alice and Bob
        signInTestUser("charlie_online@test.com")
        try {
            withTimeout(TIMEOUT_MS) {
                firestore.collection("onlineMatches").document(matchId).get().await()
            }
            fail("Expected PERMISSION_DENIED for non-member Charlie")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException() ?: (e as? FirebaseFirestoreException)
            assertNotNull(fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
        }
        assertTrue(bobUid != aliceUid)
    }

    @Test
    fun observeWaitingMatches_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = ChessXRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeWaitingMatches().first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated user")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
        }
    }

    private companion object {
        const val TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
