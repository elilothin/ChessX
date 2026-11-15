package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.ChessXRepository
import com.example.ui.ChessXViewModel
import com.example.ui.UiState
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.GoogleSignInHelper
import com.example.ui.screens.ArmoryAndIslandScreen
import com.example.ui.screens.CampaignScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MenuScreen
import com.example.ui.screens.OnlineLobbyScreen
import com.example.ui.theme.ChessXTheme
import com.example.ui.theme.ImperialGold
import com.example.ui.theme.ObsidianBg
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

const val ROUTE_MENU = "menu"
const val ROUTE_CAMPAIGN = "campaign"
const val ROUTE_ONLINE = "online"
const val ROUTE_ARMORY = "armory"
const val ROUTE_HISTORY = "history"
const val ROUTE_GAME = "game"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChessXTheme {
                AppNavigation()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = { })
    } else {
        key(user.uid) {
            AuthenticatedChessXApp(
                currentUserId = user.uid,
                displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Stratège"
            )
        }
    }
}

@Composable
private fun AuthenticatedChessXApp(
    currentUserId: String,
    displayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: ChessXViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ChessXViewModel(
                    repository = ChessXRepository(db, Firebase.auth),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName
                )
            }
        }
    )

    val profileUi by viewModel.profileState.collectAsStateWithLifecycle()
    val matchHistory by viewModel.matchHistory.collectAsStateWithLifecycle()
    val waitingMatches by viewModel.waitingMatches.collectAsStateWithLifecycle()
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val statusMsg by viewModel.statusMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMsg) {
        if (statusMsg != null) {
            snackbarHostState.showSnackbar(statusMsg!!)
            viewModel.clearStatusMessage()
        }
    }

    val navController = rememberNavController()

    LaunchedEffect(gameState.isActive) {
        if (gameState.isActive && navController.currentDestination?.route != ROUTE_GAME) {
            navController.navigate(ROUTE_GAME) {
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        containerColor = ObsidianBg,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg)
                .padding(innerPadding)
        ) {
            when (val pState = profileUi) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = ImperialGold)
                            Text("Chargement des cinq univers...", color = ImperialGold)
                        }
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text("Erreur : ${pState.message}", color = MaterialTheme.colorScheme.error)
                            Button(
                                onClick = {
                                    GoogleSignInHelper.signOut(credentialManager, scope)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ImperialGold, contentColor = ObsidianBg)
                            ) {
                                Text("Se déconnecter")
                            }
                        }
                    }
                }
                is UiState.Success -> {
                    val profile = pState.data
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_MENU
                    ) {
                        composable(ROUTE_MENU) {
                            MenuScreen(
                                profile = profile,
                                onOpenCampaign = { navController.navigate(ROUTE_CAMPAIGN) },
                                onOpenOnline = { navController.navigate(ROUTE_ONLINE) },
                                onOpenArmory = { navController.navigate(ROUTE_ARMORY) },
                                onOpenHistory = { navController.navigate(ROUTE_HISTORY) },
                                onToggleAnimations = { viewModel.toggleAnimations(it) },
                                onSignOut = {
                                    viewModel.closeGame()
                                    GoogleSignInHelper.signOut(credentialManager, scope)
                                }
                            )
                        }
                        composable(ROUTE_CAMPAIGN) {
                            CampaignScreen(
                                profile = profile,
                                onBack = { navController.popBackStack() },
                                onStartCampaignMatch = { targetUniverse ->
                                    viewModel.startCampaignMatch(targetUniverse)
                                }
                            )
                        }
                        composable(ROUTE_ONLINE) {
                            OnlineLobbyScreen(
                                profile = profile,
                                waitingMatches = waitingMatches,
                                onBack = { navController.popBackStack() },
                                onCreateMatch = { minutes -> viewModel.createOnlineMatch(minutes) },
                                onJoinMatch = { match -> viewModel.joinOnlineMatch(match) }
                            )
                        }
                        composable(ROUTE_ARMORY) {
                            ArmoryAndIslandScreen(
                                profile = profile,
                                onBack = { navController.popBackStack() },
                                onSelectLoadout = { univ, board ->
                                    viewModel.selectLoadout(univ, board)
                                }
                            )
                        }
                        composable(ROUTE_HISTORY) {
                            HistoryScreen(
                                records = matchHistory,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(ROUTE_GAME) {
                            GameScreen(
                                gameState = gameState,
                                onSquareTapped = { viewModel.onSquareTapped(it) },
                                onSelectPromotion = { viewModel.selectPromotion(it) },
                                onCancelPromotion = { viewModel.cancelPromotion() },
                                onResign = { viewModel.resignCurrentMatch() },
                                onCancelWaiting = {
                                    viewModel.cancelOnlineWaiting()
                                    navController.popBackStack()
                                },
                                onSkipCaptureAnimation = { viewModel.dismissCaptureAnimation() },
                                onPlayAgainCampaign = { targetUniverse ->
                                    viewModel.startCampaignMatch(targetUniverse)
                                },
                                onExitToMenu = {
                                    viewModel.closeGame()
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
