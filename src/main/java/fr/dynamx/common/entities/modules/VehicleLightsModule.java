package fr.dynamx.common.entities.modules;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.client.renders.vehicle.RenderBaseVehicle;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.parts.PartLightSource;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;

import java.util.HashMap;
import java.util.Map;

public class VehicleLightsModule implements IPhysicsModule<BaseVehiclePhysicsHandler<?>>, IPackInfoReloadListener {
    private final BaseVehicleEntity<?> entity;
    private final Map<Integer, Boolean> lightStates = new HashMap<>();

    public VehicleLightsModule(BaseVehicleEntity<?> entity) {
        this.entity = entity;
        onPackInfosReloaded();
    }

    @Override
    public void onPackInfosReloaded() {
        for (PartLightSource.CompoundLight compound : entity.getPackInfo().getLightSources().values()) {
            for (PartLightSource s : compound.getSources()) {
                lightStates.put(s.getLightId(), false);
            }
        }
    }

    public void setLightOn(int id, boolean state) {
        if (lightStates.containsKey(id)) {
            lightStates.put(id, state);
        }
    }

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
