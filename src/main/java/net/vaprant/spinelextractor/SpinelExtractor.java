package net.vaprant.spinelextractor;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.vaprant.spinelextractor.extractors.BlockExtractor;
import net.vaprant.spinelextractor.extractors.PacketExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpinelExtractor implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("SpinelExtractor");

    @Override
    public void onInitialize() {


        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Starting extraction...");
            PacketExtractor.extract();
            BlockExtractor.extract();
            LOGGER.info("Extraction complete, stopping server...");
            server.stopServer();
        });
    }
}
