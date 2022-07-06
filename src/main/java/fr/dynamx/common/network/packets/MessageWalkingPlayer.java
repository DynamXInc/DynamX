package fr.dynamx.common.network.packets;

import com.jme3.math.Vector3f;
import fr.dynamx.common.entities.PhysicsEntity;
import io.netty.buffer.ByteBuf;

public class MessageWalkingPlayer extends PhysicsEntityMessage<MessageWalkingPlayer>
{
    public int playerId;
    public Vector3f offset;
    public byte face;

    public MessageWalkingPlayer() {
        super(null);
    }

    public MessageWalkingPlayer(PhysicsEntity<?> entity, int playerId, Vector3f offset, byte face) {
        super(entity);
        this.playerId = playerId;
        this.offset = offset;
        this.face = face;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        super.toBytes(buf);
        buf.writeInt(playerId);
        buf.writeFloat(offset.x);
        buf.writeFloat(offset.y);
        buf.writeFloat(offset.z);
        buf.writeByte(face);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        super.fromBytes(buf);
        playerId = buf.readInt();
        offset = new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
        face = buf.readByte();
    }

    @Override
    public int getMessageId() {
        return 4;
    }
}
