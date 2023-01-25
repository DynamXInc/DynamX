package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.TrailerAttachModule;
import fr.dynamx.common.entities.vehicles.CarEntity;
import fr.dynamx.common.entities.vehicles.TrailerEntity;
import fr.dynamx.utils.DynamXUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.List;

public class MessageAttachTrailer implements IDnxPacket, IMessageHandler<MessageAttachTrailer, IMessage> {


    public MessageAttachTrailer() {
    }

    @Override
    public void fromBytes(ByteBuf byteBuf) {}

    @Override
    public void toBytes(ByteBuf byteBuf) {}

    @Override
    public IMessage onMessage(MessageAttachTrailer message, MessageContext ctx) {

        EntityPlayerMP player = ctx.getServerHandler().player;
        if (player.getRidingEntity() instanceof BaseVehicleEntity) {
            CarEntity carEntity = (CarEntity) player.getRidingEntity();
            TrailerAttachModule trailerAttachModule = (TrailerAttachModule) carEntity.getModuleByType(TrailerAttachModule.class);
            if (trailerAttachModule != null) {
                if (trailerAttachModule.getAttachPoint() != null) {
                    float x = carEntity.getPosition().getX() + trailerAttachModule.getAttachPoint().x;
                    float y = carEntity.getPosition().getY() + trailerAttachModule.getAttachPoint().y;
                    float z = carEntity.getPosition().getZ() + trailerAttachModule.getAttachPoint().z;
                    TrailerEntity trailer = null;
                    List<TrailerEntity> list = player.world.getEntitiesWithinAABB(TrailerEntity.class, carEntity.getEntityBoundingBox().grow(20));
                    for (TrailerEntity<?> trailerEntity : list) {
                        if (trailer != null) {
                            if (trailerEntity.getDistance(x, y, z) < trailer.getDistance(x, y, z)) {
                                trailer = trailerEntity;
                            }
                        } else {
                            trailer = trailerEntity;
                        }
                    }
                    if (trailer == null) {
                        return null;
                    }
                    DynamXUtils.attachTrailer(player, carEntity, trailer);

                }
            }
        }
        return null;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
