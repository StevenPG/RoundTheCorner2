package com.stevenpg.roundthecorner2.feature.triggers.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stevenpg.roundthecorner2.core.designsystem.theme.Spacing
import com.stevenpg.roundthecorner2.feature.triggers.R

/**
 * Explains the one thing blocking (or weakening) monitoring and offers the fix.
 * Required items use the error palette; recommended ones are softer and can
 * be dismissed for the session.
 */
@Composable
internal fun RequirementBanner(
    requirement: MonitoringRequirement,
    onFix: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (icon, title, body, action) = when (requirement) {
        MonitoringRequirement.PRECISE_LOCATION ->
            Quad(Icons.Outlined.LocationOff, R.string.banner_location_title, R.string.banner_location_body, R.string.banner_allow)
        MonitoringRequirement.NOTIFICATIONS ->
            Quad(Icons.Outlined.NotificationsOff, R.string.banner_notifications_title, R.string.banner_notifications_body, R.string.banner_allow)
        MonitoringRequirement.BACKGROUND_LOCATION ->
            Quad(Icons.Outlined.RestartAlt, R.string.banner_background_title, R.string.banner_background_body, R.string.banner_open_settings)
        MonitoringRequirement.BATTERY ->
            Quad(Icons.Outlined.BatteryAlert, R.string.banner_battery_title, R.string.banner_battery_body, R.string.banner_open_settings)
    }
    val required = !requirement.optional
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (required) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = if (required) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.lg, bottom = Spacing.sm)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f).padding(end = Spacing.sm)) {
                    Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = Spacing.sm),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (requirement.optional) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.banner_not_now)) }
                }
                FilledTonalButton(onClick = onFix) { Text(stringResource(action)) }
            }
        }
    }
}

private data class Quad(val icon: ImageVector, val title: Int, val body: Int, val action: Int)
