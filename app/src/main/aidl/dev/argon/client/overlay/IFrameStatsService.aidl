package dev.argon.client.overlay;

// Runs under explicitly approved Shizuku shell identity, never a Minecraft hook.
interface IFrameStatsService {
    String listLayers();
    String readLayerLatency(String layerName);
}
