package com.stevenpg.roundthecorner2.feature.places.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stevenpg.roundthecorner2.core.designsystem.theme.Spacing
import com.stevenpg.roundthecorner2.core.model.CoordinateParser
import com.stevenpg.roundthecorner2.core.model.Coordinates
import com.stevenpg.roundthecorner2.core.model.GeocodedPlace
import com.stevenpg.roundthecorner2.core.model.PlaceIcon
import com.stevenpg.roundthecorner2.core.ui.imageVector
import com.stevenpg.roundthecorner2.core.ui.label
import com.stevenpg.roundthecorner2.feature.places.R
import java.util.Locale
import kotlin.math.abs

/** Address search box with an inline results list underneath. */
@Composable
internal fun AddressSearch(
    query: String,
    search: SearchState,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onResultSelected: (GeocodedPlace) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text(stringResource(R.string.place_editor_search_label)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                when {
                    search == SearchState.Searching ->
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    query.isNotEmpty() -> IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.place_editor_search_clear))
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            modifier = Modifier.fillMaxWidth(),
        )

        AnimatedVisibility(visible = search is SearchState.Results) {
            val results = (search as? SearchState.Results)?.places.orEmpty()
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            ) {
                Column {
                    results.forEachIndexed { index, result ->
                        if (index > 0) {
                            HorizontalDivider(
                                Modifier.padding(start = 56.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                        ListItem(
                            modifier = Modifier.clickable { onResultSelected(result) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = {
                                Icon(
                                    Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            headlineContent = { Text(result.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = if (result.subtitle.isNotEmpty()) {
                                { Text(result.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            } else {
                                null
                            },
                        )
                    }
                    // Nominatim's usage policy asks for visible attribution.
                    Text(
                        stringResource(R.string.place_editor_search_attribution),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = Spacing.lg, bottom = Spacing.sm, top = Spacing.xs),
                    )
                }
            }
        }

        val message = when (search) {
            SearchState.NoResults -> R.string.place_editor_search_no_results
            SearchState.Failed -> R.string.place_editor_search_failed
            else -> null
        }
        if (message != null) {
            Text(
                stringResource(message),
                style = MaterialTheme.typography.bodySmall,
                color = if (search == SearchState.Failed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(start = Spacing.lg, top = Spacing.xs),
            )
        }
    }
}

/**
 * Free-form coordinates input. Accepts anything [CoordinateParser] does and
 * shows the interpreted position (with hemispheres) so the user can confirm
 * we read it the way they meant.
 */
@Composable
internal fun CoordinatesField(
    text: String,
    coordinates: Coordinates?,
    error: CoordinateParser.Error?,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Only range errors are shown in red; "unrecognized" is usually just
    // "still typing", so we show the format help instead of shouting.
    val isError = error == CoordinateParser.Error.LATITUDE_OUT_OF_RANGE ||
        error == CoordinateParser.Error.LONGITUDE_OUT_OF_RANGE

    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        label = { Text(stringResource(R.string.place_editor_coordinates_label)) },
        placeholder = { Text(stringResource(R.string.place_editor_coordinates_placeholder)) },
        leadingIcon = { Icon(Icons.Outlined.MyLocation, contentDescription = null) },
        trailingIcon = if (coordinates != null) {
            { Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        } else {
            null
        },
        isError = isError,
        supportingText = {
            Text(
                when {
                    error == CoordinateParser.Error.LATITUDE_OUT_OF_RANGE ->
                        stringResource(R.string.place_editor_coordinates_lat_range)
                    error == CoordinateParser.Error.LONGITUDE_OUT_OF_RANGE ->
                        stringResource(R.string.place_editor_coordinates_lng_range)
                    coordinates != null && text.isNotBlank() -> coordinates.toHemisphereString()
                    else -> stringResource(R.string.place_editor_coordinates_help)
                },
            )
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
        // Text keyboard: users need '-', ',', '°' and N/S/E/W, which number pads lack.
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
        modifier = modifier.fillMaxWidth(),
    )
}

/** "40.712800° N, 74.006000° W" - unambiguous confirmation of what we parsed. */
private fun Coordinates.toHemisphereString(): String {
    val lat = "%.6f° %s".format(Locale.ROOT, abs(latitude), if (latitude >= 0) "N" else "S")
    val lng = "%.6f° %s".format(Locale.ROOT, abs(longitude), if (longitude >= 0) "E" else "W")
    return "$lat, $lng"
}

/** Wrapping row of selectable icon chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun IconPicker(
    selected: PlaceIcon,
    onSelected: (PlaceIcon) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            stringResource(R.string.place_editor_icon_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.sm),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            PlaceIcon.entries.forEach { icon ->
                val isSelected = icon == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelected(icon) },
                    label = { Text(stringResource(icon.label)) },
                    leadingIcon = {
                        Icon(icon.imageVector, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                    },
                )
            }
        }
    }
}
