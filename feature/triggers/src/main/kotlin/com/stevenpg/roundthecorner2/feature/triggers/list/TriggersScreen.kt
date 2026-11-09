package com.stevenpg.roundthecorner2.feature.triggers.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stevenpg.roundthecorner2.core.designsystem.component.EmptyState
import com.stevenpg.roundthecorner2.core.designsystem.component.RtcScreen
import com.stevenpg.roundthecorner2.core.designsystem.theme.Spacing
import com.stevenpg.roundthecorner2.core.domain.format.RadiusInput
import com.stevenpg.roundthecorner2.core.model.TriggerDetails
import com.stevenpg.roundthecorner2.core.model.TriggerDirection
import com.stevenpg.roundthecorner2.core.model.TriggerId
import com.stevenpg.roundthecorner2.core.model.TriggerStatus
import com.stevenpg.roundthecorner2.core.ui.PlaceIconBadge
import com.stevenpg.roundthecorner2.feature.triggers.R
import com.stevenpg.roundthecorner2.feature.triggers.formatWhen
import java.time.ZoneId

@Composable
fun TriggersRoute(
    onAddTrigger: () -> Unit,
    onEditTrigger: (TriggerId) -> Unit,
    viewModel: TriggersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.deleted.collect { snackbarHostState.showSnackbar(resources.getString(R.string.triggers_deleted)) }
    }
    TriggersScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAddTrigger = onAddTrigger,
        onEditTrigger = onEditTrigger,
        onWatchingChange = viewModel::setWatching,
        onDelete = viewModel::delete,
    )
}

@Composable
fun TriggersScreen(
    uiState: TriggersUiState,
    onAddTrigger: () -> Unit,
    onEditTrigger: (TriggerId) -> Unit,
    onWatchingChange: (TriggerId, Boolean) -> Unit,
    onDelete: (TriggerId) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    zone: ZoneId = ZoneId.systemDefault(),
) {
    RtcScreen(
        title = stringResource(R.string.triggers_title),
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTrigger,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.triggers_add)) },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            TriggersUiState.Loading -> Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator()
            }
            is TriggersUiState.Ready ->
                if (uiState.triggers.isEmpty()) {
                    BoxWithConstraints(Modifier.fillMaxSize().padding(innerPadding)) {
                        Box(
                            Modifier.verticalScroll(rememberScrollState()).fillMaxWidth().heightIn(min = maxHeight),
                            contentAlignment = Alignment.Center,
                        ) {
                            EmptyState(
                                icon = Icons.Outlined.NotificationsActive,
                                title = stringResource(R.string.triggers_empty_title),
                                body = stringResource(R.string.triggers_empty_body),
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = Spacing.lg,
                            end = Spacing.lg,
                            top = innerPadding.calculateTopPadding() + Spacing.sm,
                            bottom = innerPadding.calculateBottomPadding() + 96.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(uiState.triggers, key = { it.trigger.id.value }) { details ->
                            TriggerCard(
                                details = details,
                                radiusText = RadiusInput.display(details.trigger.radius, uiState.unitSystem),
                                zone = zone,
                                onClick = { onEditTrigger(details.trigger.id) },
                                onWatchingChange = { onWatchingChange(details.trigger.id, it) },
                                onDelete = { onDelete(details.trigger.id) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
        }
    }
}

/**
 * One trigger: what/where on top, distance + recipient below, a status line,
 * and a switch to watch/pause (which also re-arms a trigger that already sent).
 */
@Composable
private fun TriggerCard(
    details: TriggerDetails,
    radiusText: String,
    zone: ZoneId,
    onClick: () -> Unit,
    onWatchingChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val trigger = details.trigger
    val armed = trigger.status == TriggerStatus.ARMED
    val title = stringResource(
        if (trigger.direction == TriggerDirection.ARRIVE) R.string.triggers_arrive_at else R.string.triggers_leave,
        details.place.name,
    )
    var menuOpen by remember { mutableStateOf(false) }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (armed) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .clickable(onClick = onClick)
                .padding(start = Spacing.lg, top = Spacing.md, bottom = Spacing.md, end = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Paused / sent triggers fade back so the active ones stand out.
            PlaceIconBadge(details.place.icon, modifier = Modifier.alpha(if (armed) 1f else 0.6f))
            Spacer(Modifier.width(Spacing.lg))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(
                        if (trigger.direction == TriggerDirection.ARRIVE) R.string.triggers_within else R.string.triggers_beyond,
                        radiusText,
                        details.recipient.name,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.size(Spacing.xs))
                if (details.recipient.needsReentry) {
                    StatusLine(
                        text = stringResource(R.string.triggers_recipient_needs_reentry, details.recipient.name),
                        color = MaterialTheme.colorScheme.error,
                        icon = true,
                    )
                } else {
                    StatusLine(
                        text = when (trigger.status) {
                            TriggerStatus.ARMED -> stringResource(R.string.triggers_status_armed)
                            TriggerStatus.PAUSED -> stringResource(R.string.triggers_status_paused)
                            TriggerStatus.FIRED -> stringResource(
                                R.string.triggers_status_fired,
                                trigger.lastFiredAt?.let { formatWhen(it, zone) }.orEmpty(),
                            )
                        },
                        color = if (armed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        dot = armed,
                    )
                }
            }
            val toggleDescription = stringResource(R.string.triggers_toggle, title)
            Switch(
                checked = armed,
                onCheckedChange = onWatchingChange,
                modifier = Modifier.padding(start = Spacing.sm).semantics { contentDescription = toggleDescription },
            )
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.triggers_more_actions))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.triggers_edit)) },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.triggers_delete)) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusLine(text: String, color: Color, dot: Boolean = false, icon: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (dot) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(color, CircleShape),
            )
            Spacer(Modifier.width(Spacing.xs + Spacing.xxs))
        }
        if (icon) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(Spacing.xs))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 2)
    }
}

