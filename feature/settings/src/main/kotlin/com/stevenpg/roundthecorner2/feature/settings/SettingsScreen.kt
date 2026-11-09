package com.stevenpg.roundthecorner2.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SquareFoot
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stevenpg.roundthecorner2.core.designsystem.component.RtcScreen
import com.stevenpg.roundthecorner2.core.designsystem.component.SectionHeader
import com.stevenpg.roundthecorner2.core.designsystem.theme.Spacing
import com.stevenpg.roundthecorner2.core.model.CheckInterval
import com.stevenpg.roundthecorner2.core.model.ResolvedUnitSystem
import com.stevenpg.roundthecorner2.core.model.ThemePreference
import com.stevenpg.roundthecorner2.core.model.UnitSystem

/**
 * Stateful entry point: obtains the ViewModel from Hilt and hands plain state +
 * callbacks to the stateless [SettingsScreen] (which previews/screenshots use).
 */
@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        onUnitSystemSelected = viewModel::onUnitSystemSelected,
        onThemeSelected = viewModel::onThemeSelected,
        onDynamicColorToggled = viewModel::onDynamicColorToggled,
        onCheckIntervalSelected = viewModel::onCheckIntervalSelected,
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onUnitSystemSelected: (UnitSystem) -> Unit,
    onThemeSelected: (ThemePreference) -> Unit,
    onDynamicColorToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onCheckIntervalSelected: (CheckInterval) -> Unit = {},
) {
    RtcScreen(title = stringResource(R.string.settings_title), modifier = modifier) { innerPadding ->
        when (uiState) {
            SettingsUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is SettingsUiState.Ready -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding,
            ) {
                item { UnitsSection(uiState, onUnitSystemSelected) }
                item { MonitoringSection(uiState.preferences.checkInterval, onCheckIntervalSelected) }
                item { AppearanceSection(uiState, onThemeSelected, onDynamicColorToggled) }
                item { AboutSection(uiState.appVersion) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Sections
// ---------------------------------------------------------------------------

@Composable
private fun UnitsSection(state: SettingsUiState.Ready, onSelected: (UnitSystem) -> Unit) {
    val automaticSummary = stringResource(
        when (state.localeUnitSystem) {
            ResolvedUnitSystem.METRIC -> R.string.settings_units_system_summary_metric
            ResolvedUnitSystem.IMPERIAL -> R.string.settings_units_system_summary_imperial
        },
    )
    SettingsGroup(title = stringResource(R.string.settings_section_units)) {
        // selectableGroup() tells accessibility services these are radio options.
        Column(Modifier.selectableGroup()) {
            UnitOption(
                icon = Icons.Outlined.Public,
                title = stringResource(R.string.settings_units_system),
                summary = automaticSummary,
                selected = state.preferences.unitSystem == UnitSystem.SYSTEM,
                onClick = { onSelected(UnitSystem.SYSTEM) },
            )
            GroupDivider()
            UnitOption(
                icon = Icons.Outlined.Straighten,
                title = stringResource(R.string.settings_units_metric),
                summary = stringResource(R.string.settings_units_metric_summary),
                selected = state.preferences.unitSystem == UnitSystem.METRIC,
                onClick = { onSelected(UnitSystem.METRIC) },
            )
            GroupDivider()
            UnitOption(
                icon = Icons.Outlined.SquareFoot,
                title = stringResource(R.string.settings_units_imperial),
                summary = stringResource(R.string.settings_units_imperial_summary),
                selected = state.preferences.unitSystem == UnitSystem.IMPERIAL,
                onClick = { onSelected(UnitSystem.IMPERIAL) },
            )
        }
    }
}

@Composable
private fun MonitoringSection(selected: CheckInterval, onSelected: (CheckInterval) -> Unit) {
    val options = listOf(
        CheckInterval.THIRTY_SECONDS to R.string.settings_interval_30s,
        CheckInterval.ONE_MINUTE to R.string.settings_interval_1m,
        CheckInterval.TWO_MINUTES to R.string.settings_interval_2m,
        CheckInterval.FIVE_MINUTES to R.string.settings_interval_5m,
    )
    SettingsGroup(title = stringResource(R.string.settings_section_monitoring)) {
        ListItem(
            colors = groupItemColors(),
            leadingContent = { LeadingIcon(Icons.Outlined.Timer) },
            headlineContent = { Text(stringResource(R.string.settings_check_interval)) },
            supportingContent = {
                Column {
                    Text(stringResource(R.string.settings_check_interval_summary))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = Spacing.sm)) {
                        options.forEachIndexed { index, (interval, label) ->
                            SegmentedButton(
                                selected = interval == selected,
                                onClick = { onSelected(interval) },
                                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                            ) { Text(stringResource(label)) }
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun AppearanceSection(
    state: SettingsUiState.Ready,
    onThemeSelected: (ThemePreference) -> Unit,
    onDynamicColorToggled: (Boolean) -> Unit,
) {
    SettingsGroup(title = stringResource(R.string.settings_section_appearance)) {
        ListItem(
            colors = groupItemColors(),
            leadingContent = { LeadingIcon(Icons.Outlined.DarkMode) },
            headlineContent = { Text(stringResource(R.string.settings_theme)) },
            supportingContent = {
                ThemeSelector(
                    selected = state.preferences.themePreference,
                    onSelected = onThemeSelected,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
            },
        )
        if (state.supportsDynamicColor) {
            GroupDivider()
            ListItem(
                colors = groupItemColors(),
                modifier = Modifier.toggleable(
                    value = state.preferences.useDynamicColor,
                    role = Role.Switch,
                    onValueChange = onDynamicColorToggled,
                ),
                leadingContent = { LeadingIcon(Icons.Outlined.Palette) },
                headlineContent = { Text(stringResource(R.string.settings_dynamic_color)) },
                supportingContent = { Text(stringResource(R.string.settings_dynamic_color_summary)) },
                trailingContent = {
                    // The whole row is toggleable, so the Switch itself is not clickable.
                    Switch(checked = state.preferences.useDynamicColor, onCheckedChange = null)
                },
            )
        }
    }
}

@Composable
private fun AboutSection(appVersion: String) {
    SettingsGroup(title = stringResource(R.string.settings_section_about)) {
        ListItem(
            colors = groupItemColors(),
            leadingContent = { LeadingIcon(Icons.Outlined.Info) },
            headlineContent = { Text(stringResource(R.string.settings_version)) },
            supportingContent = { Text(appVersion) },
        )
    }
}

// ---------------------------------------------------------------------------
// Building blocks
// ---------------------------------------------------------------------------

/** A titled, rounded card that groups related settings rows. */
@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = Spacing.lg)) {
        SectionHeader(title)
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(vertical = Spacing.xs)) { content() }
        }
    }
}

@Composable
private fun UnitOption(
    icon: ImageVector,
    title: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        colors = groupItemColors(),
        modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        leadingContent = { LeadingIcon(icon) },
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        trailingContent = { RadioButton(selected = selected, onClick = null) },
    )
}

@Composable
private fun ThemeSelector(
    selected: ThemePreference,
    onSelected: (ThemePreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        ThemePreference.SYSTEM to R.string.settings_theme_system,
        ThemePreference.LIGHT to R.string.settings_theme_light,
        ThemePreference.DARK to R.string.settings_theme_dark,
    )
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (theme, label) ->
            SegmentedButton(
                selected = theme == selected,
                onClick = { onSelected(theme) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(stringResource(label)) }
        }
    }
}

@Composable
private fun LeadingIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp, end = Spacing.lg),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

/** List rows inside a group are transparent so the group card shows through. */
@Composable
private fun groupItemColors() = ListItemDefaults.colors(
    containerColor = Color.Transparent,
)
