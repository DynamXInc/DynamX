package fr.dynamx.api.network;

import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.entities.HmServerPlayerEntity;
import org.joml.Vector4f;

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
    public static final EnumPacketTarget<HmServerPlayerEntity> PLAYER = new EnumPacketTarget<>();
    /**
     * All around point target, can be only used from server side
     */
    public static final EnumPacketTarget<Vector4f> ALL_AROUND = new EnumPacketTarget<>();
    /**
     * All players tracking a specific entity, can be only used from server side
     */
    public static final EnumPacketTarget<HmEntity> ALL_TRACKING_ENTITY = new EnumPacketTarget<>();
    /**
     * All players target, can be only used from server side
     */
    public static final EnumPacketTarget<Void> ALL = new EnumPacketTarget<>();
}
