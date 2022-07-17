package fr.dynamx.common.network.packets;

import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.physics.joints.EntityJoint;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    public int getMessageId() {
        return 6;
    }

    public List<EntityJoint.CachedJoint> getJointList() {
        return jointList;
    }
}
