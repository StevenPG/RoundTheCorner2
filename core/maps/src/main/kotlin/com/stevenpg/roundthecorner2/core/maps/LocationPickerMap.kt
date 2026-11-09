package com.stevenpg.roundthecorner2.core.maps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.stevenpg.roundthecorner2.core.model.Coordinates
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

/** Zoom used when jumping to a location (street level). */
private const val LOCATION_ZOOM = 16.0

/**
 * A map with a fixed pin in the centre ("move the map under the pin" picker,
 * as in ride-hailing apps). This avoids fiddly marker dragging and works
 * one-handed.
 *
 * - When [center] changes from outside (typed coordinates, search result) the
 *   camera animates there. A null center leaves the camera untouched (the map
 *   starts on a world view).
 * - When the user pans the map, [onCenterChangedByUser] is called once the
 *   camera settles. Programmatic moves never call it, so there's no feedback loop.
 */
@Composable
fun LocationPickerMap(
    center: Coordinates?,
    onCenterChangedByUser: (Coordinates) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnCenterChanged by rememberUpdatedState(onCenterChangedByUser)
    val styleUrl = themedStyleUrl()

    val (mapView, map) = rememberMapView { m ->
        m.uiSettings.apply {
            isRotateGesturesEnabled = false // North-up keeps the picker predictable.
            isTiltGesturesEnabled = false
            isCompassEnabled = false
        }
        var userGesture = false
        m.addOnCameraMoveStartedListener { reason ->
            userGesture = reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE
        }
        m.addOnCameraIdleListener {
            if (userGesture) {
                userGesture = false
                val target = m.cameraPosition.target ?: return@addOnCameraIdleListener
                Coordinates.orNull(target.latitude, target.longitude)?.let(currentOnCenterChanged)
            }
        }
    }

    // Swap style when the app theme changes (light/dark).
    LaunchedEffect(map, styleUrl) { map?.setStyle(styleUrl) }

    // Follow externally-set centers.
    LaunchedEffect(map, center) {
        val m = map ?: return@LaunchedEffect
        // Null (e.g. half-typed coordinates): leave the camera where it is
        // rather than jumping away on every keystroke.
        if (center == null) return@LaunchedEffect
        val target = LatLng(center.latitude, center.longitude)
        val current = m.cameraPosition.target
        // Ignore echoes of the user's own pan (sub-metre differences).
        if (current != null && current.distanceTo(target) < 1.0) return@LaunchedEffect
        val zoom = maxOf(m.cameraPosition.zoom, LOCATION_ZOOM)
        m.animateCamera(CameraUpdateFactory.newLatLngZoom(target, zoom), 600)
    }

    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.matchParentSize())
        // Always visible: with no location yet, panning the map is a way to pick one.
        CenterPin(Modifier.align(Alignment.Center))
    }
}

/** The fixed pin. Offset upward by half its height so its *tip* marks the centre. */
@Composable
private fun CenterPin(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Filled.Place,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.tertiary,
        modifier = modifier
            .offset(y = (-20).dp)
            .size(40.dp),
    )
}
