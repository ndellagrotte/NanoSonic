package com.denizen.nanosonic.ui.screens.wizard

/**
 * UI State for Wizard Screen
 * Manages the three-step device setup flow
 */
data class WizardState(
    // Current wizard step
    val currentStep: WizardStep = WizardStep.BRAND_SELECTION,

    // Step 1: Brand Selection
    val brandSearchQuery: String = "",
    val brands: List<DeviceBrand> = emptyList(),
    val selectedBrand: DeviceBrand? = null,

    // Step 2: Model Selection
    val modelSearchQuery: String = "",
    val models: List<DeviceModel> = emptyList(),
    val selectedModel: DeviceModel? = null,

    // Step 3: Variant Selection
    val variants: List<EQProfileVariant> = emptyList(),
    val selectedVariantIds: Set<String> = emptySet(),

    // UI state flags
    val isLoading: Boolean = false,
    val error: String? = null,
    val isComplete: Boolean = false
) {
    /**
     * Check if user can proceed to next step
     */
    val canProceed: Boolean
        get() = when (currentStep) {
            WizardStep.BRAND_SELECTION -> selectedBrand != null
            WizardStep.MODEL_SELECTION -> selectedModel != null
            WizardStep.VARIANT_SELECTION -> selectedVariantIds.isNotEmpty()
        }

    /**
     * Check if user can go back
     */
    val canGoBack: Boolean
        get() = currentStep != WizardStep.BRAND_SELECTION
}

/**
 * Wizard steps
 */
enum class WizardStep {
    BRAND_SELECTION,
    MODEL_SELECTION,
    VARIANT_SELECTION
}

/**
 * Device Brand model
 */
data class DeviceBrand(
    val id: String,
    val name: String,
    val logoUrl: String? = null
)

/**
 * Device Model
 */
data class DeviceModel(
    val id: String,
    val brandId: String,
    val name: String,
    val imageUrl: String? = null,
    val hasMultipleVariants: Boolean = false
)

/**
 * EQ Profile Variant
 */
data class EQProfileVariant(
    val id: String,
    val modelId: String,
    val name: String,
    val variant: String? = null,
    val isANC: Boolean = false,
    val isPadModified: Boolean = false,
    val description: String? = null,
    val source: String = "unknown", // Measurement source (e.g., oratory1990, crinacle)
    val rig: String = "unknown"    // Measurement rig (e.g., HMS II.3, GRAS 43AG-7)
) {
    /**
     * Display name with variant info including source and rig
     */
    val displayName: String
        get() = buildString {
            append(name)
            if (variant != null) {
                append(" - $variant")
            }
            when {
                isANC -> append(" (ANC)")
                isPadModified -> append(" (Velour pads)")
            }
        }

    /**
     * Source display string
     */
    val sourceDisplay: String
        get() = if (source != "unknown") "Source: $source" else ""

    /**
     * Rig display string
     */
    val rigDisplay: String
        get() = if (rig != "unknown") "Rig: $rig" else ""
}