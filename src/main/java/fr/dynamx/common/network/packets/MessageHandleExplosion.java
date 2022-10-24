package fr.dynamx.common.network.packets;

import com.jme3.math.Vector3f;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;


public class MessageHandleExplosion implements IDnxPacket, net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler<MessageHandleExplosion, IMessage>{

    private Vector3f explosionPosition;
    private List<Integer> entityIdList = new ArrayList<>();

    public MessageHandleExplosion() {
    }

    public MessageHandleExplosion(Vector3f explosionPosition, List<Entity> entityList) {
        this.explosionPosition = explosionPosition;
        for (int i = 0; i < entityList.size(); i++) {
            entityIdList.add(entityList.get(i).getEntityId());
        }
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        explosionPosition = DynamXUtils.readVector3f(buf);
        entityIdList = new ArrayList<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            entityIdList.add(buf.readInt());
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        DynamXUtils.writeVector3f(buf, explosionPosition);
        buf.writeInt(entityIdList.size());
        for (int i = 0; i < entityIdList.size(); i++) {
            buf.writeInt(entityIdList.get(i));
        }
    }

        @Override
        public IMessage onMessage(MessageHandleExplosion message, MessageContext ctx) {
            message.entityIdList.forEach(integer -> {
                Entity entityByID = Minecraft.getMinecraft().world.getEntityByID(integer);
                if(entityByID instanceof PhysicsEntity) {
                    DynamXPhysicsHelper.createExplosion((PhysicsEntity<?>) entityByID, message.explosionPosition, 10.0D);
                }
            });
            return null;
        }
}
