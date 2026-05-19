package net.vaprant.spinelextractor;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.vaprant.spinelextractor.extractors.BiomeExtractor;
import net.vaprant.spinelextractor.extractors.BlockExtractor;
import net.vaprant.spinelextractor.extractors.DimensionTypeExtractor;
import net.vaprant.spinelextractor.extractors.DynamicRegistryAssetExtractor;
import net.vaprant.spinelextractor.extractors.ItemExtractor;
import net.vaprant.spinelextractor.extractors.PacketExtractor;
import net.vaprant.spinelextractor.extractors.StaticRegistryTagExtractor;
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
            ItemExtractor.extract();
            BiomeExtractor.extract(server);
            DimensionTypeExtractor.extract(server);
            DynamicRegistryAssetExtractor.extract(server);
            StaticRegistryTagExtractor.extract();
            LOGGER.info("Extraction complete, stopping server...");
            server.stopServer();
        });
    }
}
