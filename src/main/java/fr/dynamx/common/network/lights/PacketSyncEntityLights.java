package fr.dynamx.common.network.lights;

import fr.aym.acslib.utils.packetserializer.ISerializablePacket;
import fr.dynamx.common.entities.ModularPhysicsEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.AbstractLightsModule;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSyncEntityLights implements ISerializablePacket {

    private int entityId;

    public PacketSyncEntityLights() {
    }

    public PacketSyncEntityLights(int entityId) {
        this.entityId = entityId;
    }

    @Override
    public Object[] getObjectsToSave() {
        return new Object[]{
                entityId
        };
    }

    @Override
    public void populateWithSavedObjects(Object[] objects) {
        entityId = (int) objects[0];
    }

    public static class ServerHandler implements IMessageHandler<PacketSyncEntityLights, IMessage> {

        @Override
        public IMessage onMessage(PacketSyncEntityLights message, MessageContext ctx) {
            Entity entityByID = ctx.getServerHandler().player.world.getEntityByID(message.entityId);
            if(entityByID instanceof ModularPhysicsEntity){
                AbstractLightsModule lightsModule = ((ModularPhysicsEntity<?>) entityByID).getModuleByType(AbstractLightsModule.class);
                if(lightsModule instanceof AbstractLightsModule.EntityLightsModule){
                    AbstractLightsModule.EntityLightsModule module = (AbstractLightsModule.EntityLightsModule) lightsModule;
                    if(!module.isSynced) {
                        module.isSynced = true;
                        //TODO LIGHT YANIS
                        module.addLights(((PackPhysicsEntity<?, ?>) entityByID));
                    }
                }
            }
            return null;
        }
    }
}
