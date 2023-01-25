package fr.dynamx.common.network.packets;

import com.jme3.math.Vector3f;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.TrailerAttachModule;
import fr.dynamx.common.entities.vehicles.CarEntity;
import fr.dynamx.common.entities.vehicles.TrailerEntity;
import fr.dynamx.common.physics.joints.EntityJoint;
import fr.dynamx.common.physics.joints.EntityJointsHandler;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.Collection;
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

        if(player.getRidingEntity() instanceof BaseVehicleEntity) {
            CarEntity carEntity = (CarEntity) player.getRidingEntity();

            TrailerAttachModule trailerAttachModule = (TrailerAttachModule) carEntity.getModuleByType(TrailerAttachModule.class);
            if (trailerAttachModule != null) {
                if (trailerAttachModule.getAttachPoint() != null) {
                    float x = carEntity.getPosition().getX() + trailerAttachModule.getAttachPoint().x;
                    float y = carEntity.getPosition().getY() + trailerAttachModule.getAttachPoint().y;
                    float z = carEntity.getPosition().getZ() + trailerAttachModule.getAttachPoint().z;
                    int range = 20;
                    TrailerEntity trailer = null;
                    List<TrailerEntity> list = player.world.getEntitiesWithinAABB(TrailerEntity.class, carEntity.getEntityBoundingBox().grow(range));
                    for (TrailerEntity<?> trailerEntity : list) {
                        if (trailer != null) {
                            if (trailerEntity.getDistance(x, y, z) < trailer.getDistance(x, y, z)) {
                                trailer = trailerEntity;
                            }
                        } else {
                            trailer = trailerEntity;
                        }
                    }

                    if (trailer != null) {
                        Vector3fPool.openPool();
                        Vector3f p1r = DynamXGeometry.rotateVectorByQuaternion(trailerAttachModule.getAttachPoint(), carEntity.physicsRotation);
                        Vector3f p2r = DynamXGeometry.rotateVectorByQuaternion(((TrailerAttachModule) trailer.getModuleByType(TrailerAttachModule.class)).getAttachPoint(), trailer.physicsRotation);
                        if (p1r.addLocal(carEntity.physicsPosition).subtract(p2r.addLocal(trailer.physicsPosition)).lengthSquared() < 6.0F) {
                            if (carEntity.getJointsHandler() != null) {
                                EntityJointsHandler handler = (EntityJointsHandler) carEntity.getJointsHandler();
                                Collection<EntityJoint<?>> curJoints = handler.getJoints();
                                TrailerEntity trailerIsAttached = null;
                                for (EntityJoint<?> joint : curJoints) {
                                    if (joint.getEntity2() instanceof TrailerEntity) {
                                        trailerIsAttached = (TrailerEntity) joint.getEntity2();
                                        break;
                                    }
                                }
                                if (trailerIsAttached == null) {
                                    if (TrailerAttachModule.HANDLER.createJoint(carEntity, trailer, (byte) 0)) {
                                        player.sendMessage(new TextComponentString(TextFormatting.GREEN + "Attached " + ((ModularVehicleInfo) trailer.getPackInfo()).getName() + " to " + ((ModularVehicleInfo) carEntity.getPackInfo()).getName()));
                                    } else {
                                        player.sendMessage(new TextComponentString(TextFormatting.RED + "Cannot attach " + ((ModularVehicleInfo) trailer.getPackInfo()).getName() + " to " + ((ModularVehicleInfo) carEntity.getPackInfo()).getName()));
                                    }
                                } else {
                                    carEntity.getJointsHandler().removeJointWith(trailerIsAttached, TrailerAttachModule.JOINT_NAME, (byte) 0);
                                    player.sendMessage(new TextComponentString(TextFormatting.RED + "The joint has been removed"));
                                }
                            }
                        } else {
                            player.sendMessage(new TextComponentString(TextFormatting.RED + "The joint points are too far away !"));
                        }
                        Vector3fPool.closePool();
                    }
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
