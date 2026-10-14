package dev.argon.client.overlay

/**
 * Third-party Minecraft Bedrock FPS pack (MIT licensed by its author).
 *
 * Argon links to its official project page; it does not copy, download,
 * activate or republish anyone's pack. Players choose whether to install.
 * Minecraft renders the FPS HUD internally after the user activates the pack.
 */
object FpsPackInfo {
    const val PROJECT_URL =
        "https://www.curseforge.com/minecraft-bedrock/texture-packs/fps-display"

    const val INSTALL_NOTE =
        "Download the .mcpack from the creator's page, open it with Minecraft, " +
            "then enable it in Settings > Global Resources or in your world. " +
            "The Argon floating menu cannot toggle Minecraft resource packs."

    fun isTrustedProjectUrl(): Boolean =
        PROJECT_URL.startsWith("https://www.curseforge.com/minecraft-bedrock/texture-packs/")
}
