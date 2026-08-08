@file:OptIn(ExperimentalMaterial3Api::class)

package com.nandan.calorimeterapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.nandan.calorimeterapp.ui.theme.CalorimeterAppTheme
import com.nandan.calorimeterapp.ui.AppNavigation

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Ads (non-blocking)
        MobileAds.initialize(this) {
            Log.d("Ads", "MobileAds initialized")
        }

        setContent {
            CalorimeterAppTheme {
                AppNavigation(
                    auth = auth,
                    googleSignInClient = googleSignInClient,
                )
            }
        }
    }
}
