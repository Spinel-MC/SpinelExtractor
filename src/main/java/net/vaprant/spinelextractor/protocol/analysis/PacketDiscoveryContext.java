package net.vaprant.spinelextractor.protocol.analysis;

import net.minecraft.network.protocol.PacketType;
import java.util.Map;

public record PacketDiscoveryContext(
    Object serializer,
    PacketType<?> type,
    Map<Integer, Class<?>> classMap,
    int index
) {}
