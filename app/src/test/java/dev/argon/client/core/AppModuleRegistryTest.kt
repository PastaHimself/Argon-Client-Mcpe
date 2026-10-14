package dev.argon.client.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AppModuleRegistryTest {
    @Test fun availableModulesAreUniqueAndNonempty() {
        assertEquals(AppModuleRegistry.all.size, AppModuleRegistry.all.map { it.id }.toSet().size)
        assertTrue(AppModuleRegistry.all.all { it.title.isNotBlank() && it.description.isNotBlank() })
        assertNotNull(AppModuleRegistry.find(AppModuleRegistry.UI_FPS))
        assertNotNull(AppModuleRegistry.find(AppModuleRegistry.DEVICE_STATUS))
    }

    @Test fun gameModulesAreNotAppModules() {
        assertFalse(AppModuleRegistry.isSupported("fps_counter"))
        assertFalse(AppModuleRegistry.isSupported("coordinates"))
        assertFalse(AppModuleRegistry.isSupported("ping_display"))
        assertFalse(AppModuleRegistry.isSupported(""))
        assertEquals(ModuleStatus.UNAVAILABLE, ModuleGate.status(ModuleCatalog.find("fps_counter")!!, NoGameIntegration))
    }

    @Test fun noGameIntegrationCannotEnableGameModules() {
        for (spec in ModuleCatalog.all) {
            assertEquals(ModuleStatus.UNAVAILABLE, ModuleGate.status(spec, NoGameIntegration))
            assertFalse(AppModuleRegistry.isSupported(spec.id))
        }
    }
}
