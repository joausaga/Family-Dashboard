package com.lito.planify.dashboard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvLocalApiTest {
    @Test fun validSamsungDeviceResponseMeansReachableAndOnWhenNoPowerFieldExists() {
        assertTrue(TvLocalApi.parseIsOn("""{"device":{"model":"UN55RU7100","name":"TV Sala"}}"""))
    }

    @Test fun explicitPowerStateTakesPrecedence() {
        assertTrue(TvLocalApi.parseIsOn("""{"device":{"PowerState":"on"}}"""))
        assertFalse(TvLocalApi.parseIsOn("""{"device":{"PowerState":"off"}}"""))
    }

    @Test(expected = java.io.IOException::class)
    fun malformedOrUnrecognizedPayloadIsNotTreatedAsAnOnResponse() {
        TvLocalApi.parseIsOn("{}")
    }

    @Test fun settingsAcceptBlankIpToDisablePollingAndEnforceIntervalBounds() {
        assertEquals(null, TvMonitorSettings("", 1).validationError())
        assertEquals(null, TvMonitorSettings("192.168.1.2", 60).validationError())
        assertTrue(TvMonitorSettings("192.168.1.2", 0).validationError() != null)
        assertTrue(TvMonitorSettings("192.168.1.2", 2).validationError() != null)
        assertTrue(TvMonitorSettings("tv.local", 1).validationError() != null)
    }

    @Test fun validatesIpv4AndIpv6WithoutAcceptingHostnames() {
        assertTrue(TvLocalApi.isIpAddress("192.168.100.233"))
        assertTrue(TvLocalApi.isIpAddress("2001:db8::1"))
        assertFalse(TvLocalApi.isIpAddress("192.168.100.999"))
        assertFalse(TvLocalApi.isIpAddress("tv-sala.local"))
    }
}
