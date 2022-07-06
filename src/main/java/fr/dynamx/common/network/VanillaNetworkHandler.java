package fr.dynamx.common.network;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.network.IDnxNetworkHandler;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

import javax.annotation.Nullable;

public class VanillaNetworkHandler implements IDnxNetworkHandler {
    public final SimpleNetworkWrapper HANDLER = NetworkRegistry.INSTANCE.newSimpleChannel(DynamXConstants.ID);

    @Override
    public <T> void sendPacket(IDnxPacket packet, EnumPacketTarget<T> targetType, @Nullable T target) {
        if (EnumPacketTarget.SERVER == targetType) {
            HANDLER.sendToServer(packet);
        } else {
            sendPacketServer(packet, targetType, target);
        }
    }

    private <T> void sendPacketServer(IDnxPacket packet, EnumPacketTarget<T> targetType, @Nullable T target) {
        if (EnumPacketTarget.PLAYER == targetType) {
            HANDLER.sendTo(packet, (EntityPlayerMP) target);
        } else if (EnumPacketTarget.ALL_AROUND == targetType) {
            HANDLER.sendToAllAround(packet, (NetworkRegistry.TargetPoint) target);
        } else if (EnumPacketTarget.ALL_TRACKING_ENTITY == targetType) {
            HANDLER.sendToAllTracking(packet, (Entity) target);
        } else if (EnumPacketTarget.ALL == targetType) {
            HANDLER.sendToAll(packet);
        }
    }

    @Override
    public EnumNetworkType getType() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public void stop() {
    }

    public SimpleNetworkWrapper getChannel() {
        return HANDLER;
    }
}
