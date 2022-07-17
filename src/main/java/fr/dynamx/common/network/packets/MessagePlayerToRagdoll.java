package fr.dynamx.common.network.packets;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.RagdollEntity;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.optimization.Vector3fPool;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessagePlayerToRagdoll implements IDnxPacket, IMessageHandler<MessagePlayerToRagdoll, IMessage> {

    private Vector3f velocity;

    public MessagePlayerToRagdoll() {
    }

    public MessagePlayerToRagdoll(Vector3f velocity) {
        this.velocity = velocity;
    }

    @Override
    public void fromBytes(ByteBuf byteBuf) {
        velocity = DynamXUtils.readVector3f(byteBuf);
    }

    @Override
    public void toBytes(ByteBuf byteBuf) {
        DynamXUtils.writeVector3f(byteBuf, velocity);
    }

    @Override
    public IMessage onMessage(MessagePlayerToRagdoll message, MessageContext ctx) {
        Vector3fPool.openPool();
        EntityPlayerMP player = ctx.getServerHandler().player;
        RagdollEntity ragdollEntity = new RagdollEntity(player.world, DynamXUtils.toVector3f(player.getPositionVector().add(0, player.getDefaultEyeHeight(), 0)),
                player.rotationYaw + 180, player.getName(), (short) -1, player);
        ragdollEntity.setPhysicsInitCallback((a, b) -> {
            if (b != null && b.getCollisionObject() != null) {
                ((PhysicsRigidBody) b.getCollisionObject()).setLinearVelocity(message.velocity);
            }
        });
        player.setInvisible(true);
        player.world.spawnEntity(ragdollEntity);
        DynamXContext.getPlayerToCollision().get(player).ragdollEntity = ragdollEntity;
        DynamXContext.getPlayerToCollision().get(player).removeFromWorld(false);
        Vector3fPool.closePool();
        return null;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
