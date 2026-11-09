package com.stevenpg.roundthecorner2.core.monitoring

import com.stevenpg.roundthecorner2.core.monitoring.service.IntervalBuckets
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class IntervalBucketsTest {
    @Test
    fun `snaps down so it never checks less often than asked`() {
        assertEquals(10.seconds, IntervalBuckets.quantize(3.seconds))
        assertEquals(10.seconds, IntervalBuckets.quantize(14.9.seconds))
        assertEquals(30.seconds, IntervalBuckets.quantize(44.seconds))
        assertEquals(60.seconds, IntervalBuckets.quantize(60.seconds))
        assertEquals(300.seconds, IntervalBuckets.quantize(900.seconds))
    }
}
