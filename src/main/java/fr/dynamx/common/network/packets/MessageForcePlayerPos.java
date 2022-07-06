package fr.dynamx.common.network.packets;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.common.entities.PhysicsEntity;
import io.netty.buffer.ByteBuf;

public class MessageForcePlayerPos extends PhysicsEntityMessage<MessageForcePlayerPos>
{
    public Vector3f rightPos;
    public Quaternion rotation = new Quaternion();
    public Vector3f linearVel = new Vector3f();
    public Vector3f rotationalVel = new Vector3f();

    public MessageForcePlayerPos() {
        super(null);
    }

    public MessageForcePlayerPos(PhysicsEntity entity, Vector3f rightPos, Quaternion rotation, Vector3f linearVel, Vector3f rotationalVel) {
        super(entity);
        this.rightPos = rightPos;
        this.rotation = rotation;
        this.linearVel = linearVel;
        this.rotationalVel = rotationalVel;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        super.fromBytes(buf);
        rightPos = new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
        rotation = new Quaternion(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());

        linearVel.set(buf.readFloat(), buf.readFloat(), buf.readFloat());
        rotationalVel.set(buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    @Override
    public int getMessageId() {
        return 5;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        super.toBytes(buf);
        buf.writeFloat(rightPos.x);
        buf.writeFloat(rightPos.y);
        buf.writeFloat(rightPos.z);

        buf.writeFloat(rotation.getX());
        buf.writeFloat(rotation.getY());
        buf.writeFloat(rotation.getZ());
        buf.writeFloat(rotation.getW());

        buf.writeFloat(linearVel.x);
        buf.writeFloat(linearVel.y);
        buf.writeFloat(linearVel.z);
        buf.writeFloat(rotationalVel.x);
        buf.writeFloat(rotationalVel.y);
        buf.writeFloat(rotationalVel.z);
    }
}
