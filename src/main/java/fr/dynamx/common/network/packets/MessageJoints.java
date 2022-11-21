package fr.dynamx.common.network.packets;

import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.physics.joints.EntityJoint;
import fr.dynamx.common.physics.joints.EntityJointsHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static fr.dynamx.common.DynamXMain.log;

public class MessageJoints extends PhysicsEntityMessage<MessageJoints> {
    private List<EntityJoint.CachedJoint> jointList;

    public MessageJoints() {
        super(null);
    }

    public MessageJoints(PhysicsEntity<?> entity, List<EntityJoint.CachedJoint> jointList) {
        super(entity);
        this.jointList = jointList;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        super.toBytes(buf);
        buf.writeInt(jointList.size());
        for (EntityJoint.CachedJoint g : jointList) {
            ByteBufUtils.writeUTF8String(buf, g.getId().toString());
            buf.writeByte(g.getJid());
            ByteBufUtils.writeUTF8String(buf, g.getType().toString());
            buf.writeBoolean(g.isJointOwner());
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        super.fromBytes(buf);
        jointList = new ArrayList<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            EntityJoint.CachedJoint g = new EntityJoint.CachedJoint(UUID.fromString(ByteBufUtils.readUTF8String(buf)), buf.readByte(),
                    new ResourceLocation(ByteBufUtils.readUTF8String(buf)), buf.readBoolean());
            jointList.add(g);
        }
    }

    @Override
    protected void processMessageClient(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, EntityPlayer player) {
        if (entity.getJointsHandler() != null) {
            List<EntityJoint.CachedJoint> joints = ((MessageJoints) message).getJointList();
            EntityJointsHandler handler = ((EntityJointsHandler) entity.getJointsHandler());
            Collection<EntityJoint<?>> curJoints = handler.getJoints();
            curJoints.removeIf(j -> { //done in client thread
                EntityJoint.CachedJoint found = null;
                for (EntityJoint.CachedJoint g : joints) {
                    if (g.getId().equals(j.getOtherEntity(entity).getPersistentID())) {
                        found = g;
                        break;
                    }
                }
                if (found != null) {
                    joints.remove(found); //keep it
                    return false;
                } else {
                    handler.onRemoveJoint(j);
                    return true;
                }
            });
            for (EntityJoint.CachedJoint g : joints) {
                if (g.isJointOwner()) //Only allow the owner to re-create the joint on client side
                    handler.onNewJointSynchronized(g);
            }
        } else
            log.error("Cannot sync joints of " + entity + " : joint handler is null !");
    }

    @Override
    protected void processMessageServer(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, EntityPlayer player) {
        throw new IllegalStateException();
    }

    public List<EntityJoint.CachedJoint> getJointList() {
        return jointList;
    }
}
