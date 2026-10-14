package dev.argon.client.overlay

import dev.argon.client.core.ModuleCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayWidgetRegistryTest {
    @Test fun allowlistHasUniqueIdsAndExplanations() {
        assertTrue(OverlayWidgetRegistry.supported.isNotEmpty())
        assertEquals(
            OverlayWidgetRegistry.supported.size,
            OverlayWidgetRegistry.supported.map { it.id }.toSet().size
        )
        assertTrue(OverlayWidgetRegistry.supported.all { it.title.isNotBlank() && it.description.isNotBlank() })
        assertTrue(OverlayWidgetRegistry.supports(OverlayWidgetRegistry.CLOCK))
        assertTrue(OverlayWidgetRegistry.supports(OverlayWidgetRegistry.DEVICE))
    }

    @Test fun minecraftModulesCannotBeAccidentallyEnabledAsOverlayWidgets() {
        for (module in ModuleCatalog.all) {
            assertFalse(OverlayWidgetRegistry.supports(module.id))
        }
        assertFalse(OverlayWidgetRegistry.supports("fps_counter"))
        assertFalse(OverlayWidgetRegistry.supports("ping_display"))
        assertFalse(OverlayWidgetRegistry.supports("world_clock"))
        assertFalse(OverlayWidgetRegistry.supports("not_a_widget"))
        assertFalse(OverlayWidgetRegistry.supports(""))
    }
}
