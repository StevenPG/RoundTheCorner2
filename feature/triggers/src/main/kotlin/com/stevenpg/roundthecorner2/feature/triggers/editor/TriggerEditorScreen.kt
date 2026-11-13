package com.stevenpg.roundthecorner2.feature.triggers.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stevenpg.roundthecorner2.core.designsystem.component.SectionHeader
import com.stevenpg.roundthecorner2.core.designsystem.theme.Spacing
import com.stevenpg.roundthecorner2.core.domain.format.RadiusInput
import com.stevenpg.roundthecorner2.core.domain.message.MessageTemplate.Placeholder
import com.stevenpg.roundthecorner2.core.maps.RadiusPreviewMap
import com.stevenpg.roundthecorner2.core.model.Coordinates
import com.stevenpg.roundthecorner2.core.model.Distance
import com.stevenpg.roundthecorner2.core.model.SendAttempt
import com.stevenpg.roundthecorner2.core.model.SendStatus
import com.stevenpg.roundthecorner2.core.model.TriggerDirection
import com.stevenpg.roundthecorner2.core.ui.ChannelBadge
import com.stevenpg.roundthecorner2.core.ui.PlaceIconBadge
import com.stevenpg.roundthecorner2.feature.triggers.R
import com.stevenpg.roundthecorner2.feature.triggers.formatWhen
import java.time.LocalDate
import java.time.ZoneId

/** Map slot (see the Places editor): screenshot tests swap in a placeholder for MapLibre. */
typealias RadiusMapContent = @Composable (center: Coordinates, radius: Distance, modifier: Modifier) -> Unit

private val DefaultRadiusMap: RadiusMapContent = { center, radius, modifier -> RadiusPreviewMap(center, radius, modifier) }

@Composable
fun TriggerEditorRoute(
    triggerId: Long?,
    onClose: () -> Unit,
    onCreatePlace: () -> Unit,
    onCreateRecipient: () -> Unit,
) {
    val defaults = MessageDefaults(
        arrive = stringResource(R.string.trigger_editor_default_arrive),
        leave = stringResource(R.string.trigger_editor_default_leave),
    )
    val viewModel = hiltViewModel<TriggerEditorViewModel, TriggerEditorViewModel.Factory>(
        creationCallback = { it.create(triggerId, defaults) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.events.collect { onClose() } } // Saved or Deleted.

    TriggerEditorScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onClose = onClose,
        onCreatePlace = onCreatePlace,
        onCreateRecipient = onCreateRecipient,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerEditorScreen(
    uiState: TriggerEditorUiState,
    onAction: (TriggerEditorAction) -> Unit,
    onClose: () -> Unit,
    onCreatePlace: () -> Unit,
    onCreateRecipient: () -> Unit,
    modifier: Modifier = Modifier,
    mapContent: RadiusMapContent = DefaultRadiusMap,
    zone: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zone),
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (uiState.isNew) R.string.trigger_editor_new_title else R.string.trigger_editor_edit_title))
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.trigger_editor_close))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onAction(TriggerEditorAction.Save) },
                        enabled = uiState.canSave,
                        modifier = Modifier.padding(end = Spacing.sm),
                    ) { Text(stringResource(R.string.trigger_editor_save)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.xxl),
        ) {
            // Live preview of the trigger area; updates as place/distance change.
            val place = uiState.place
            val radius = uiState.radius?.takeIf { uiState.radiusError == null }
            Box(Modifier.fillMaxWidth().height(MAP_HEIGHT)) {
                if (place != null && radius != null) {
                    mapContent(place.coordinates, radius, Modifier.fillMaxSize())
                } else {
                    MapPlaceholder()
                }
            }

            Column(Modifier.padding(horizontal = Spacing.lg)) {
                SectionHeader(stringResource(R.string.trigger_editor_section_when))
                DirectionSelector(uiState.direction) { onAction(TriggerEditorAction.DirectionSelected(it)) }
                Text(
                    stringResource(
                        if (uiState.direction == TriggerDirection.ARRIVE) {
                            R.string.trigger_editor_arrive_help
                        } else {
                            R.string.trigger_editor_leave_help
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Spacing.xs, top = Spacing.sm, bottom = Spacing.md),
                )
                PlacePicker(uiState, onAction, onCreatePlace)
                Spacer(Modifier.height(Spacing.md))
                DistanceField(uiState, onAction)

                SectionHeader(stringResource(R.string.trigger_editor_section_message))
                RecipientPicker(uiState, onAction, onCreateRecipient)
                Spacer(Modifier.height(Spacing.md))
                MessageEditor(uiState, onAction)

                if (!uiState.isNew) {
                    SectionHeader(stringResource(R.string.trigger_editor_section_history))
                    History(uiState.history, zone, today)
                    TextButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg),
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(Spacing.sm))
                        Text(stringResource(R.string.trigger_editor_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text(stringResource(R.string.trigger_editor_delete_confirm_title)) },
            text = { Text(stringResource(R.string.trigger_editor_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onAction(TriggerEditorAction.Delete)
                }) { Text(stringResource(R.string.triggers_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.trigger_editor_cancel)) }
            },
        )
    }
}

@Composable
private fun MapPlaceholder() {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxSize()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Outlined.Map, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Spacing.sm))
            Text(
                stringResource(R.string.trigger_editor_map_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DirectionSelector(selected: TriggerDirection, onSelected: (TriggerDirection) -> Unit) {
    val options = listOf(
        Triple(TriggerDirection.ARRIVE, R.string.trigger_editor_arrive, Icons.AutoMirrored.Outlined.Login),
        Triple(TriggerDirection.LEAVE, R.string.trigger_editor_leave, Icons.AutoMirrored.Outlined.Logout),
    )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (direction, label, icon) ->
            SegmentedButton(
                selected = direction == selected,
                onClick = { onSelected(direction) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                icon = {
                    SegmentedButtonDefaults.Icon(active = direction == selected) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                },
            ) { Text(stringResource(label)) }
        }
    }
}

/**
 * Generic "pick a saved thing" dropdown with a trailing "New…" entry. When
 * nothing is saved yet it collapses to a single button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SavedItemPicker(
    label: String,
    items: List<T>,
    selected: T?,
    itemName: (T) -> String,
    leading: @Composable (T) -> Unit,
    newLabel: String,
    emptyLabel: String,
    onSelect: (T) -> Unit,
    onCreate: () -> Unit,
) {
    if (items.isEmpty()) {
        OutlinedButton(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text(emptyLabel)
        }
        return
    }
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.let(itemName).orEmpty(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            leadingIcon = selected?.let { { Box(Modifier.padding(start = Spacing.sm)) { leading(it) } } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemName(item), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { leading(item) },
                    onClick = {
                        expanded = false
                        onSelect(item)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(newLabel, color = MaterialTheme.colorScheme.primary) },
                leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = {
                    expanded = false
                    onCreate()
                },
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
            )
        }
    }
}

@Composable
private fun PlacePicker(uiState: TriggerEditorUiState, onAction: (TriggerEditorAction) -> Unit, onCreatePlace: () -> Unit) {
    SavedItemPicker(
        label = stringResource(R.string.trigger_editor_place_label),
        items = uiState.places,
        selected = uiState.place,
        itemName = { it.name },
        leading = { PlaceIconBadge(it.icon, size = 28.dp) },
        newLabel = stringResource(R.string.trigger_editor_place_new),
        emptyLabel = stringResource(R.string.trigger_editor_place_none),
        onSelect = { onAction(TriggerEditorAction.PlaceSelected(it.id)) },
        onCreate = onCreatePlace,
    )
}

@Composable
private fun RecipientPicker(
    uiState: TriggerEditorUiState,
    onAction: (TriggerEditorAction) -> Unit,
    onCreateRecipient: () -> Unit,
) {
    SavedItemPicker(
        label = stringResource(R.string.trigger_editor_recipient_label),
        items = uiState.recipients,
        selected = uiState.recipient,
        itemName = { it.name },
        leading = { ChannelBadge(it.destination.channel, size = 28.dp) },
        newLabel = stringResource(R.string.trigger_editor_recipient_new),
        emptyLabel = stringResource(R.string.trigger_editor_recipient_none),
        onSelect = { onAction(TriggerEditorAction.RecipientSelected(it.id)) },
        onCreate = onCreateRecipient,
    )
    if (uiState.recipient?.needsReentry == true) {
        Text(
            stringResource(R.string.trigger_editor_recipient_needs_reentry),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = Spacing.lg, top = Spacing.xs),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DistanceField(uiState: TriggerEditorUiState, onAction: (TriggerEditorAction) -> Unit) {
    val unit = RadiusInput.unitLabel(uiState.unitSystem)
    val error = uiState.radiusError
    OutlinedTextField(
        value = uiState.radiusText,
        onValueChange = { onAction(TriggerEditorAction.RadiusTextChanged(it)) },
        label = { Text(stringResource(R.string.trigger_editor_distance_label)) },
        suffix = { Text(unit) },
        isError = error != null,
        supportingText = error?.let {
            {
                Text(
                    when (it) {
                        RadiusError.INVALID -> stringResource(R.string.trigger_editor_distance_invalid)
                        RadiusError.TOO_SMALL -> stringResource(R.string.trigger_editor_distance_too_small, uiState.minRadiusText)
                        RadiusError.TOO_LARGE -> stringResource(R.string.trigger_editor_distance_too_large, uiState.maxRadiusText)
                    },
                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.padding(top = Spacing.sm),
    ) {
        RadiusInput.PRESETS.forEach { value ->
            val label = RadiusInput.format(RadiusInput.toDistance(value, uiState.unitSystem), uiState.unitSystem)
            FilterChip(
                selected = uiState.radiusText == label,
                onClick = { onAction(TriggerEditorAction.RadiusPresetSelected(value)) },
                label = { Text("$label $unit") },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageEditor(uiState: TriggerEditorUiState, onAction: (TriggerEditorAction) -> Unit) {
    OutlinedTextField(
        value = uiState.message,
        onValueChange = { onAction(TriggerEditorAction.MessageChanged(it)) },
        label = { Text(stringResource(R.string.trigger_editor_message_label)) },
        minLines = 3,
        maxLines = 6,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        stringResource(R.string.trigger_editor_insert),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = Spacing.xs, top = Spacing.md),
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        listOf(
            Placeholder.PLACE to R.string.placeholder_place,
            Placeholder.DISTANCE to R.string.placeholder_distance,
            Placeholder.TIME to R.string.placeholder_time,
            Placeholder.RADIUS to R.string.placeholder_radius,
        ).forEach { (placeholder, label) ->
            AssistChip(
                onClick = { onAction(TriggerEditorAction.InsertPlaceholder(placeholder)) },
                label = { Text(stringResource(label)) },
                leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
            )
        }
    }

    // A chat-bubble preview of exactly what will be sent.
    Text(
        stringResource(R.string.trigger_editor_preview),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = Spacing.xs, top = Spacing.md, bottom = Spacing.sm),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.large.copy(bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd),
            modifier = Modifier.fillMaxWidth(0.85f),
        ) {
            Text(uiState.preview, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(Spacing.md))
        }
    }
}

@Composable
private fun History(history: List<SendAttempt>, zone: ZoneId, today: LocalDate) {
    if (history.isEmpty()) {
        Text(
            stringResource(R.string.trigger_editor_history_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.xs),
        )
        return
    }
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(vertical = Spacing.xs)) {
            history.take(MAX_HISTORY).forEachIndexed { index, attempt ->
                if (index > 0) HorizontalDivider(Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                val (icon, tint) = when (attempt.status) {
                    SendStatus.SENT -> Icons.Outlined.CheckCircle to MaterialTheme.colorScheme.primary
                    SendStatus.AWAITING_USER -> Icons.Outlined.HourglassTop to MaterialTheme.colorScheme.tertiary
                    SendStatus.FAILED -> Icons.Outlined.ErrorOutline to MaterialTheme.colorScheme.error
                }
                HistoryRow(icon, tint, attempt, zone, today)
            }
        }
    }
}

@Composable
private fun HistoryRow(icon: ImageVector, tint: Color, attempt: SendAttempt, zone: ZoneId, today: LocalDate) {
    Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(top = 2.dp).size(20.dp))
        Spacer(Modifier.width(Spacing.lg))
        Column {
            Text(formatWhen(attempt.timestamp, zone, today), style = MaterialTheme.typography.labelLarge)
            Text(
                when (attempt.status) {
                    SendStatus.FAILED -> attempt.error ?: attempt.renderedMessage
                    SendStatus.AWAITING_USER -> stringResource(R.string.trigger_editor_history_awaiting)
                    SendStatus.SENT -> attempt.renderedMessage
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (attempt.status == SendStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val MAP_HEIGHT = 200.dp
private const val MAX_HISTORY = 10
