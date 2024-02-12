package fr.dynamx.common.network.lights;

import dz.betterlights.BetterLightsMod;
import dz.betterlights.dynamx.LightCasterPartSync;
import dz.betterlights.lighting.lightcasters.LightCaster;
import dz.betterlights.network.EnumPacketType;
import fr.aym.acslib.utils.packetserializer.ISerializablePacket;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.AbstractLightsModule;
import fr.dynamx.common.entities.modules.ILightContainer;
import fr.dynamx.common.items.DynamXItem;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class PacketSyncPartLights implements ISerializablePacket {

    private LightCasterPartSync lightCaster;
    private EnumPacketType actionType;


    public PacketSyncPartLights() {
    }

    public PacketSyncPartLights(LightCasterPartSync lightCaster, EnumPacketType actionType) {
        this(actionType);
        this.lightCaster = lightCaster;
    }

    public PacketSyncPartLights(EnumPacketType actionType) {
        this.actionType = actionType;
    }

    @Override
    public Object[] getObjectsToSave() {
        if (lightCaster == null) {
            return new Object[]{
                    actionType
            };
        }
        return new Object[]{
                actionType,
                lightCaster,
        };
    }

    @Override
    public void populateWithSavedObjects(Object[] objects) {
        actionType = (EnumPacketType) objects[0];
        if (objects.length == 2) {
            lightCaster = (LightCasterPartSync) objects[1];
        }
    }

    public static class ClientHandler implements IMessageHandler<PacketSyncPartLights, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncPartLights message, MessageContext ctx) {
            Minecraft mc = Minecraft.getMinecraft();
            LightCasterPartSync lightCaster = message.lightCaster;
            if (lightCaster == null) {
                return null;
            }
            ILightContainer lightContainer = getLightContainer(mc, message);
            switch (message.actionType) {
                case ADD:
                    mc.addScheduledTask(() -> {
                        if (mc.player == null) {
                            return;
                        }
                        //BetterLights
                        for (LightCaster caster : lightCaster.lightCasters.values()) {
                            BetterLightsMod.getLightManager().addLightCaster(caster, false);
                            caster.tmpLightCasterPartSync = lightCaster;
                        }
                        //End of BetterLights
                        if (lightContainer != null)
                            lightContainer.getLightCastersSync().put(lightCaster.ownerId, lightCaster);
                    });

                    break;
                case REMOVE:
                    if (lightContainer == null) {
                        Set<UUID> lightCasterIds = lightCaster.lightCasters.values().stream()
                                .map(LightCaster::getId)
                                .collect(Collectors.toSet());

                        //BetterLights
                        BetterLightsMod.getLightManager().getLightsInWorld().stream()
                                .filter(lightCaster1 -> lightCasterIds.contains(lightCaster1.getId()))
                                .forEach(lightToRemove -> BetterLightsMod.getLightManager().removeLightCaster(lightToRemove, false));
                        //End of BetterLights
                    } else {
                        //BetterLights
                        for (LightCaster lightCaster11 : lightContainer.getLightCastersSync().get(lightCaster.ownerId).lightCasters.values()) {
                            BetterLightsMod.getLightManager().removeLightCaster(lightCaster11, false);
                        }
                        //End of BetterLights
                    }
                    break;
                case REMOVE_ALL:
                    if (lightContainer != null) {
                        //BetterLights
                        for (LightCasterPartSync value : lightContainer.getLightCastersSync().values()) {
                            for (LightCaster lightCaster1 : value.lightCasters.values()) {
                                BetterLightsMod.getLightManager().removeLightCaster(lightCaster1, false);
                            }
                        }
                        //End of BetterLights
                        lightContainer.getLightCastersSync().clear();
                    }
                    break;
                case UPDATE:
                    if (lightContainer == null) {
                        //BetterLights
                        Set<UUID> lightCasterIds = lightCaster.lightCasters.values().stream()
                                .map(LightCaster::getId)
                                .collect(Collectors.toSet());

                        BetterLightsMod.getLightManager().getLightsInWorld().stream()
                                .filter(lightCaster1 -> lightCasterIds.contains(lightCaster1.getId()))
                                .forEach(lightCaster1 -> {
                                    lightCaster1.setEnabled(lightCaster.isEnabled);
                                });
                        //End of BetterLights
                    } else {

                        lightContainer.getLightCastersSync().get(lightCaster.ownerId).isEnabled = lightCaster.isEnabled;
                        //BetterLights
                        for (LightCaster caster : lightContainer.getLightCastersSync().get(lightCaster.ownerId).lightCasters.values()) {
                            caster.setEnabled(lightCaster.isEnabled);
                        }
                        //End of BetterLights
                    }
                    break;
            }
            return null;
        }

        private ILightContainer getLightContainer(Minecraft mc, PacketSyncPartLights message) {
            LightCasterPartSync lightCaster = message.lightCaster;
            if (lightCaster == null) {
                return null;
            }
            if (mc.world == null) {
                return null;
            }
            ILightContainer lightContainer = null;
            switch (lightCaster.type) {
                case 0:
                    TileEntity tileEntity = mc.world.getTileEntity(lightCaster.data2);
                    if (tileEntity instanceof TEDynamXBlock) {
                        lightContainer = ((TEDynamXBlock) tileEntity).getLightsModule();
                    }
                    break;
                case 1:
                    String data1 = lightCaster.data1;
                    if (data1.isEmpty()) {
                        return null;
                    }
                    UUID uuid = UUID.fromString(data1);
                    Entity entity = mc.player.world.loadedEntityList
                            .stream()
                            .filter(e -> e.getPersistentID().equals(uuid))
                            .findFirst()
                            .orElse(null);
                    if (entity instanceof PackPhysicsEntity) {
                        lightContainer = ((PackPhysicsEntity<?, ?>) entity).getModuleByType(AbstractLightsModule.class);
                    }
                    break;
                case 2:
                    String uuidStr = lightCaster.data1;
                    if (uuidStr.isEmpty()) {
                        return null;
                    }
                    UUID uuid1 = UUID.fromString(uuidStr);

                    lightContainer = DynamXItem.itemInstanceLights.get(uuid1);
                    break;
            }
            return lightContainer;
        }
    }
}
