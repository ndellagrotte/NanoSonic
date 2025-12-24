package com.denizen.nanosonic.ui.screens.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.denizen.nanosonic.ui.theme.NanoSonicProjectTheme

/**
 * Splash/Welcome Screen
 * Shows the NanoSonic logo and Get Started button
 */
@Composable
fun SplashScreen(
    viewModel: SplashViewModel = hiltViewModel(
        checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        },
        null
    ),
    onNavigateToWizard: () -> Unit,
    onNavigateToMain: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Handle navigation events
    LaunchedEffect(state.navigationEvent) {
        when (state.navigationEvent) {
            NavigationEvent.NavigateToWizard -> {
                onNavigateToWizard()
                viewModel.onNavigationHandled()
            }
            NavigationEvent.NavigateToMain -> {
                onNavigateToMain()
                viewModel.onNavigationHandled()
            }
            NavigationEvent.None -> { /* Do nothing */ }
        }
    }

    SplashScreenContent(
        isLoading = state.isLoading,
        onGetStartedClick = { viewModel.onGetStartedClicked() }
    )
}

@Composable
private fun SplashScreenContent(
    isLoading: Boolean,
    onGetStartedClick: () -> Unit
) {
    // Animation for fade in effect
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 800,
                    easing = FastOutSlowInEasing
                )
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo Section
                LogoSection()

                Spacer(modifier = Modifier.height(64.dp))

                // Get Started Button
                GetStartedButton(
                    isLoading = isLoading,
                    onClick = onGetStartedClick
                )
            }
        }
    }
}

@Composable
private fun LogoSection() {
    // Scale animation for logo
    val scale by rememberInfiniteTransition(label = "logo_pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo Image
        Surface(
            modifier = Modifier
                .size(120.dp)
                .scale(scale),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                // Placeholder text (replace with actual logo when available)
                Text(
                    text = "NS",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // App Name
        Text(
            text = "NanoSonic",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tagline
        Text(
            text = "Next-Gen Music Player with Integrated EQ",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GetStartedButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(
                text = "Get Started",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREVIEW
// ═══════════════════════════════════════════════════════════════

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    NanoSonicProjectTheme {
        SplashScreenContent(
            isLoading = false,
            onGetStartedClick = {}
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SplashScreenPreviewDark() {
    NanoSonicProjectTheme {
        SplashScreenContent(
            isLoading = false,
            onGetStartedClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenLoadingPreview() {
    NanoSonicProjectTheme {
        SplashScreenContent(
            isLoading = true,
            onGetStartedClick = {}
        )
    }
}
