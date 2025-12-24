package com.example.nanosonicproject.ui.screens.wizard

import androidx.compose.animation.*
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.example.nanosonicproject.ui.components.NanoSonicTextField
import com.example.nanosonicproject.ui.theme.NanoSonicProjectTheme

/**
 * NanoSonic Wizard - Device Setup Flow
 * Three steps: Brand → Model → Variants
 */
@Composable
fun WizardScreen(
    viewModel: WizardViewModel = hiltViewModel(checkNotNull(LocalViewModelStoreOwner.current) {
                "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
            }, null),
    onNavigateToMain: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Handle completion
    LaunchedEffect(state.isComplete) {
        if (state.isComplete) {
            onNavigateToMain()
        }
    }

    WizardScreenContent(
        state = state,
        onBrandSearchQueryChanged = { viewModel.onBrandSearchQueryChanged(it) },
        onBrandSelected = { viewModel.onBrandSelected(it) },
        onModelSearchQueryChanged = { viewModel.onModelSearchQueryChanged(it) },
        onModelSelected = { viewModel.onModelSelected(it) },
        onVariantToggled = { viewModel.onVariantToggled(it) },
        onNextClicked = { viewModel.onNextClicked() },
        onBackClicked = { viewModel.onBackClicked() }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
private fun WizardScreenContent(
    state: WizardState,
    onBrandSearchQueryChanged: (String) -> Unit,
    onBrandSelected: (DeviceBrand) -> Unit,
    onModelSearchQueryChanged: (String) -> Unit,
    onModelSelected: (DeviceModel) -> Unit,
    onVariantToggled: (String) -> Unit,
    onNextClicked: () -> Unit,
    onBackClicked: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (state.currentStep) {
                            WizardStep.BRAND_SELECTION -> "Select Brand"
                            WizardStep.MODEL_SELECTION -> "Select Model"
                            WizardStep.VARIANT_SELECTION -> "Select Profiles"
                        }
                    )
                },
                navigationIcon = {
                    if (state.canGoBack) {
                        IconButton(onClick = onBackClicked) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Progress Indicator
                WizardProgressIndicator(currentStep = state.currentStep)

                // Step Content
                AnimatedContent(
                    targetState = state.currentStep,
                    label = "wizard_step",
                    transitionSpec = {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                    }
                ) { step ->
                    when (step) {
                        WizardStep.BRAND_SELECTION -> BrandSelectionStep(
                            searchQuery = state.brandSearchQuery,
                            brands = state.brands,
                            isLoading = state.isLoading,
                            onSearchQueryChanged = onBrandSearchQueryChanged,
                            onBrandSelected = onBrandSelected
                        )
                        WizardStep.MODEL_SELECTION -> ModelSelectionStep(
                            brandName = state.selectedBrand?.name ?: "",
                            searchQuery = state.modelSearchQuery,
                            models = state.models,
                            isLoading = state.isLoading,
                            onSearchQueryChanged = onModelSearchQueryChanged,
                            onModelSelected = onModelSelected
                        )
                        WizardStep.VARIANT_SELECTION -> VariantSelectionStep(
                            modelName = state.selectedModel?.name ?: "",
                            variants = state.variants,
                            selectedVariantIds = state.selectedVariantIds,
                            isLoading = state.isLoading,
                            onVariantToggled = onVariantToggled,
                            onCompleteClicked = onNextClicked,
                            canComplete = state.canProceed
                        )
                    }
                }
            }

            // Error Snackbar
            if (state.error != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(state.error)
                }
            }
        }
    }
}

@Composable
private fun WizardProgressIndicator(currentStep: WizardStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepIndicator(
            stepNumber = 1,
            label = "Brand",
            isActive = currentStep == WizardStep.BRAND_SELECTION,
            isCompleted = currentStep.ordinal > WizardStep.BRAND_SELECTION.ordinal
        )

        HorizontalDivider(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            thickness = DividerDefaults.Thickness, color = if (currentStep.ordinal > WizardStep.BRAND_SELECTION.ordinal) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )

        StepIndicator(
            stepNumber = 2,
            label = "Model",
            isActive = currentStep == WizardStep.MODEL_SELECTION,
            isCompleted = currentStep.ordinal > WizardStep.MODEL_SELECTION.ordinal
        )

        HorizontalDivider(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            thickness = DividerDefaults.Thickness, color = if (currentStep.ordinal > WizardStep.MODEL_SELECTION.ordinal) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )

        StepIndicator(
            stepNumber = 3,
            label = "Profiles",
            isActive = currentStep == WizardStep.VARIANT_SELECTION,
            isCompleted = false
        )
    }
}

@Composable
private fun StepIndicator(
    stepNumber: Int,
    label: String,
    isActive: Boolean,
    isCompleted: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = MaterialTheme.shapes.small,
            color = when {
                isCompleted -> MaterialTheme.colorScheme.primary
                isActive -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = stepNumber.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// STEP 1: BRAND SELECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun BrandSelectionStep(
    searchQuery: String,
    brands: List<DeviceBrand>,
    isLoading: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onBrandSelected: (DeviceBrand) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Search for your headphone or earphone brand",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        NanoSonicTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            label = "Brand Name",
            placeholder = "e.g., Sony, Bose, Beats",
            leadingIcon = Icons.Default.Search,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (brands.isEmpty() && searchQuery.isNotBlank()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No brands found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(brands) { brand ->
                    BrandItem(
                        brand = brand,
                        onClick = { onBrandSelected(brand) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandItem(
    brand: DeviceBrand,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Headphones,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = brand.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// STEP 2: MODEL SELECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ModelSelectionStep(
    brandName: String,
    searchQuery: String,
    models: List<DeviceModel>,
    isLoading: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onModelSelected: (DeviceModel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Search for your $brandName model",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        NanoSonicTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            label = "Model Name",
            placeholder = "e.g., Galaxy Buds, QuietComfort 45, WH-1000XM4",
            leadingIcon = Icons.Default.Search,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (models.isEmpty() && searchQuery.isNotBlank()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No models found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(models) { model ->
                    ModelItem(
                        model = model,
                        onClick = { onModelSelected(model) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelItem(
    model: DeviceModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Headset,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (model.hasMultipleVariants) {
                    Text(
                        text = "Multiple variants available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// STEP 3: VARIANT SELECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun VariantSelectionStep(
    modelName: String,
    variants: List<EQProfileVariant>,
    selectedVariantIds: Set<String>,
    isLoading: Boolean,
    onVariantToggled: (String) -> Unit,
    onCompleteClicked: () -> Unit,
    canComplete: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Select EQ profiles for $modelName",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "About EQ (Equalizer) Profiles",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Each profile provides device-specific sound quality enhancement to your music",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Multiple profiles for your device may be available from different sources and measured with different rigs",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• If unsure, select the topmost item and continue",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• After the setup is complete, custom EQ profiles may be imported through the 'EQ' screen",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            items(variants) { variant ->
                VariantItem(
                    variant = variant,
                    isSelected = selectedVariantIds.contains(variant.id),
                    onToggle = { onVariantToggled(variant.id) }
                )
            }
        }

        // Bottom button
        Surface(
            tonalElevation = 3.dp,
            shadowElevation = 8.dp
        ) {
            Button(
                onClick = onCompleteClicked,
                enabled = canComplete && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Complete Setup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun VariantItem(
    variant: EQProfileVariant,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = variant.displayName.substringBefore(" - "),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
                // Show source and rig information
                if (variant.sourceDisplay.isNotEmpty() || variant.rigDisplay.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    if (variant.sourceDisplay.isNotEmpty()) {
                        Text(
                            text = variant.sourceDisplay,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant                        )
                    }
                    if (variant.rigDisplay.isNotEmpty()) {
                        Text(
                            text = variant.rigDisplay,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREVIEW
// ═══════════════════════════════════════════════════════════════

@Preview(showBackground = true)
@Composable
private fun WizardBrandStepPreview() {
    NanoSonicProjectTheme {
        WizardScreenContent(
            state = WizardState(
                currentStep = WizardStep.BRAND_SELECTION,
                brandSearchQuery = "sony",
                brands = listOf(
                    DeviceBrand("sony", "Sony"),
                    DeviceBrand("sennheiser", "Sennheiser")
                )
            ),
            onBrandSearchQueryChanged = {},
            onBrandSelected = {},
            onModelSearchQueryChanged = {},
            onModelSelected = {},
            onVariantToggled = {},
            onNextClicked = {},
            onBackClicked = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WizardVariantStepPreview() {
    NanoSonicProjectTheme {
        WizardScreenContent(
            state = WizardState(
                currentStep = WizardStep.VARIANT_SELECTION,
                selectedModel = DeviceModel("sony_wh1000xm4", "sony", "WH-1000XM4"),
                variants = listOf(
                    EQProfileVariant("1", "sony_wh1000xm4", "WH-1000XM4", "Default"),
                    EQProfileVariant("2", "sony_wh1000xm4", "WH-1000XM4", "ANC On", isANC = true)
                ),
                selectedVariantIds = setOf("1")
            ),
            onBrandSearchQueryChanged = {},
            onBrandSelected = {},
            onModelSearchQueryChanged = {},
            onModelSelected = {},
            onVariantToggled = {},
            onNextClicked = {},
            onBackClicked = {}
        )
    }
}