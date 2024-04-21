package fr.dynamx.common.network;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.aym.acslib.utils.packetserializer.PacketDataSerializer;
import fr.aym.acslib.utils.packetserializer.PacketSerializer;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxNetworkSystem;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.sync.MessagePacksHashs;
import fr.dynamx.common.network.lights.PacketSyncEntityLights;
import fr.dynamx.common.network.lights.PacketSyncItemLight;
import fr.dynamx.common.network.lights.PacketSyncPartLights;
import fr.dynamx.common.network.packets.*;
import fr.dynamx.common.network.sync.MessageMultiPhysicsEntitySync;
import fr.dynamx.common.network.sync.MessagePhysicsEntitySync;
import fr.dynamx.common.network.sync.MessageSeatsSync;
import fr.dynamx.common.network.udp.auth.MessageDynamXUdpSettings;
import fr.dynamx.server.network.DynamXServerNetworkSystem;
import fr.dynamx.utils.DynamXConfig;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * The DynamX network holding packets registry <br>
 * The network instance is located in {@link DynamXContext}
 */
public class DynamXNetwork {
    public static final BiMap<Integer, Class<? extends IDnxPacket>> UDP_PACKETS = HashBiMap.create();

    private static int id;
    private static int udpId;

    /**
     * Creates a new {@link IDnxNetworkSystem} for this side and registers all packets <br>
     * The network instance is located in {@link DynamXContext}
     */
    public static IDnxNetworkSystem init(Side side) {
        IDnxNetworkSystem network = side.isServer() ? new DynamXServerNetworkSystem(DynamXConfig.useUdp ? EnumNetworkType.DYNAMX_UDP : EnumNetworkType.VANILLA_TCP) : new DynamXClientNetworkSystem(DynamXConfig.useUdp ? EnumNetworkType.DYNAMX_UDP : EnumNetworkType.VANILLA_TCP);
        SimpleNetworkWrapper channel = network.getVanillaNetwork().getChannel();

        //Udp packets

        //Bi-way
        registerMessageWithUDP(channel, MessagePhysicsEntitySync.Handler.class, MessagePhysicsEntitySync.class, Side.CLIENT, Side.SERVER);
        registerMessageWithUDP(channel, MessageMultiPhysicsEntitySync.class, MessageMultiPhysicsEntitySync.class, Side.CLIENT, Side.SERVER);
        registerMessageWithUDP(channel, MessagePing.class, MessagePing.class, Side.SERVER, Side.CLIENT);
        registerMessageWithUDP(channel, MessageWalkingPlayer.class, MessageWalkingPlayer.class, Side.CLIENT, Side.SERVER);

        //To server
        registerMessageWithUDP(channel, MessageRequestFullEntitySync.class, MessageRequestFullEntitySync.class, Side.SERVER);

        //Standard packets

        //Bi-way
        registerMessage(channel, MessageQueryChunks.Handler.class, MessageQueryChunks.class, Side.CLIENT, Side.SERVER);
        registerMessage(channel, MessageOpenDebugGui.Handler.class, MessageOpenDebugGui.class, Side.SERVER, Side.CLIENT);

        //To client
        registerMessage(channel, MessageDynamXUdpSettings.class, MessageDynamXUdpSettings.class, Side.CLIENT);
        registerMessage(channel, MessageSyncConfig.class, MessageSyncConfig.class, Side.CLIENT);
        registerMessage(channel, MessagePacksHashs.HandlerClient.class, MessagePacksHashs.class, Side.CLIENT);
        registerMessage(channel, MessageSeatsSync.class, MessageSeatsSync.class, Side.CLIENT);
        registerMessage(channel, MessageUpdateChunk.class, MessageUpdateChunk.class, Side.CLIENT);
        registerMessage(channel, MessageChunkData.Handler.class, MessageChunkData.class, Side.CLIENT);
        registerMessage(channel, MessageForcePlayerPos.class, MessageForcePlayerPos.class, Side.CLIENT);
        registerMessage(channel, MessageJoints.class, MessageJoints.class, Side.CLIENT);
        registerMessage(channel, MessageSyncPlayerPicking.class, MessageSyncPlayerPicking.class, Side.CLIENT);
        registerMessage(channel, MessageSwitchAutoSlopesMode.Handler.class, MessageSwitchAutoSlopesMode.class, Side.CLIENT);
        registerMessage(channel, MessageCollisionDebugDraw.class, MessageCollisionDebugDraw.class, Side.CLIENT);
        registerMessage(channel, MessageCollisionDebugDraw.class, MessageCollisionDebugDraw.class, Side.CLIENT);
        registerMessage(channel, MessageHandleExplosion.class, MessageHandleExplosion.class, Side.CLIENT);
        //BetterLights
        registerMessage(channel, PacketSyncPartLights.ClientHandler.class, PacketSyncPartLights.class, Side.CLIENT);
        registerMessage(channel, PacketSyncItemLight.ClientHandler.class, PacketSyncItemLight.class, Side.CLIENT);

        //To server
        registerMessage(channel, MessagePacksHashs.HandlerServer.class, MessagePacksHashs.class, Side.SERVER);
        registerMessage(channel, MessageEntityInteract.class, MessageEntityInteract.class, Side.SERVER);
        registerMessage(channel, MessagePickObject.class, MessagePickObject.class, Side.SERVER);
        registerMessage(channel, MessageChangeDoorState.class, MessageChangeDoorState.class, Side.SERVER);
        registerMessage(channel, MessageSyncBlockCustomization.class, MessageSyncBlockCustomization.class, Side.SERVER);
        registerMessage(channel, MessageSlopesConfigGui.class, MessageSlopesConfigGui.class, Side.SERVER);
        registerMessage(channel, MessageDebugRequest.class, MessageDebugRequest.class, Side.SERVER);
        registerMessage(channel, MessageAttachTrailer.class, MessageAttachTrailer.class, Side.SERVER);
        //BetterLights
        registerMessage(channel, PacketSyncEntityLights.ServerHandler.class, PacketSyncEntityLights.class, Side.SERVER);

        //Packet serializers
        PacketSerializer.addCustomSerializer(new PacketDataSerializer<Vector3f>() {
            @Override
            public Class<Vector3f> objectType() {
                return Vector3f.class;
            }

            @Override
            public void serialize(ByteBuf to, Vector3f vector3f) {
                to.writeFloat(vector3f.x);
                to.writeFloat(vector3f.y);
                to.writeFloat(vector3f.z);
            }

            @Override
            public Vector3f unserialize(ByteBuf byteBuf) {
                return new Vector3f(byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat());
            }
        });
        PacketSerializer.addCustomSerializer(new PacketDataSerializer<Quaternion>() {
            @Override
            public Class<Quaternion> objectType() {
                return Quaternion.class;
            }

            @Override
            public void serialize(ByteBuf to, Quaternion quaternion) {
                to.writeFloat(quaternion.getX());
                to.writeFloat(quaternion.getY());
                to.writeFloat(quaternion.getZ());
                to.writeFloat(quaternion.getW());
            }

            @Override
            public Quaternion unserialize(ByteBuf byteBuf) {
                return new Quaternion(byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat());
            }
        });

        return network;
    }

    private static <REQ extends IMessage, REPLY extends IMessage> void registerMessage(SimpleNetworkWrapper network, Class<? extends IMessageHandler<REQ, REPLY>> messageHandler, Class<REQ> message, Side... sides) {
        for (Side side : sides) {
            network.registerMessage(messageHandler, message, id, side);
            id++;
        }
    }

    private static <REQ extends IDnxPacket, REPLY extends IMessage> void registerMessageWithUDP(SimpleNetworkWrapper network, Class<? extends IMessageHandler<REQ, REPLY>> vanillaMessageHandler, Class<REQ> message, Side... sides) {
        if (udpId > 245)
            throw new RuntimeException("There is too many packets, limit is 245 for the UDP !");
        for (Side side : sides) {
            network.registerMessage(vanillaMessageHandler, message, id, side);
            id++;
        }
        UDP_PACKETS.put(udpId, message);
        udpId++;
    }

    public static IDnxPacket getUdpPacketById(int id) {
        try {
            return UDP_PACKETS.get(id).newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int getUdpMessageId(IDnxPacket message) {
        return UDP_PACKETS.inverse().get(message.getClass());
    }
}