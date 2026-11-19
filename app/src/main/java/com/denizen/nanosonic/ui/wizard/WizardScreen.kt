package com.denizen.nanosonic.ui.wizard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denizen.nanosonic.R

/**
 * @param canExit whether the wizard can be left from its first step (not during first-run setup).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardScreen(
    canExit: Boolean,
    onExit: () -> Unit,
    onFinished: () -> Unit,
    viewModel: WizardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isFinished) { if (state.isFinished) onFinished() }

    BackHandler(enabled = state.step != WizardStep.BRAND) { viewModel.back() }

    val errorText = when (val error = state.error) {
        WizardError.DatabaseUnavailable -> stringResource(R.string.wizard_error_database)
        is WizardError.SaveFailed -> stringResource(R.string.wizard_error_save, error.reason)
        null -> null
    }
    LaunchedEffect(errorText) {
        if (errorText != null) {
            snackbarHostState.showSnackbar(errorText)
            viewModel.onErrorShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            when (state.step) {
                                WizardStep.BRAND -> R.string.wizard_title_brand
                                WizardStep.MODEL -> R.string.wizard_title_model
                                WizardStep.PROFILES -> R.string.wizard_title_profiles
                            },
                        ),
                    )
                },
                navigationIcon = {
                    if (state.step != WizardStep.BRAND || canExit) {
                        IconButton(onClick = { if (!viewModel.back()) onExit() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            StepProgress(state.step)
            AnimatedContent(
                targetState = state.step,
                label = "wizard_step",
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    (slideInHorizontally { if (forward) it else -it } + fadeIn())
                        .togetherWith(slideOutHorizontally { if (forward) -it else it } + fadeOut())
                },
            ) { step ->
                when (step) {
                    WizardStep.BRAND -> BrandStep(
                        query = state.brandQuery,
                        brands = state.brands,
                        isLoading = state.isDatabaseLoading,
                        onQueryChanged = viewModel::onBrandQueryChanged,
                        onSelected = viewModel::onBrandSelected,
                    )
                    WizardStep.MODEL -> ModelStep(
                        brandName = state.brand?.name.orEmpty(),
                        query = state.modelQuery,
                        models = state.models,
                        onQueryChanged = viewModel::onModelQueryChanged,
                        onSelected = viewModel::onModelSelected,
                    )
                    WizardStep.PROFILES -> ProfilesStep(
                        modelName = state.model?.name.orEmpty(),
                        variants = state.variants,
                        selected = state.selected,
                        isSaving = state.isSaving,
                        canComplete = state.canComplete,
                        onToggle = viewModel::onVariantToggled,
                        onComplete = viewModel::complete,
                    )
                }
            }
        }
    }
}

@Composable
private fun StepProgress(current: WizardStep) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        WizardStep.entries.forEachIndexed { index, step ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    color = if (current.ordinal >= step.ordinal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                )
            }
            StepIndicator(number = index + 1, label = stepLabel(step), isActive = step == current, isCompleted = step.ordinal < current.ordinal)
        }
    }
}

@Composable
private fun stepLabel(step: WizardStep) = stringResource(
    when (step) {
        WizardStep.BRAND -> R.string.wizard_step_brand
        WizardStep.MODEL -> R.string.wizard_step_model
        WizardStep.PROFILES -> R.string.wizard_step_profiles
    },
)

@Composable
private fun StepIndicator(number: Int, label: String, isActive: Boolean, isCompleted: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = MaterialTheme.shapes.small,
            color = when {
                isCompleted -> MaterialTheme.colorScheme.primary
                isActive -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = null)
                } else {
                    Text(number.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
