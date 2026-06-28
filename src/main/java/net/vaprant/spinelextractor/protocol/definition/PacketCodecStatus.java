package net.vaprant.spinelextractor.protocol.definition;

import com.google.gson.annotations.SerializedName;

public enum PacketCodecStatus {
    @SerializedName("exact")
    EXACT,
    @SerializedName("unresolved")
    UNRESOLVED
}
