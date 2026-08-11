@file:OptIn(ExperimentalMaterial3Api::class)

package com.nandan.calorimeterapp.ui

import android.app.Activity
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.nandan.calorimeterapp.data.repository.FoodRepository
import com.nandan.calorimeterapp.ui.add.AddFoodSheet
import com.nandan.calorimeterapp.ui.add.AddFoodViewModel
import com.nandan.calorimeterapp.ui.auth.AuthScreen
import com.nandan.calorimeterapp.ui.camera.BarcodeScannerScreen
import com.nandan.calorimeterapp.ui.camera.CameraAnalyzeScreen
import com.nandan.calorimeterapp.ui.home.HomeScreen
import com.nandan.calorimeterapp.ui.home.HomeViewModel
import com.nandan.calorimeterapp.ui.onboarding.OnboardingScreen
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

sealed class AppScreen {
    data object Auth : AppScreen()
    data object Onboarding : AppScreen()
    data object Home : AppScreen()
    data object Streak : AppScreen()
}

@Composable
fun AppNavigation(
    auth: FirebaseAuth,
    googleSignInClient: GoogleSignInClient,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf<AppScreen>(
        if (auth.currentUser != null) AppScreen.Home else AppScreen.Auth
    )}
    var uid by remember { mutableStateOf(auth.currentUser?.uid ?: "") }
    var userWeight by remember { mutableStateOf("70") }
    var userGoal by remember { mutableStateOf("Maintain Weight") }

    // Interstitial ad state
    var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }

    // Load interstitial
    fun loadAd() {
        InterstitialAd.load(
            context,
            "ca-app-pub-2842829612476029/1293112009",
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
                override fun onAdFailedToLoad(err: LoadAdError) {
                    interstitialAd = null
                    Log.d("Ads", "Interstitial failed: ${err.message}")
                }
            }
        )
    }

    LaunchedEffect(Unit) { loadAd() }

    // Sub-screen overlays
    var showAddFood by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var showBarcode by remember { mutableStateOf(false) }

    // Shared ViewModels
    val homeViewModel: HomeViewModel = viewModel()
    val addFoodViewModel: AddFoodViewModel = viewModel()
    val streakViewModel: com.nandan.calorimeterapp.ui.streak.StreakViewModel = viewModel()

    // Repository for barcode lookup (no ViewModel needed — fire-and-forget pattern)
    val foodRepository = remember { FoodRepository() }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "screen_transition",
        ) { currentScreen ->
            when (currentScreen) {
                is AppScreen.Auth -> {
                    AuthScreen(
                        googleSignInClient = googleSignInClient,
                        onSuccess = { newUid, isNewUser ->
                            uid = newUid
                            screen = if (isNewUser) AppScreen.Onboarding else AppScreen.Home
                        },
                    )
                }
                is AppScreen.Onboarding -> {
                    OnboardingScreen(
                        onComplete = { weight, goal ->
                            userWeight = weight
                            userGoal = goal
                            screen = AppScreen.Home
                        }
                    )
                }
                is AppScreen.Home -> {
                    HomeScreen(
                        uid = uid,
                        currentWeight = userWeight,
                        currentGoal = userGoal,
                        mInterstitialAd = interstitialAd,
                        onAdShowComplete = { interstitialAd = null; loadAd() },
                        onLogout = {
                            auth.signOut()
                            scope.launch {
                                runCatching { googleSignInClient.signOut().await() }
                                uid = ""
                                screen = AppScreen.Auth
                            }
                        },
                        onUpdateSettings = { w, g ->
                            userWeight = w
                            userGoal = g
                        },
                        onShowAddFood = { showAddFood = true },
                        onShowCamera = { showCamera = true },
                        onShowBarcode = { showBarcode = true },
                        onShowStreak = { screen = AppScreen.Streak },
                        homeViewModel = homeViewModel,
                        streakViewModel = streakViewModel,
                    )
                }
                is AppScreen.Streak -> {
                    com.nandan.calorimeterapp.ui.streak.StreakScreen(
                        uid = uid,
                        viewModel = streakViewModel,
                        onBack = { screen = AppScreen.Home }
                    )
                }
            }
        }

        // Add Food Sheet overlay
        if (showAddFood) {
            AddFoodSheet(
                uid = uid,
                onDismiss = { showAddFood = false },
                onSuccess = {
                    showAddFood = false
                    homeViewModel.loadFoods(uid)
                    streakViewModel.loadStreak(uid)
                    interstitialAd?.let { ad ->
                        ad.show(context as Activity)
                        interstitialAd = null
                        loadAd()
                    }
                },
                onScanBarcode = {
                    showAddFood = false
                    showBarcode = true
                },
                addViewModel = addFoodViewModel,
            )
        }

        // Barcode Scanner overlay
        if (showBarcode) {
            BarcodeScannerScreen(
                onDismiss = {
                    showBarcode = false
                },
                onCodeScanned = { code ->
                    showBarcode = false
                    showAddFood = true
                    addFoodViewModel.fetchBarcode(code)
                },
            )
        }

        // Camera AI Screen overlay
        if (showCamera) {
            CameraAnalyzeScreen(
                onDismiss = { showCamera = false },
                onResult = { analyzeResult ->
                    showCamera = false
                    addFoodViewModel.applyAnalyzeResult(analyzeResult)
                    showAddFood = true
                },
            )
        }
    }
}
