package com.stevenpg.roundthecorner2.core.maps

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.stevenpg.roundthecorner2.core.domain.geo.Geodesy
import com.stevenpg.roundthecorner2.core.model.Coordinates
import com.stevenpg.roundthecorner2.core.model.Distance
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

private const val SOURCE_ID = "rtc-radius"
private const val FILL_LAYER_ID = "rtc-radius-fill"
private const val LINE_LAYER_ID = "rtc-radius-line"

/**
 * Read-only map showing a trigger's radius as a translucent circle around the
 * place, zoomed so the whole circle is visible. Gestures are off: it's a
 * preview that updates live as the radius changes, not something to pan.
 */
@Composable
fun RadiusPreviewMap(
    center: Coordinates,
    radius: Distance,
    modifier: Modifier = Modifier,
) {
    val styleUrl = themedStyleUrl()
    val color = MaterialTheme.colorScheme.primary.toArgb()
    val paddingPx = with(LocalDensity.current) { 32.dp.roundToPx() }

    val (mapView, map) = rememberMapView { m ->
        m.uiSettings.setAllGesturesEnabled(false)
        m.uiSettings.isCompassEnabled = false
    }

    LaunchedEffect(map, styleUrl, center, radius, color) {
        val m = map ?: return@LaunchedEffect
        val ring = Geodesy.circle(center, radius)
        val polygon = Polygon.fromLngLats(listOf(ring.map { Point.fromLngLat(it.longitude, it.latitude) }))

        fun draw(style: Style) {
            val source = style.getSourceAs<GeoJsonSource>(SOURCE_ID)
                ?: GeoJsonSource(SOURCE_ID).also(style::addSource)
            source.setGeoJson(polygon)
            if (style.getLayer(FILL_LAYER_ID) == null) {
                style.addLayer(
                    FillLayer(FILL_LAYER_ID, SOURCE_ID).withProperties(
                        PropertyFactory.fillColor(color),
                        PropertyFactory.fillOpacity(0.18f),
                    ),
                )
                style.addLayer(
                    LineLayer(LINE_LAYER_ID, SOURCE_ID).withProperties(
                        PropertyFactory.lineColor(color),
                        PropertyFactory.lineWidth(2.5f),
                    ),
                )
            }
            val bounds = LatLngBounds.Builder()
                .includes(ring.map { LatLng(it.latitude, it.longitude) })
                .build()
            // post: bounds fitting needs the view to be laid out.
            mapView.post { m.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, paddingPx)) }
        }

        // Re-use the loaded style if it's the right one; otherwise (first run
        // or theme change) load it and draw once ready.
        val current = m.style
        if (current != null && current.isFullyLoaded && current.uri == styleUrl) draw(current) else m.setStyle(styleUrl, ::draw)
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
