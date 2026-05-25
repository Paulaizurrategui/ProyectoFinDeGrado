package com.paulaizurrategui.urtriply

import com.paulaizurrategui.urtriply.domain.model.countTripNights
import org.junit.Assert.assertEquals
import org.junit.Test

class TripRulesTest {
    @Test
    fun countTripNights_returnsAtLeastOneNight_whenDatesAreMissingOrInvalid() {
        assertEquals(1, countTripNights(null, null))
        assertEquals(1, countTripNights(1_000L, 1_000L))
        assertEquals(1, countTripNights(2_000L, 1_000L))
    }

    @Test
    fun countTripNights_computesWholeNights_fromMillisDifference() {
        val oneNight = 24L * 60L * 60L * 1000L
        assertEquals(3, countTripNights(0L, oneNight * 3))
    }
}
