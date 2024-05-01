package fr.dynamx.common.entities.modules;

import dz.betterlights.BetterLightsMod;
import dz.betterlights.dynamx.LightPartGroup;
import dz.betterlights.lighting.LightSerialization;
import dz.betterlights.lighting.lightcasters.BlockLightCaster;
import dz.betterlights.lighting.lightcasters.EntityLightCaster;
import dz.betterlights.lighting.lightcasters.LightCaster;
import dz.betterlights.network.EnumPacketType;
import dz.betterlights.network.PacketSyncLight;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.contentpack.parts.ILightOwner;
import fr.dynamx.common.contentpack.parts.LightObject;
import fr.dynamx.common.contentpack.parts.PartLightSource;
import fr.dynamx.common.contentpack.parts.lights.SpotLightObject;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.items.DynamXItem;
import fr.dynamx.common.network.lights.PacketSyncEntityLights;
import fr.dynamx.common.network.lights.PacketSyncPartLights;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractLightsModule implements IPhysicsModule<BaseVehiclePhysicsHandler<?>>, IPhysicsModule.IEntityUpdateListener, ILightContainer {
    @Getter
    protected final ILightOwner<?> lightOwner;

    protected final Map<SpotLightObject, LightCaster> lightCasters = new HashMap<>();

    @Getter
    protected final Map<Integer, LightPartGroup> lightCasterPartSyncs = new HashMap<>();

    public AbstractLightsModule(ILightOwner<?> lightOwner) {
        this.lightOwner = lightOwner;
    }

    public void setLightOn(String id, boolean state) {
        setLightOn(id.hashCode(), state);
    }

    public void setLightOn(int id, boolean state) {
        if (lightCasterPartSyncs.containsKey(id)) {
            lightCasterPartSyncs.get(id).setEnabled(state);
        }
    }

    public boolean isLightOn(String id) {
        return isLightOn(id.hashCode());
    }


    public boolean isLightOn(int id) {
        return lightCasterPartSyncs.containsKey(id) && lightCasterPartSyncs.get(id).isEnabled();
    }

    @Override
    public Map<SpotLightObject, LightCaster> getLightCasters() {
        return lightCasters;
    }

    @Override
    public Map<Integer, LightPartGroup> getLightCastersSync() {
        return lightCasterPartSyncs;
    }

    public void addLights(PackPhysicsEntity<?, ?> entity) {
        if (entity.world.isRemote) {
            addLights(entity, null, null, null, null);
        }
    }

    public void addLights(TEDynamXBlock tileEntity) {
        if (tileEntity.getWorld().isRemote) {
            addLights(null, tileEntity, null, null, null);
        }
    }

    public void addLights(DynamXItem<?> item, UUID id, Entity entityHolder) {
        if(entityHolder.world.isRemote) {
            addLights(null, null, item, id, entityHolder);
        }
    }

    public void addLights(@Nullable PackPhysicsEntity<?, ?> entity, @Nullable TEDynamXBlock tileEntity, @Nullable DynamXItem<?> item, @Nullable UUID id, @Nullable Entity entityHolder) {
       /* if (!FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            return;
        }*/
        for (PartLightSource compound : lightOwner.getLightSources().values()) {
            //Create all the light casters for the spotlight objects
            for (SpotLightObject spotLight : compound.getSpotLights()) {
                LightCaster lightCaster;
                if (entity != null) {
                    lightCaster = new EntityLightCaster(entity);
                } else if (tileEntity != null) {
                    lightCaster = new BlockLightCaster(tileEntity);
                } else if (item != null) {
                    lightCaster = new EntityLightCaster(entityHolder);
                } else
                    throw new IllegalArgumentException("Invalid light owner type. Must be either a block, entity or item.");
                createLightCaster(lightCaster, spotLight);
            }
            for (LightObject s : compound.getSources()) {
                //Add all the spotlight objects to each light object (ID -> spotlights)
                LightPartGroup lightCasterPartSync;
                World world;
                if (tileEntity != null) {
                    lightCasterPartSync = new LightPartGroup.BlockOwner(s.getLightId(), tileEntity.getPos());
                    world = tileEntity.getWorld();
                } else if (entity != null) {
                    lightCasterPartSync = new LightPartGroup.EntityAndItemOwner(LightPartGroup.EnumOwnerType.ENTITY, s.getLightId(), entity.getPersistentID());
                    world = entity.world;
                } else if (item != null) {
                    lightCasterPartSync = new LightPartGroup.EntityAndItemOwner(LightPartGroup.EnumOwnerType.ITEM, s.getLightId(), id);
                    world = entityHolder.world;
                } else
                    throw new IllegalArgumentException("Invalid light owner type. Must be either a block, entity or item.");

                lightCasterPartSync.setEnabled(s.getActivationState().equals(LightObject.ActivationState.ALWAYS));
                for (Map.Entry<SpotLightObject, LightCaster> entry : lightCasters.entrySet()) {
                    String objectName = entry.getKey().getOwner().getObjectName();
                    lightCasterPartSync.getLightCasters().put(objectName, entry.getValue());
                }
                lightCasterPartSyncs.put(lightCasterPartSync.getOwnerId(), lightCasterPartSync);
                if (!world.isRemote) {
                    DynamXContext.getNetwork().getVanillaNetwork().sendPacket(new PacketSyncPartLights(lightCasterPartSync, EnumPacketType.ADD), EnumPacketTarget.ALL, null);
                } else {
                    for (LightCaster caster : lightCasterPartSync.getLightCasters().values()) {
                        BetterLightsMod.getLightManager().addLightCaster(world, caster, false);
                        caster.lightPartGroup = lightCasterPartSync;
                    }
                }
            }
        }

    }

    public static class LightsModule extends AbstractLightsModule implements IPackInfoReloadListener {
        private final Map<Integer, Boolean> lightStates = new HashMap<>();

        public LightsModule(ILightOwner<?> lightOwner) {
            super(lightOwner);
            onPackInfosReloaded();
        }

        @Override
        public void onPackInfosReloaded() {
            for (PartLightSource compound : getLightOwner().getLightSources().values()) {
                for (LightObject s : compound.getSources()) {
                    lightStates.put(s.getLightId(), false);
                }
            }
        }

        @Override
        public void setLightOn(int id, boolean state) {
            if (lightStates.containsKey(id)) {
                lightStates.put(id, state);
            }
        }

        @Override
        public boolean isLightOn(int id) {
            return lightStates.getOrDefault(id, false);
        }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            NBTTagList d = new NBTTagList();
            lightStates.forEach((i, b) -> {
                NBTTagCompound light = new NBTTagCompound();
                light.setInteger("Id", i);
                light.setBoolean("St", b);
            });
            tag.setTag("lights_m_states", d);
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            NBTTagList d = tag.getTagList("lights_m_states", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < d.tagCount(); i++) {
                NBTTagCompound light = d.getCompoundTagAt(i);
                lightStates.put(light.getInteger("Id"), light.getBoolean("St"));
            }
        }


    }

    public static class EntityLightsModule extends AbstractLightsModule implements IPackInfoReloadListener {
        private final Entity entity;
        private boolean isInit;
        public boolean isSynced;

        public EntityLightsModule(Entity entity, ILightOwner<?> lightOwner) {
            super(lightOwner);
            this.entity = entity;
            if (entity instanceof PackPhysicsEntity)
                addLights((PackPhysicsEntity<?, ?>) entity);
        }

        @Override
        public void updateEntity() {
            super.updateEntity();
            if (entity.world.isRemote) {
                if (lightCasterPartSyncs.isEmpty() && !isInit) {
                    isInit = true;
                    DynamXContext.getNetwork().getVanillaNetwork().sendPacket(new PacketSyncEntityLights(entity.getEntityId()), EnumPacketTarget.SERVER, null);
                }
            }
        }

        @Override
        public void onPackInfosReloaded() {
            lightCasters.forEach((spotLightObject, lightCaster) -> {
                LightSerialization.getLights(entity.world).remove(lightCaster);
                BetterLightsMod.getLightManager().getLightsInWorld().remove(lightCaster);
            });
            lightCasterPartSyncs.clear();
            lightCasters.clear();
            /*if(entity instanceof PackPhysicsEntity){
                lightOwner = ((PackPhysicsEntity<?, ?>) entity).get
            }*/
            addLights((PackPhysicsEntity<?, ?>) entity);
        }
    }

    public static class BlockLightsModule extends AbstractLightsModule implements IPackInfoReloadListener {
        private final TileEntity tileEntity;
        private boolean isInit = false;

        public BlockLightsModule(TileEntity tileEntity, ILightOwner<?> lightOwner) {
            super(lightOwner);
            this.tileEntity = tileEntity;
        }

        @Override
        public void updateEntity() {
            super.updateEntity();
            if (!isInit) {
                isInit = true;
                addLights((TEDynamXBlock) tileEntity);
            }
        }

        public void removeLights() {
            if (!FMLCommonHandler.instance().getEffectiveSide().isServer()) {
                return;
            }
            for (Map.Entry<Integer, LightPartGroup> entry : lightCasterPartSyncs.entrySet()) {
                for (LightPartGroup value : lightCasterPartSyncs.values()) {
                    for (LightCaster lightCaster : value.getLightCasters().values()) {
                        DynamXContext.getNetwork().getVanillaNetwork().sendPacket(new PacketSyncLight(lightCaster, EnumPacketType.REMOVE), EnumPacketTarget.ALL, null);
                        LightSerialization.getLights(tileEntity.getWorld()).remove(lightCaster);
                        BetterLightsMod.getLightManager().getLightsInWorld().remove(lightCaster);
                    }
                }
            }
        }

        @Override
        public void onPackInfosReloaded() {

        }
    }

    public static class ItemLightsModule extends AbstractLightsModule implements IPackInfoReloadListener {
        private final DynamXItem<?> item;
        private final UUID id;
        private final Entity playerIn;

        public ItemLightsModule(DynamXItem<?> item, ILightOwner<?> lightOwner, UUID id, Entity playerIn) {
            super(lightOwner);
            this.item = item;
            this.id = id;
            this.playerIn = playerIn;
            addLights(item, id, playerIn);
        }


        public void removeLights() {
            for (LightPartGroup value : lightCasterPartSyncs.values()) {
                for (LightCaster lightCaster : value.getLightCasters().values()) {
                    LightSerialization.getLights(playerIn.world).remove(lightCaster);
                    BetterLightsMod.getLightManager().getLightsInWorld().remove(lightCaster);
                    if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
                        DynamXContext.getNetwork().getVanillaNetwork().sendPacket(new PacketSyncLight(lightCaster, EnumPacketType.REMOVE), EnumPacketTarget.ALL, null);
                    }
                }
            }
            lightCasterPartSyncs.clear();
            lightCasters.clear();
        }

        @Override
        public void onPackInfosReloaded() {
            removeLights();
            addLights(item, id, playerIn);
        }
    }

    public static class TrailerLightsModule extends AbstractLightsModule {
        private final PackPhysicsEntity<?, ?> trailer;

        public TrailerLightsModule(ILightOwner<?> lightOwner, PackPhysicsEntity<?, ?> trailer) {
            super(lightOwner);
            this.trailer = trailer;
        }

        @Override
        public void setLightOn(int id, boolean state) {
        }

        @Override
        public boolean isLightOn(int id) {
            TrailerAttachModule attachModule = trailer.getModuleByType(TrailerAttachModule.class);
            if (attachModule == null || attachModule.getConnectedEntity() == -1)
                return false;
            Entity entity = trailer.world.getEntityByID(attachModule.getConnectedEntity());
            if (!(entity instanceof BaseVehicleEntity<?>) || !((BaseVehicleEntity<?>) entity).hasModuleOfType(LightsModule.class))
                return false;
            return ((BaseVehicleEntity<?>) entity).getModuleByType(LightsModule.class).isLightOn(id);
        }
    }

}
