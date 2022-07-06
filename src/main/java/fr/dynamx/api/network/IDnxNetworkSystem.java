package fr.dynamx.api.network;

import fr.dynamx.common.network.VanillaNetworkHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nullable;

/**
 *  Responsible to dispatch vanilla and udp packets and manages udp network
 */
public interface IDnxNetworkSystem
{
    /**
     * Sends the packet to the server, using preferred network of the packet
     */
    default void sendToServer(IDnxPacket packet)
    {
        throw new UnsupportedOperationException("Unsupported on this side !");
    }

    /**
     * Sends the packet to the targeted client(s), using preferred network of the packet
     *
     * @param targetType The client(s) type
     */
    default void sendToClient(IDnxPacket packet, EnumPacketTarget<Void> targetType)
    {
        sendToClient(packet, targetType, null);
    }

    /**
     * Sends the packet to the targeted clients, using preferred network of the packet <br>
     * <strong>Only use this from the server thread because fml packets code isn't multi-threaded. Use 'sendToClientFromOtherThread' instead.</strong>
     *
     * @param targetType The client(s) type
     * @param target The client(s)
     */
    default <T> void sendToClient(IDnxPacket packet, EnumPacketTarget<T> targetType, @Nullable T target)
    {
        throw new UnsupportedOperationException("Unsupported on this side !");
    }

    /**
     * Sends the packet to the targeted clients, using preferred network of the packet <br>
     * <strong>Unlike 'sendToClient', this will send the send task to the server thread, so use this if you are sending the packet from another thread.</strong>
     *
     * @param targetType The client(s) type
     * @param target The client(s)
     */
    default <T> void sendToClientFromOtherThread(IDnxPacket packet, EnumPacketTarget<T> targetType, @Nullable T target)
    {
        FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(() -> {
            sendToClient(packet, targetType, target);
        });
    }

    /**
     * @return The {@link VanillaNetworkHandler}, permits to bypass any udp handling
     */
    VanillaNetworkHandler getVanillaNetwork();

    boolean isConnected();

    /**
     * Called to start udp server, on server side
     */
    void startNetwork();

    /**
     * Called to stop udp server
     */
    void stopNetwork();

    /**
     * @return The preferred network, can be vanilla or udp
     */
    IDnxNetworkHandler getQuickNetwork();
}
