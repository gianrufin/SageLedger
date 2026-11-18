package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.ui.components.CrispCard
import com.example.ui.theme.LocalLedgerExtraColors
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val extraColors = LocalLedgerExtraColors.current
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var visible by remember { mutableStateOf(false) }
    var autoSignInJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        visible = true
        autoSignInJob = attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) +
                slideInVertically(
                    initialOffsetY = { it / 12 },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        extraColors.heroGradientTop,
                                        extraColors.heroGradientMid,
                                        extraColors.heroGradientBottom
                                    )
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.AccountBalanceWallet,
                                        contentDescription = "SageLedger emblem",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White
                            )
                            Text(
                                text = "Playful, automatic spending clarity for Google Pay, cards, and everyday cash.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                        }
                    }
                }

                CrispCard(
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FeatureRow(
                            icon = Icons.Rounded.NotificationsActive,
                            iconTint = MaterialTheme.colorScheme.tertiary,
                            title = "Instant payment capture",
                            subtitle = "Detects Wallet, card, and SMS purchase alerts and confirms them automatically."
                        )
                        FeatureRow(
                            icon = Icons.AutoMirrored.Rounded.ShowChart,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Interactive visual clarity",
                            subtitle = "Track daily, weekly, monthly, and yearly habits with donut breakdowns and savings pace."
                        )
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        autoSignInJob?.cancel()
                        autoSignInJob = null
                        errorMessage = null
                        isLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extraColors.contrastPillBg,
                        contentColor = extraColors.contrastPillText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("sign_in_google_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = extraColors.contrastPillText,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.sign_in_with_google),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = iconTint.copy(alpha = 0.14f),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun resolveDefaultWebClientId(context: Context): String? {
    val resId = context.resources.getIdentifier(
        "default_web_client_id",
        "string",
        context.packageName
    )
    if (resId == 0) return null
    return runCatching { context.getString(resId) }
        .getOrNull()
        ?.takeIf { it.isNotBlank() }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
): Job? {
    if (FirebaseApp.getApps(context).isEmpty()) {
        onUnauthenticated()
        return null
    }
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return null
    }
    val clientId = resolveDefaultWebClientId(context)
    if (clientId == null) {
        onUnauthenticated()
        return null
    }
    val hostContext = context.findActivity() ?: context

    return scope.launch {
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val cred = withTimeout(8_000L) {
                credentialManager.getCredential(hostContext, request)
            }.credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                withTimeout(15_000L) {
                    Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                }
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    if (FirebaseApp.getApps(context).isEmpty()) {
        onAuthError("Firebase is not initialized in this build (missing google-services.json).")
        return
    }
    val clientId = resolveDefaultWebClientId(context)
    if (clientId == null) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val activity = context.findActivity()
    if (activity == null) {
        onAuthError("Unable to launch sign-in from current context.")
        return
    }

    scope.launch {
        var settled = false
        try {
            val cred = requestGoogleCredentialWithFallback(
                activity = activity,
                credentialManager = credentialManager,
                clientId = clientId
            )
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                withTimeout(15_000L) {
                    Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                }
                settled = true
                onAuthSuccess()
            } else {
                settled = true
                onAuthError("Unexpected credential type returned.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            settled = true
            onAuthCancelled()
        } catch (e: TimeoutCancellationException) {
            Log.w("Auth", "Google Sign-In timed out waiting for Credential Manager / Firebase", e)
            settled = true
            onAuthError("Google Sign-In timed out. Check your internet connection and Google Play Services, then try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialException) {
            Log.e("Auth", "Google Sign-In credential error (${e.type}): ${e.message}", e)
            settled = true
            onAuthError(e.errorMessage?.toString()?.takeIf { it.isNotBlank() } ?: e.localizedMessage ?: "Sign in failed (${e.type})")
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            settled = true
            onAuthError(e.localizedMessage ?: "Sign in failed")
        } finally {
            if (!settled) {
                onAuthCancelled()
            }
        }
    }
}

private suspend fun requestGoogleCredentialWithFallback(
    activity: Activity,
    credentialManager: CredentialManager,
    clientId: String
) = try {
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()
    withTimeout(15_000L) {
        credentialManager.getCredential(activity, request).credential
    }
} catch (e: GetCredentialCancellationException) {
    throw e
} catch (e: Exception) {
    if (e is TimeoutCancellationException || e is NoCredentialException || e is GetCredentialException) {
        Log.w("Auth", "Primary Sign-In with Google prompt fell back (${e.javaClass.simpleName}): ${e.message}")
        val fallbackOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(false)
            .build()
        val fallbackRequest = GetCredentialRequest.Builder()
            .addCredentialOption(fallbackOption)
            .build()
        withTimeout(20_000L) {
            credentialManager.getCredential(activity, fallbackRequest).credential
        }
    } else {
        throw e
    }
}

fun performSignOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                runCatching { Firebase.auth.signOut() }
                onSignOutComplete()
            }
        }
    }
}
