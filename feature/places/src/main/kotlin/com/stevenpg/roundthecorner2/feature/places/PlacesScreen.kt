package com.stevenpg.roundthecorner2.feature.places

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.stevenpg.roundthecorner2.core.designsystem.component.EmptyState
import com.stevenpg.roundthecorner2.core.designsystem.component.RtcScreen

/**
 * Places tab. Milestone 1 only renders the empty state; the list, ViewModel and
 * Room-backed data arrive in a later milestone.
 */
@Composable
fun PlacesScreen(
    onAddPlace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RtcScreen(
        title = stringResource(R.string.places_title),
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddPlace,
                icon = { Icon(Icons.Rounded.AddLocationAlt, contentDescription = null) },
                text = { Text(stringResource(R.string.places_add)) },
            )
        },
    ) { innerPadding ->
        // BoxWithConstraints + heightIn(min) centers the empty state vertically
        // while still letting it scroll (keeps copy reachable at large font
        // sizes, and lets the large app bar collapse).
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
}
