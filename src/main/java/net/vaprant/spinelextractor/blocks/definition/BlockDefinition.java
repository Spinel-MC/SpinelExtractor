package net.vaprant.spinelextractor.blocks.definition;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public record BlockDefinition(
        int id,
        String name,
        @SerializedName("translation_key") String translationKey,
        @SerializedName("default_state_id") int defaultStateId,
        @SerializedName("block_entity_type") String blockEntityType,
        Map<String, List<String>> properties,
        Map<String, BlockStateDefinition> states,
        float hardness,
        float friction,
        @SerializedName("requires_tool") boolean requiresTool,
        BlockFlags flags
) {
}
