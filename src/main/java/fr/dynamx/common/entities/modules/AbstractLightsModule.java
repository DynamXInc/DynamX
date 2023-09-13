package fr.dynamx.common.entities.modules;

import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.common.contentpack.parts.ILightOwner;
import fr.dynamx.common.contentpack.parts.LightObject;
import fr.dynamx.common.contentpack.parts.PartLightSource;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractLightsModule implements IPhysicsModule<BaseVehiclePhysicsHandler<?>> {
    @Getter
    private final ILightOwner<?> lightOwner;

    public AbstractLightsModule(ILightOwner<?> lightOwner) {
        this.lightOwner = lightOwner;
    }

    public void setLightOn(String id, boolean state) {
        setLightOn(id.hashCode(), state);
    }

    public abstract void setLightOn(int id, boolean state);

    public boolean isLightOn(String id) {
        return isLightOn(id.hashCode());
    }

    public abstract boolean isLightOn(int id);

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
