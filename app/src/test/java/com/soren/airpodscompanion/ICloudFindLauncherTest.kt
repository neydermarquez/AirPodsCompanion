package com.soren.airpodscompanion

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URI

class ICloudFindLauncherTest {
    @Test
    fun findUrlUsesOfficialSecureAppleHost() {
        val uri = URI(ICLOUD_FIND_URL)

        assertEquals("https", uri.scheme)
        assertEquals("www.icloud.com", uri.host)
        assertEquals("/find/", uri.path)
    }
}
