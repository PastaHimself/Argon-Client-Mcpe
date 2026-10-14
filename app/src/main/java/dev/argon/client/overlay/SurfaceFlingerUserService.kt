package dev.argon.client.overlay

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Shizuku UserService: runs as the explicitly approved ADB shell identity.
 * It can only execute fixed SurfaceFlinger diagnostic commands. It never
 * touches Minecraft files/process memory or any user data.
 */
class SurfaceFlingerUserService : IFrameStatsService.Stub() {
    override fun listLayers(): String =
        execute(arrayOf("/system/bin/dumpsys", "SurfaceFlinger", "--list"))

    override fun readLayerLatency(layerName: String?): String {
        if (layerName.isNullOrBlank() ||
            layerName.length > 240 ||
            !layerName.contains("com.mojang.minecraftpe") ||
            layerName.any { it == '\n' || it == '\r' || it == '\u0000' }
        ) return ""
        return execute(arrayOf("/system/bin/dumpsys", "SurfaceFlinger", "--latency", layerName))
    }

    private fun execute(command: Array<String>): String {
        val process = try {
            ProcessBuilder(*command).redirectErrorStream(true).start()
        } catch (_: Exception) {
            return ""
        }
        val executor = Executors.newSingleThreadExecutor { job ->
            Thread(job, "argon-shizuku-diagnostics").apply { isDaemon = true }
        }
        return try {
            val result = executor.submit<String> {
                val output = StringBuilder()
                process.inputStream.bufferedReader().use { input ->
                    val buffer = CharArray(2048)
                    while (output.length < 128_000) {
                        val count = input.read(buffer, 0, minOf(buffer.size, 128_000 - output.length))
                        if (count < 0) break
                        output.append(buffer, 0, count)
                    }
                }
                if (!process.waitFor(300, TimeUnit.MILLISECONDS) || process.exitValue() != 0) "" else output.toString()
            }
            result.get(2_500L, TimeUnit.MILLISECONDS)
        } catch (_: Exception) {
            ""
        } finally {
            process.destroyForcibly()
            executor.shutdownNow()
        }
    }
}
