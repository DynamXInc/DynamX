package fr.dynamx.api.network;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;

/**
 * Fake enum, enumerating all possible targets for {@link IDnxPacket}
 *
 * @param <O> Type of additional target information
 */
public class EnumPacketTarget<O> {
    /**
     * Server target, can be only used from client side
     */
    public static final EnumPacketTarget<Void> SERVER = new EnumPacketTarget<>();
    /**
     * Specific player target, can be only used from server side
     */
    public static final EnumPacketTarget<EntityPlayerMP> PLAYER = new EnumPacketTarget<>();
    /**
     * All around point target, can be only used from server side
     */
    public static final EnumPacketTarget<NetworkRegistry.TargetPoint> ALL_AROUND = new EnumPacketTarget<>();
    /**
     * All players tracking a specific entity, can be only used from server side
     */
    public static final EnumPacketTarget<Entity> ALL_TRACKING_ENTITY = new EnumPacketTarget<>();
    /**
     * All players target, can be only used from server side
     */
    public static final EnumPacketTarget<Void> ALL = new EnumPacketTarget<>();
}
