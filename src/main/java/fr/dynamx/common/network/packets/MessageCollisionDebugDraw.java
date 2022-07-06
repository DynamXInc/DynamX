package fr.dynamx.common.network.packets;

import fr.aym.acslib.utils.packetserializer.ISerializablePacket;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.debug.TerrainDebugData;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.Map;

public class MessageCollisionDebugDraw implements IDnxPacket, net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler<MessageCollisionDebugDraw, IMessage>, ISerializablePacket
{
    private Map<Integer, TerrainDebugData> chunkOrBlockData;
    private Map<Integer, TerrainDebugData> slopeData;

    public MessageCollisionDebugDraw() {}

    public MessageCollisionDebugDraw(Map<Integer, TerrainDebugData> chunkOrBlockData, Map<Integer, TerrainDebugData> slopeData)
    {
        this.chunkOrBlockData = chunkOrBlockData;
        this.slopeData = slopeData;
    }

    @Override
    public Object[] getObjectsToSave() {
        return new Object[] {chunkOrBlockData, slopeData};
    }

    @Override
    public void populateWithSavedObjects(Object[] objects) {
        this.chunkOrBlockData = (Map<Integer, TerrainDebugData>) objects[0];
        this.slopeData = (Map<Integer, TerrainDebugData>) objects[1];
    }

    @Override
    public IMessage onMessage(MessageCollisionDebugDraw message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
        if(DynamXDebugOptions.CHUNK_BOXES.isActive())
            DynamXDebugOptions.CHUNK_BOXES.setDataIn(message.chunkOrBlockData);
        else if(DynamXDebugOptions.BLOCK_BOXES.isActive())
            DynamXDebugOptions.BLOCK_BOXES.setDataIn(message.chunkOrBlockData);
        if(DynamXDebugOptions.SLOPE_BOXES.isActive())
            DynamXDebugOptions.SLOPE_BOXES.setDataIn(message.slopeData);});
        return null;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}