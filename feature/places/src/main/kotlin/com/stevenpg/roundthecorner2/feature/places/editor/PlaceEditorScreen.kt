package com.stevenpg.roundthecorner2.feature.places.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stevenpg.roundthecorner2.core.designsystem.component.SectionHeader
import com.stevenpg.roundthecorner2.core.designsystem.theme.Spacing
import com.stevenpg.roundthecorner2.core.maps.LocationPickerMap
import com.stevenpg.roundthecorner2.core.model.Coordinates
import com.stevenpg.roundthecorner2.feature.places.R

/**
 * Renders the map. A slot so JVM screenshot tests can substitute a placeholder
 * (the real MapLibre view needs native GL libraries).
 */
typealias MapContent = @Composable (center: Coordinates?, onMoved: (Coordinates) -> Unit, modifier: Modifier) -> Unit

private val DefaultMapContent: MapContent = { center, onMoved, modifier ->
    LocationPickerMap(center = center, onCenterChangedByUser = onMoved, modifier = modifier)
}

@Composable
fun PlaceEditorRoute(
    placeId: Long?,
    onClose: () -> Unit,
    viewModel: PlaceEditorViewModel = hiltViewModel<PlaceEditorViewModel, PlaceEditorViewModel.Factory>(
        creationCallback = { factory -> factory.create(placeId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                PlaceEditorEvent.Saved, PlaceEditorEvent.Deleted -> onClose()
                is PlaceEditorEvent.InUse -> snackbarHostState.showSnackbar(
                    resources.getQuantityString(
                        R.plurals.places_in_use, event.triggerCount, uiState.name, event.triggerCount,
                    ),
                )
            }
        }
    }

    PlaceEditorScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onClose = onClose,
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceEditorScreen(
    uiState: PlaceEditorUiState,
    onAction: (PlaceEditorAction) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    mapContent: MapContent = DefaultMapContent,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.isNew) R.string.place_editor_new_title else R.string.place_editor_edit_title,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.place_editor_close))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onAction(PlaceEditorAction.Save) },
                        enabled = uiState.canSave,
                        modifier = Modifier.padding(end = Spacing.sm),
                    ) { Text(stringResource(R.string.place_editor_save)) }
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
                .imePadding(),
        ) {
            // The map stays fixed above the form, so dragging it never fights
            // with scrolling the form.
            Box(Modifier.fillMaxWidth().height(MAP_HEIGHT)) {
                mapContent(uiState.coordinates, { onAction(PlaceEditorAction.MapMoved(it)) }, Modifier.fillMaxSize())
                MapHint(Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.md))
            }
            EditorForm(
                uiState = uiState,
                onAction = onAction,
                onDeleteClick = { confirmDelete = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = Spacing.xxl),
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text(stringResource(R.string.places_delete_confirm_title, uiState.name)) },
            text = { Text(stringResource(R.string.places_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onAction(PlaceEditorAction.Delete)
                }) { Text(stringResource(R.string.places_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.places_cancel)) }
            },
        )
    }
}

@Composable
private fun EditorForm(
    uiState: PlaceEditorUiState,
    onAction: (PlaceEditorAction) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        SectionHeader(stringResource(R.string.place_editor_section_location))
        AddressSearch(
            query = uiState.searchQuery,
            search = uiState.search,
            onQueryChange = { onAction(PlaceEditorAction.SearchQueryChanged(it)) },
            onSubmit = { onAction(PlaceEditorAction.SearchSubmitted) },
            onResultSelected = { onAction(PlaceEditorAction.SearchResultSelected(it)) },
        )
        CoordinatesField(
            text = uiState.coordinatesText,
            coordinates = uiState.coordinates,
            error = uiState.coordinatesError,
            onTextChange = { onAction(PlaceEditorAction.CoordinatesTextChanged(it)) },
            modifier = Modifier.padding(top = Spacing.md),
        )

        SectionHeader(stringResource(R.string.place_editor_section_details))
        OutlinedTextField(
            value = uiState.name,
            onValueChange = { onAction(PlaceEditorAction.NameChanged(it)) },
            label = { Text(stringResource(R.string.place_editor_name_label)) },
            placeholder = { Text(stringResource(R.string.place_editor_name_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        IconPicker(
            selected = uiState.icon,
            onSelected = { onAction(PlaceEditorAction.IconSelected(it)) },
            modifier = Modifier.padding(top = Spacing.lg),
        )
        OutlinedTextField(
            value = uiState.address,
            onValueChange = { onAction(PlaceEditorAction.AddressChanged(it)) },
            label = { Text(stringResource(R.string.place_editor_address_label)) },
            minLines = 1,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text,
            ),
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg),
        )

        if (!uiState.isNew) {
            OutlinedButton(
                onClick = onDeleteClick,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xxl),
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(
                    stringResource(R.string.place_editor_delete),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }
        }
    }
}

/** Translucent pill reminding the user the map is draggable. */
@Composable
private fun MapHint(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs + Spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.PanTool,
                contentDescription = null,
                modifier = Modifier.padding(end = Spacing.xs + Spacing.xxs).height(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.place_editor_map_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val MAP_HEIGHT = 240.dp
