package com.github.razorplay01.geoware.geowarecommon.network.packet;

import com.github.razorplay.packet_handler.network.IPacket;
import com.github.razorplay.packet_handler.network.network_util.PacketDataSerializer;
import com.github.razorplay.packet_handler.exceptions.PacketSerializationException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ScorePacket implements IPacket {
    private int score;

    @Override
    public void read(PacketDataSerializer serializer) throws PacketSerializationException {
        this.score = serializer.readInt();
    }

    @Override
    public void write(PacketDataSerializer serializer) throws PacketSerializationException {
        serializer.writeInt(this.score);
    }


    @Override
    public String getPacketId() {
        return "ScorePacket";
    }
}
