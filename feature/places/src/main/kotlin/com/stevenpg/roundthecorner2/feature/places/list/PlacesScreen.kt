package com.stevenpg.roundthecorner2.feature.places.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stevenpg.roundthecorner2.core.designsystem.component.EmptyState
import com.stevenpg.roundthecorner2.core.designsystem.component.ListCard
import com.stevenpg.roundthecorner2.core.designsystem.component.ListCardAction
import com.stevenpg.roundthecorner2.core.designsystem.component.RtcScreen
import com.stevenpg.roundthecorner2.core.designsystem.theme.Spacing
import com.stevenpg.roundthecorner2.core.model.Place
import com.stevenpg.roundthecorner2.core.model.PlaceId
import com.stevenpg.roundthecorner2.core.ui.PlaceIconBadge
import com.stevenpg.roundthecorner2.feature.places.R

@Composable
fun PlacesRoute(
    onAddPlace: () -> Unit,
    onEditPlace: (PlaceId) -> Unit,
    viewModel: PlacesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            val text = when (message) {
                is PlacesMessage.Deleted -> resources.getString(R.string.places_deleted, message.name)
                is PlacesMessage.InUse -> resources.getQuantityString(
                    R.plurals.places_in_use, message.triggerCount, message.name, message.triggerCount,
                )
            }
            snackbarHostState.showSnackbar(text)
        }
    }

    PlacesScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAddPlace = onAddPlace,
        onEditPlace = { onEditPlace(it.id) },
        onDeletePlace = viewModel::delete,
    )
}

@Composable
fun PlacesScreen(
    uiState: PlacesUiState,
    onAddPlace: () -> Unit,
    onEditPlace: (Place) -> Unit,
    onDeletePlace: (Place) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    // The place awaiting delete confirmation; saved by id so it survives rotation.
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    RtcScreen(
        title = stringResource(R.string.places_title),
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddPlace,
                icon = { Icon(Icons.Rounded.AddLocationAlt, contentDescription = null) },
                text = { Text(stringResource(R.string.places_add)) },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            PlacesUiState.Loading -> Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator()
            }
            is PlacesUiState.Ready ->
                if (uiState.places.isEmpty()) {
                    EmptyPlaces(innerPadding)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = Spacing.lg,
                            end = Spacing.lg,
                            top = innerPadding.calculateTopPadding() + Spacing.sm,
                            // Leave room so the FAB never covers the last row.
                            bottom = innerPadding.calculateBottomPadding() + 96.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(uiState.places, key = { it.id.value }) { place ->
                            PlaceRow(
                                place = place,
                                onClick = { onEditPlace(place) },
                                onDelete = { pendingDeleteId = place.id.value },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
        }
    }

    val pending = (uiState as? PlacesUiState.Ready)?.places?.find { it.id.value == pendingDeleteId }
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text(stringResource(R.string.places_delete_confirm_title, pending.name)) },
            text = { Text(stringResource(R.string.places_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteId = null
                    onDeletePlace(pending)
                }) { Text(stringResource(R.string.places_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text(stringResource(R.string.places_cancel)) }
            },
        )
    }
}

@Composable
private fun EmptyPlaces(innerPadding: PaddingValues) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(innerPadding)) {
        Box(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .heightIn(min = maxHeight),
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(
                icon = Icons.Outlined.Place,
                title = stringResource(R.string.places_empty_title),
                body = stringResource(R.string.places_empty_body),
            )
        }
    }
}

/** A saved place: icon, name, and its address (or coordinates if it has none). */
@Composable
private fun PlaceRow(
    place: Place,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListCard(
        modifier = modifier,
        onClick = onClick,
        leading = { PlaceIconBadge(place.icon) },
        headline = { Text(place.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supporting = {
            Text(text = place.address ?: place.coordinates.toString(), maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        moreActionsDescription = stringResource(R.string.places_more_actions, place.name),
        actions = listOf(
            ListCardAction(stringResource(R.string.places_edit), Icons.Outlined.Edit, onClick),
            ListCardAction(stringResource(R.string.places_delete), Icons.Outlined.Delete, onDelete),
        ),
    )
}
