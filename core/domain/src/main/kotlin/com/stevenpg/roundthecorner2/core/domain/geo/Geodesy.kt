package com.stevenpg.roundthecorner2.core.domain.geo

import com.stevenpg.roundthecorner2.core.model.Coordinates
import com.stevenpg.roundthecorner2.core.model.Distance
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Distances on the WGS84 ellipsoid (the datum GPS reports in).
 *
 * Uses Vincenty's inverse formula (sub-millimetre accuracy). For nearly
 * antipodal points, where Vincenty can fail to converge, it falls back to the
 * spherical haversine formula (error <= ~0.5%, irrelevant at those ranges).
 *
 * This is the same family of algorithm as Android's `Location.distanceBetween`,
 * but pure Kotlin so it can be unit-tested on the JVM.
 */
object Geodesy {

    // WGS84 ellipsoid parameters.
    private const val A = 6_378_137.0 // Semi-major axis (m)
    private const val F = 1 / 298.257223563 // Flattening
    private const val B = A * (1 - F) // Semi-minor axis (m)

    /** IUGG mean Earth radius, used by the spherical fallback. */
    private const val MEAN_RADIUS = 6_371_008.8

    private const val MAX_ITERATIONS = 200
    private const val CONVERGENCE_THRESHOLD = 1e-12

    /** Ellipsoidal distance between two points. */
    fun distance(from: Coordinates, to: Coordinates): Distance =
        Distance(vincentyMeters(from, to) ?: haversineMeters(from, to))

    /** Great-circle distance on a sphere. Exposed for comparison/testing. */
    fun haversineMeters(from: Coordinates, to: Coordinates): Double {
        val phi1 = Math.toRadians(from.latitude)
        val phi2 = Math.toRadians(to.latitude)
        val dPhi = phi2 - phi1
        val dLambda = Math.toRadians(to.longitude - from.longitude)
        val h = sin(dPhi / 2).let { it * it } + cos(phi1) * cos(phi2) * sin(dLambda / 2).let { it * it }
        return 2 * MEAN_RADIUS * asin(min(1.0, sqrt(h)))
    }

    /**
     * Vincenty inverse solution. Returns null if the iteration does not
     * converge (only happens for nearly antipodal points).
     * Variable names follow the original 1975 paper for traceability.
     */
    internal fun vincentyMeters(from: Coordinates, to: Coordinates): Double? {
        val l = Math.toRadians(to.longitude - from.longitude)
        val u1 = atan((1 - F) * tan(Math.toRadians(from.latitude))) // Reduced latitudes
        val u2 = atan((1 - F) * tan(Math.toRadians(to.latitude)))
        val sinU1 = sin(u1)
        val cosU1 = cos(u1)
        val sinU2 = sin(u2)
        val cosU2 = cos(u2)

        var lambda = l
        repeat(MAX_ITERATIONS) {
            val sinLambda = sin(lambda)
            val cosLambda = cos(lambda)
            val sinSigma = sqrt(
                (cosU2 * sinLambda).let { it * it } +
                    (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda).let { it * it },
            )
            if (sinSigma == 0.0) return 0.0 // Coincident points.

            val cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda
            val sigma = atan2(sinSigma, cosSigma)
            val sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma
            val cosSqAlpha = 1 - sinAlpha * sinAlpha
            // On the equator cosSqAlpha == 0; the term is then defined as 0.
            val cos2SigmaM = if (cosSqAlpha != 0.0) cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha else 0.0
            val c = F / 16 * cosSqAlpha * (4 + F * (4 - 3 * cosSqAlpha))

            val lambdaPrev = lambda
            lambda = l + (1 - c) * F * sinAlpha *
                (sigma + c * sinSigma * (cos2SigmaM + c * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)))

            if (abs(lambda - lambdaPrev) < CONVERGENCE_THRESHOLD) {
                val uSq = cosSqAlpha * (A * A - B * B) / (B * B)
                val bigA = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)))
                val bigB = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)))
                val deltaSigma = bigB * sinSigma * (
                    cos2SigmaM + bigB / 4 * (
                        cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) -
                            bigB / 6 * cos2SigmaM * (-3 + 4 * sinSigma * sinSigma) *
                            (-3 + 4 * cos2SigmaM * cos2SigmaM)
                        )
                    )
                return B * bigA * (sigma - deltaSigma)
            }
        }
        return null
    }
}
