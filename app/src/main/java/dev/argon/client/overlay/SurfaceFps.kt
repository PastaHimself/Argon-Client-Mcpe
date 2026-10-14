package dev.argon.client.overlay

/** Parses actual Minecraft compositor presentation times; never guesses from refresh rate. */
object SurfaceFps {
    private const val MINECRAFT_PACKAGE = "com.mojang.minecraftpe"
    private const val PENDING = Long.MAX_VALUE
    private const val WINDOW_NS = 1_000_000_000L
    private const val IDLE_NS = 1_250_000_000L

    /**
     * SurfaceFlinger --list contains many layers. Only inspect a Minecraft-owned
     * SurfaceView / BLAST layer; refuse to measure a different app or the overlay.
     */
    fun selectLayer(list: String): String? = list.lineSequence()
        .map { it.trim().trim('"') }
        .filter { it.contains(MINECRAFT_PACKAGE) && !it.contains('\n') && it.length <= 240 }
        .sortedWith(compareBy<String> { !it.contains("SurfaceView", ignoreCase = true) }
            .thenBy { !it.contains("BLAST", ignoreCase = true) }
            .thenBy { it.length })
        .firstOrNull()

    /**
     * --latency <layer> reports refresh period followed by up to 128 triples:
     * app desired timestamp, SF present timestamp, completion timestamp.
     * Only *actual recent, unique* present times count.
     * Returns null for absent, stale, malformed, or frozen/idle surfaces.
     */
    fun readFps(report: String, nowNanos: Long): Double? {
        if (nowNanos <= 0L) return null
        val frameTimes = report.lineSequence()
            .drop(1) // The first line is refresh period, not an FPS reading.
            .mapNotNull { row ->
                val fields = row.trim().split(Regex("\\s+"))
                if (fields.size != 3) return@mapNotNull null
                val time = fields[1].toLongOrNull() ?: return@mapNotNull null
                if (time <= 0L || time == PENDING || time > nowNanos) null else time
            }
            .filter { it >= nowNanos - WINDOW_NS }
            .distinct()
            .sorted()
            .toList()
        if (frameTimes.size < 5) return null
        val first = frameTimes.first()
        val last = frameTimes.last()
        if (nowNanos - last > IDLE_NS) return null
        val duration = last - first
        if (duration < 250_000_000L) return null
        val fps = (frameTimes.size - 1).toDouble() * 1_000_000_000.0 / duration
        return if (fps.isFinite() && fps in 1.0..300.0) fps else null
    }
}
