package com.nandan.calorimeterapp.ui.auth

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val uid: String = "",
    val isNewUser: Boolean = false,
)

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                _uiState.value = AuthUiState(
                    isSuccess = true,
                    uid = result.user?.uid ?: "",
                    isNewUser = false,
                )
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = e.message ?: "Sign-in failed")
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                _uiState.value = AuthUiState(
                    isSuccess = true,
                    uid = result.user?.uid ?: "",
                    isNewUser = true,
                )
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = e.message ?: "Sign-up failed")
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                _uiState.value = AuthUiState(
                    isSuccess = true,
                    uid = result.user?.uid ?: "",
                    isNewUser = result.additionalUserInfo?.isNewUser ?: false,
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google sign-in failed", e)
                _uiState.value = AuthUiState(error = e.message ?: "Google sign-in failed")
            }
        }
    }

    fun resetPassword(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                onResult(true, "Reset email sent to $email")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to send reset email")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
