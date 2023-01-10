package fr.dynamx.common.network.packets;

import com.jme3.math.Vector3f;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.physics.player.WalkingOnPlayerController;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MessageWalkingPlayer extends PhysicsEntityMessage<MessageWalkingPlayer> {
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
    @SideOnly(Side.CLIENT)
    protected void processMessageClient(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, EntityPlayer player) {
        MessageWalkingPlayer p = (MessageWalkingPlayer) message;
        Entity e = entity.world.getEntityByID(p.playerId);
        if (e instanceof EntityPlayer && e != Minecraft.getMinecraft().player) {
            if (p.face == -1) {
                entity.walkingOnPlayers.remove(e);
                DynamXContext.getWalkingPlayers().remove(e);
            } else {
                entity.walkingOnPlayers.put((EntityPlayer) e, new WalkingOnPlayerController((EntityPlayer) e, entity, EnumFacing.byIndex(p.face), p.offset));
                DynamXContext.getWalkingPlayers().put((EntityPlayer) e, entity);
            }
        }
    }

    @Override
    protected void processMessageServer(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, EntityPlayer player) {
        MessageWalkingPlayer p = (MessageWalkingPlayer) message;
        Entity e = entity.world.getEntityByID(p.playerId);
        if (e instanceof EntityPlayer) {
            if (p.face == -1) {
                entity.walkingOnPlayers.remove(e);
                DynamXContext.getWalkingPlayers().remove(e);
            } else {
                entity.walkingOnPlayers.put((EntityPlayer) e, new WalkingOnPlayerController((EntityPlayer) e, entity, EnumFacing.byIndex(p.face), p.offset));
                DynamXContext.getWalkingPlayers().put((EntityPlayer) e, entity);
            }
        }
        DynamXContext.getNetwork().sendToClientFromOtherThread(new MessageWalkingPlayer(entity, p.playerId, p.offset, p.face), EnumPacketTarget.ALL_TRACKING_ENTITY, entity);
    }
}
