package com.phuongnn14.tuithantai.backup

import org.junit.Assert.assertTrue
import org.junit.Test

class DriveAuthSupportTest {
    @Test
    fun developerErrorExplainsSigningConfiguration() {
        val message = DriveAuthSupport.signInErrorMessage(10)

        assertTrue(message.contains("SHA-1"))
        assertTrue(message.contains("Firebase"))
    }

    @Test
    fun cancelledSignInIsReportedClearly() {
        assertTrue(DriveAuthSupport.signInErrorMessage(12501).contains("hủy"))
    }
}
