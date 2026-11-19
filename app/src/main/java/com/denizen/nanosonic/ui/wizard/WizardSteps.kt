package com.denizen.nanosonic.ui.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denizen.nanosonic.R
import com.denizen.nanosonic.data.autoeq.AutoEqEntry
import com.denizen.nanosonic.data.autoeq.Brand
import com.denizen.nanosonic.data.autoeq.Model
import com.denizen.nanosonic.ui.components.OptionCard
import com.denizen.nanosonic.ui.components.SearchField

@Composable
fun BrandStep(
    query: String,
    brands: List<Brand>,
    isLoading: Boolean,
    onQueryChanged: (String) -> Unit,
    onSelected: (Brand) -> Unit,
) {
    SearchStep(
        prompt = stringResource(R.string.wizard_brand_prompt),
        query = query,
        label = stringResource(R.string.wizard_brand_label),
        placeholder = stringResource(R.string.wizard_brand_placeholder),
        emptyText = stringResource(R.string.wizard_brand_none),
        items = brands,
        isLoading = isLoading,
        key = { it.key },
        onQueryChanged = onQueryChanged,
    ) { brand ->
        NavigationRow(Icons.Default.Headphones, brand.name, subtitle = null, onClick = { onSelected(brand) })
    }
}

@Composable
fun ModelStep(
    brandName: String,
    query: String,
    models: List<Model>,
    onQueryChanged: (String) -> Unit,
    onSelected: (Model) -> Unit,
) {
    val variantsText = stringResource(R.string.wizard_model_variants)
    SearchStep(
        prompt = stringResource(R.string.wizard_model_prompt, brandName),
        query = query,
        label = stringResource(R.string.wizard_model_label),
        placeholder = stringResource(R.string.wizard_model_placeholder),
        emptyText = stringResource(R.string.wizard_model_none),
        items = models,
        isLoading = false,
        key = { it.key },
        onQueryChanged = onQueryChanged,
    ) { model ->
        NavigationRow(
            Icons.Default.Headset,
            model.name,
            subtitle = variantsText.takeIf { model.variantCount > 1 },
            onClick = { onSelected(model) },
        )
    }
}

@Composable
private fun <T> SearchStep(
    prompt: String,
    query: String,
    label: String,
    placeholder: String,
    emptyText: String,
    items: List<T>,
    isLoading: Boolean,
    key: (T) -> Any,
    onQueryChanged: (String) -> Unit,
    row: @Composable (T) -> Unit,
) {
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    // Hide the keyboard once the user starts scrolling the results.
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) focusManager.clearFocus() }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(prompt, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
        SearchField(value = query, onValueChange = onQueryChanged, label = label, placeholder = placeholder)
        when {
            isLoading -> Centered { CircularProgressIndicator() }
            items.isEmpty() && query.isNotBlank() -> Centered {
                Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = key) { row(it) }
            }
        }
    }
}

@Composable
private fun NavigationRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    OptionCard(
        onClick = onClick,
        leading = { Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary) },
        trailing = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
fun ProfilesStep(
    modelName: String,
    variants: List<AutoEqEntry>,
    selected: Set<AutoEqEntry>,
    isSaving: Boolean,
    canComplete: Boolean,
    onToggle: (AutoEqEntry) -> Unit,
    onComplete: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.wizard_profiles_prompt, modelName),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.wizard_profiles_info_title), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.wizard_profiles_info), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            items(variants, key = { it.assetPath }) { entry ->
                val isSelected = entry in selected
                OptionCard(
                    onClick = { onToggle(entry) },
                    selected = isSelected,
                    leading = { Checkbox(checked = isSelected, onCheckedChange = null) },
                ) {
                    Text(
                        entry.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    Text(stringResource(R.string.wizard_source, entry.source), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.wizard_rig, entry.rig), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
            Button(
                onClick = onComplete,
                enabled = canComplete,
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.wizard_complete), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
