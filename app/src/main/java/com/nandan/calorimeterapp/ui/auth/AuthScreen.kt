package com.nandan.calorimeterapp.ui.auth

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.nandan.calorimeterapp.ui.theme.*

@Composable
fun AuthScreen(
    googleSignInClient: GoogleSignInClient,
    onSuccess: (uid: String, isNewUser: Boolean) -> Unit,
    authViewModel: AuthViewModel = viewModel(),
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Handle success
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onSuccess(uiState.uid, uiState.isNewUser)
        }
    }

    // Handle errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            authViewModel.clearError()
        }
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            authViewModel.signInWithGoogle(account.idToken ?: "")
        } catch (e: ApiException) {
            Toast.makeText(context, "Google Sign-In failed: ${e.statusCode}", Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D1117), Color(0xFF0A1628), Color(0xFF0D1117))
                )
            )
    ) {
        // Ambient glow
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(y = (-60).dp)
                .align(Alignment.TopCenter)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Emerald.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(60.dp))

            // Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(EmeraldContainer)
                    .border(1.dp, Emerald.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = Emerald,
                )
            }

            Spacer(Modifier.height(24.dp))

            AnimatedContent(targetState = isLogin, label = "auth_title") { login ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (login) "Welcome back" else "Create account",
                        style = MaterialTheme.typography.headlineMedium,
                        color = OnBackground,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (login) "Sign in to track your nutrition" else "Start your health journey today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            // Email field
            AppTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email address",
                leadingIcon = Icons.Default.Email,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )

            Spacer(Modifier.height(14.dp))

            // Password field
            AppTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                leadingIcon = Icons.Default.Lock,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = OnSurfaceMuted,
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
            )

            if (isLogin) {
                TextButton(
                    onClick = {
                        if (email.isNotBlank()) {
                            authViewModel.resetPassword(email) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Enter your email above first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Forgot password?", color = Emerald, fontSize = 13.sp)
                }
            } else {
                Spacer(Modifier.height(14.dp))
            }

            Spacer(Modifier.height(8.dp))

            // Primary CTA
            Button(
                onClick = {
                    if (isLogin) authViewModel.signIn(email, password)
                    else authViewModel.signUp(email, password)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Emerald,
                    contentColor = Color(0xFF0D1117),
                    disabledContainerColor = Emerald.copy(alpha = 0.3f),
                ),
                enabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank(),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color(0xFF0D1117),
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Text(
                        text = if (isLogin) "Sign In" else "Create Account",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = BorderColor)
                Text(
                    " OR ",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceMuted,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = BorderColor)
            }

            Spacer(Modifier.height(24.dp))

            // Google sign-in button
            OutlinedButton(
                onClick = { googleLauncher.launch(googleSignInClient.signInIntent) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OnBackground),
                border = null,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = OnSurfaceMuted,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Continue with Google",
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            TextButton(onClick = { isLogin = !isLogin }) {
                Text(
                    text = if (isLogin) "Don't have an account? " else "Already have an account? ",
                    color = OnSurfaceMuted,
                    fontSize = 14.sp,
                )
                Text(
                    text = if (isLogin) "Sign up" else "Sign in",
                    color = Emerald,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        leadingIcon = leadingIcon?.let { icon ->
            { Icon(icon, null, tint = OnSurfaceMuted, modifier = Modifier.size(20.dp)) }
        },
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Emerald,
            unfocusedBorderColor = BorderColor,
            focusedLabelColor = Emerald,
            unfocusedLabelColor = OnSurfaceMuted,
            cursorColor = Emerald,
            focusedTextColor = OnBackground,
            unfocusedTextColor = OnBackground,
            focusedContainerColor = SurfaceCard,
            unfocusedContainerColor = SurfaceCard,
        ),
    )
}
