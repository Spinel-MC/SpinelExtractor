package net.vaprant.spinelextractor;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.vaprant.spinelextractor.extractors.BiomeExtractor;
import net.vaprant.spinelextractor.extractors.BlockExtractor;
import net.vaprant.spinelextractor.extractors.DimensionTypeExtractor;
import net.vaprant.spinelextractor.extractors.DamageTypeExtractor;
import net.vaprant.spinelextractor.extractors.DynamicRegistryAssetExtractor;
import net.vaprant.spinelextractor.extractors.EntityTypeExtractor;
import net.vaprant.spinelextractor.extractors.EnchantmentExtractor;
import net.vaprant.spinelextractor.extractors.ItemExtractor;
import net.vaprant.spinelextractor.extractors.MobEffectExtractor;
import net.vaprant.spinelextractor.extractors.PacketExtractor;
import net.vaprant.spinelextractor.extractors.ParticleTypeExtractor;
import net.vaprant.spinelextractor.extractors.SoundEventExtractor;
import net.vaprant.spinelextractor.extractors.StaticProtocolRegistryExtractor;
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
            ParticleTypeExtractor.extract();
            BlockExtractor.extract();
            ItemExtractor.extract(server);
            MobEffectExtractor.extract(server);
            EntityTypeExtractor.extract();
            SoundEventExtractor.extract();
            BiomeExtractor.extract(server);
            DimensionTypeExtractor.extract(server);
            DamageTypeExtractor.extract(server);
            EnchantmentExtractor.extract(server);
            DynamicRegistryAssetExtractor.extract(server);
            StaticProtocolRegistryExtractor.extract();
            StaticRegistryTagExtractor.extract();
            LOGGER.info("Extraction complete, stopping server...");
            server.halt(false);
        });
    }
}
