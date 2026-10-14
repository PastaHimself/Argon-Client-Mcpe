package dev.argon.client.overlay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FpsPackInfoTest {
    @Test fun externalPackLinkUsesExpectedSecureOfficialHost() {
        assertTrue(FpsPackInfo.isTrustedProjectUrl())
        assertTrue(FpsPackInfo.PROJECT_URL.startsWith("https://www.curseforge.com/"))
        assertFalse(FpsPackInfo.PROJECT_URL.contains("shizuku", ignoreCase = true))
    }

    @Test fun installationInstructionsRequireMinecraftActivation() {
        assertTrue(FpsPackInfo.INSTALL_NOTE.contains(".mcpack"))
        assertTrue(FpsPackInfo.INSTALL_NOTE.contains("Global Resources"))
        assertTrue(FpsPackInfo.INSTALL_NOTE.contains("cannot toggle"))
    }
}
