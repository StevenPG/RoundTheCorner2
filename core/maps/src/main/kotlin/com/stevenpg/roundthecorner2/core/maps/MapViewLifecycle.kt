package com.stevenpg.roundthecorner2.core.maps

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

/**
 * Creates a [MapView] tied to the current Compose lifecycle (MapView manages
 * GL resources and must be told about start/stop/destroy), and exposes the
 * [MapLibreMap] once it's ready.
 */
@Composable
internal fun rememberMapView(configure: (MapLibreMap) -> Unit = {}): Pair<MapView, MapLibreMap?> {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        MapLibre.getInstance(context) // Must precede the first MapView.
        MapView(context).apply { onCreate(null) }
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }
    LaunchedEffect(mapView) {
        mapView.getMapAsync { m ->
            configure(m)
            map = m
        }
    }
    return mapView to map
}

/** Light or dark OpenFreeMap style, following the app theme (not just the system). */
@Composable
internal fun themedStyleUrl(): String =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) MapStyles.DARK else MapStyles.LIGHT
