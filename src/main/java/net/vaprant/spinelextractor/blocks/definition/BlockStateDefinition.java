package net.vaprant.spinelextractor.blocks.definition;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public record BlockStateDefinition(
        int id,
        Map<String, String> properties,
        @SerializedName("light_emission") int lightEmission,
        @SerializedName("light_block") int lightBlock,
        @SerializedName("propagates_skylight_down") boolean propagatesSkylightDown,
        @SerializedName("uses_shape_for_light_occlusion") boolean usesShapeForLightOcclusion,
        @SerializedName("collision_shape") int collisionShape,
        @SerializedName("occlusion_shape") int occlusionShape,
        @SerializedName("face_occlusion_shapes") Map<String, Integer> faceOcclusionShapes
) {
}
