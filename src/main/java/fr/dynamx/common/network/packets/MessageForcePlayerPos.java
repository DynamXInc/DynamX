package fr.dynamx.common.network.packets;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.common.entities.PhysicsEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static fr.dynamx.common.DynamXMain.log;

public class MessageForcePlayerPos extends PhysicsEntityMessage<MessageForcePlayerPos> {
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
    @SideOnly(Side.CLIENT)
    protected void processMessageClient(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, EntityPlayer player) {
        MessageForcePlayerPos p = (MessageForcePlayerPos) message;
        if (entity.physicsHandler != null) {
            entity.physicsPosition.set(p.rightPos);
            //entity.physicEntity.getPosition().set(p.rightPos);
            entity.physicsRotation.set(p.rotation);
            //entity.physicEntity.getRotation().set(p.rotation);
            entity.physicsHandler.updatePhysicsState(p.rightPos, p.rotation, p.linearVel, p.rotationalVel);
            log.info("Entity " + entity + " has been resynced");

            Minecraft.getMinecraft().ingameGUI.setOverlayMessage("Resynchronisation...", true);
        } else
            log.fatal("Cannot resync entity " + entity + " : not physics found !");
    }

    @Override
    protected void processMessageServer(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, EntityPlayer player) {
        throw new IllegalStateException();
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
