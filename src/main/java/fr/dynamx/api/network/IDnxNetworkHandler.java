package fr.dynamx.api.network;

import javax.annotation.Nullable;

/**
 * Dispatch packets over a specific network type
 */
public interface IDnxNetworkHandler {
    /**
     * Sends a packet
     *
     * @param targetType The target type
     * @param target     The target, nullable
     */
    <T> void sendPacket(IDnxPacket packet, EnumPacketTarget<T> targetType, @Nullable T target);

    /**
     * @return Get the managed network type
     */
    EnumNetworkType getType();

    /**
     * Tries to start this network
     */
    boolean start();

    /**
     * Stops this network
     */
    void stop();

    /**
     * @return True is the connection is established between the two hosts <br>
     * Only used on client side of udp network
     */
    default boolean isAuthenticated() {
        return true;
    }
}
