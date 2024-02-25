package fr.dynamx.common.network.lights;

import dz.betterlights.BetterLightsMod;
import dz.betterlights.dynamx.LightPartGroup;
import dz.betterlights.lighting.lightcasters.LightCaster;
import dz.betterlights.network.EnumPacketType;
import fr.aym.acslib.utils.packetserializer.ISerializablePacket;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.capability.itemdata.DynamXItemData;
import fr.dynamx.common.entities.ModularPhysicsEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.AbstractLightsModule;
import fr.dynamx.common.entities.modules.ILightContainer;
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

    private LightPartGroup lightPartGroup;
    private EnumPacketType actionType;


    public PacketSyncPartLights() {
    }

    public PacketSyncPartLights(LightPartGroup lightPartGroup, EnumPacketType actionType) {
        this(actionType);
        this.lightPartGroup = lightPartGroup;
    }

    public PacketSyncPartLights(EnumPacketType actionType) {
        this.actionType = actionType;
    }

    @Override
    public Object[] getObjectsToSave() {
        if (lightPartGroup == null) {
            return new Object[]{
                    actionType
            };
        }
        return new Object[]{
                actionType,
                lightPartGroup,
        };
    }

    @Override
    public void populateWithSavedObjects(Object[] objects) {
        actionType = (EnumPacketType) objects[0];
        if (objects.length == 2) {
            lightPartGroup = (LightPartGroup) objects[1];
        }
    }

    public static class ClientHandler implements IMessageHandler<PacketSyncPartLights, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncPartLights message, MessageContext ctx) {
            Minecraft mc = Minecraft.getMinecraft();
            LightPartGroup lightCaster = message.lightPartGroup;
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
                        for (LightCaster caster : lightCaster.getLightCasters().values()) {
                            BetterLightsMod.getLightManager().addLightCaster(caster, false);
                            caster.lightPartGroup = lightCaster;
                        }
                        //End of BetterLights
                        if (lightContainer != null)
                            lightContainer.getLightCastersSync().put(lightCaster.getOwnerId(), lightCaster);
                    });

                    break;
                case REMOVE:
                    if (lightContainer == null) {
                        Set<UUID> lightCasterIds = lightCaster.getLightCasters().values().stream()
                                .map(LightCaster::getId)
                                .collect(Collectors.toSet());

                        //BetterLights
                        BetterLightsMod.getLightManager().getLightsInWorld().stream()
                                .filter(lightCaster1 -> lightCasterIds.contains(lightCaster1.getId()))
                                .forEach(lightToRemove -> BetterLightsMod.getLightManager().removeLightCaster(lightToRemove, false));
                        //End of BetterLights
                    } else {
                        //BetterLights
                        for (LightCaster lightCaster11 : lightContainer.getLightCastersSync().get(lightCaster.getOwnerId()).getLightCasters().values()) {
                            BetterLightsMod.getLightManager().removeLightCaster(lightCaster11, false);
                        }
                        //End of BetterLights
                    }
                    break;
                case REMOVE_ALL:
                    if (lightContainer != null) {
                        //BetterLights
                        for (LightPartGroup value : lightContainer.getLightCastersSync().values()) {
                            for (LightCaster lightCaster1 : value.getLightCasters().values()) {
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
                        Set<UUID> lightCasterIds = lightCaster.getLightCasters().values().stream()
                                .map(LightCaster::getId)
                                .collect(Collectors.toSet());

                        BetterLightsMod.getLightManager().getLightsInWorld().stream()
                                .filter(lightCaster1 -> lightCasterIds.contains(lightCaster1.getId()))
                                .forEach(lightCaster1 -> {
                                    lightCaster1.setEnabled(lightCaster.isEnabled());
                                });
                        //End of BetterLights
                    } else {

                        lightContainer.getLightCastersSync().get(lightCaster.getOwnerId()).setEnabled(lightCaster.isEnabled());
                        //BetterLights
                        for (LightCaster caster : lightContainer.getLightCastersSync().get(lightCaster.getOwnerId()).getLightCasters().values()) {
                            caster.setEnabled(lightCaster.isEnabled());
                        }
                        //End of BetterLights
                    }
                    break;
            }
            return null;
        }

        private ILightContainer getLightContainer(Minecraft mc, PacketSyncPartLights message) {
            LightPartGroup lightPartGroup = message.lightPartGroup;
            if (lightPartGroup == null) {
                return null;
            }
            if (mc.world == null) {
                return null;
            }
            ILightContainer lightContainer = null;
            switch (lightPartGroup.getType()) {
                case BLOCK:
                    TileEntity tileEntity = mc.world.getTileEntity(((LightPartGroup.BlockOwner)lightPartGroup).getPos());
                    if (tileEntity instanceof TEDynamXBlock) {
                        lightContainer = ((TEDynamXBlock) tileEntity).getLightsModule();
                    }
                    break;
                case ENTITY:
                    UUID entityId = ((LightPartGroup.EntityAndItemOwner)lightPartGroup).getUuid();
                    if (entityId == null) {
                        return null;
                    }
                    Entity entity = mc.player.world.loadedEntityList
                            .stream()
                            .filter(e -> e.getPersistentID().equals(entityId))
                            .findFirst()
                            .orElse(null);
                    if (entity instanceof ModularPhysicsEntity) {
                        lightContainer = ((ModularPhysicsEntity<?>) entity).getModuleByType(AbstractLightsModule.class);
                    }
                    break;
                case ITEM:
                    UUID uuid = ((LightPartGroup.EntityAndItemOwner)lightPartGroup).getUuid();
                    if (uuid == null) {
                        return null;
                    }
                    lightContainer = DynamXItemData.itemInstanceLights.get(uuid);
                    break;
            }
            return lightContainer;
        }
    }
}
