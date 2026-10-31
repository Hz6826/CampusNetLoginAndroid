package com.hz6826.xjtunetlogin

import com.hz6826.xjtunetlogin.update.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test
    fun stableIsNewerThanPrerelease() {
        assertTrue(UpdateChecker.compareVersions("v1.0.0", "v1.0.0-beta.1") > 0)
    }

    @Test
    fun prereleaseOrdering() {
        assertTrue(UpdateChecker.compareVersions("v1.0.0-beta.2", "v1.0.0-beta.1") > 0)
    }

    @Test
    fun numericOrdering() {
        assertTrue(UpdateChecker.compareVersions("v1.10.0", "v1.9.9") > 0)
    }

    @Test
    fun equalTagsWithPrefix() {
        assertEquals(0, UpdateChecker.compareVersions("1.0.0", "v1.0.0"))
    }
}
