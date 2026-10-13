package dev.argon.client.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleCatalogTest {
    @Test fun catalogHasUniqueStableIdentifiers() {
        assertTrue(ModuleCatalog.all.isNotEmpty())
        assertEquals(ModuleCatalog.all.size, ModuleCatalog.all.map { it.id }.toSet().size)
        assertNotNull(ModuleCatalog.find("fps_counter"))
        assertNull(ModuleCatalog.find("does_not_exist"))
    }

    @Test fun noInGameModulesCanRunWithoutAnIntegration() {
        assertTrue(ModuleCatalog.all.all {
            ModuleGate.status(it, NoGameIntegration) == ModuleStatus.UNAVAILABLE
        })
    }

    @Test fun bridgeMustBeVerifiedAndProvideEveryNeededCapability() {
        val fps = ModuleCatalog.find("fps_counter")!!
        val unverified = object : GameIntegration {
            override val readyAndVerified = false
            override val capabilities = GameCapability.entries.toSet()
        }
        val partial = object : GameIntegration {
            override val readyAndVerified = true
            override val capabilities = setOf(GameCapability.HUD_DRAW)
        }
        val complete = object : GameIntegration {
            override val readyAndVerified = true
            override val capabilities = setOf(GameCapability.HUD_DRAW, GameCapability.GAME_METRICS)
        }
        assertEquals(ModuleStatus.UNAVAILABLE, ModuleGate.status(fps, unverified))
        assertEquals(ModuleStatus.UNAVAILABLE, ModuleGate.status(fps, partial))
        assertEquals(ModuleStatus.AVAILABLE, ModuleGate.status(fps, complete))
    }

    @Test fun categoryAndSearchFilterWork() {
        val visible = ModuleCatalog.search("counter", ModuleCategory.HUD)
        assertTrue(visible.any { it.id == "fps_counter" })
        assertFalse(visible.any { it.category != ModuleCategory.HUD })
        assertTrue(ModuleCatalog.search("NO MATCH").isEmpty())
        assertEquals(ModuleCatalog.all.size, ModuleCatalog.search("   ").size)
    }
}
