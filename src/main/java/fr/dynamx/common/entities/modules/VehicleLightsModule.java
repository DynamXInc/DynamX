package fr.dynamx.common.entities.modules;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
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
import scala.xml.dtd.impl.Base;

import java.util.HashMap;
import java.util.Map;

public class VehicleLightsModule implements IPhysicsModule<BaseVehiclePhysicsHandler<?>>, IPhysicsModule.IDrawableModule<BaseVehicleEntity<?>>, IPackInfoReloadListener {
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

    @Override
    public void drawParts(RenderPhysicsEntity<?> render, float partialTicks, BaseVehicleEntity<?> carEntity) {
        //    setLightOn(9, true);
        //    setLightOn(1, false);
        /* Rendering light sources */
        if (!MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.LIGHTS, (RenderBaseVehicle<?>) render, carEntity, PhysicsEntityEvent.Phase.PRE, partialTicks))) {
            if (carEntity.hasModuleOfType(VehicleLightsModule.class)) {
                ObjModelRenderer vehicleModel = DynamXContext.getObjModelRegistry().getModel(carEntity.getPackInfo().getModel());
                for (PartLightSource.CompoundLight sources : carEntity.getPackInfo().getLightSources().values()) {
                    PartLightSource onSource = null;
                    for (PartLightSource source : sources.getSources()) {
                        if (isLightOn(source.getLightId())) {
                            onSource = source;
                        }
                    }
                    boolean isOn = true;
                    if (onSource == null) {
                        isOn = false;
                        onSource = sources.getSources().get(0);
                    }
                    int activeStep = 0;
                    if (isOn && onSource.getBlinkSequence() != null) {
                        int[] seq = onSource.getBlinkSequence();
                        int mod = carEntity.ticksExisted % seq[seq.length - 1];
                        isOn = false; //Default state
                        for (int i = seq.length - 1; i >= 0; i--) {
                            if (mod > seq[i]) {
                                isOn = i % 2 == 0;
                                activeStep = (byte) (i + 1);
                                break;
                            }
                        }
                    }

                    //Set luminescent
                    if (isOn) {
                        int i = 15728880;
                        int j = i % 65536;
                        int k = i / 65536;
                        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) j, (float) k);
                        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    }

                    GlQuaternionPool.openPool();
                    GlStateManager.pushMatrix();
                    GlStateManager.scale(carEntity.getPackInfo().getScaleModifier().x, carEntity.getPackInfo().getScaleModifier().y, carEntity.getPackInfo().getScaleModifier().z);

                    if (onSource.getPosition() != null) {
                        DynamXRenderUtils.glTranslate(onSource.getPosition());
                    }
                    GlStateManager.rotate(GlQuaternionPool.get(onSource.getRotation()));

                    if (isLightOn(onSource.getLightId()) && onSource.getRotateDuration() > 0) {
                        float step = ((float) (carEntity.ticksExisted % onSource.getRotateDuration())) / onSource.getRotateDuration();
                        step = step * 360;
                        GlStateManager.rotate(step, 0, 1, 0);
                    }
                    byte texId = 0;
                    if (onSource.getTextures() != null) {
                        if (isOn && !onSource.getTextureMap().containsKey(activeStep)) {
                            activeStep = activeStep % onSource.getTextureMap().size();
                            if (!onSource.getTextureMap().containsKey(activeStep)) {
                                isOn = false;
                                //TODO CLEAN
                                System.out.println("WARN TEXT NOT FOUND " + activeStep + " :" + onSource + " " + onSource.getTextureMap());
                            } else {
                                texId = onSource.getTextureMap().get(activeStep).getId();
                            }
                        } else {
                            texId = isOn ? onSource.getTextureMap().get(activeStep).getId() : (byte) 0;
                        }
                    } else if (onSource.getColors() != null && activeStep < onSource.getColors().length) {
                        Vector3f color = onSource.getColors()[activeStep];
                        GlStateManager.color(color.x / 255, color.y / 255, color.z / 255, 1);
                    }
                    render.renderModelGroup(vehicleModel, onSource.getPartName(), carEntity, texId);
                    GlStateManager.popMatrix();
                    GlQuaternionPool.closePool();

                    int i = carEntity.getBrightnessForRender();
                    int j = i % 65536;
                    int k = i / 65536;
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) j, (float) k);
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                }
            }
            MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.LIGHTS, (RenderBaseVehicle<?>) render, carEntity, PhysicsEntityEvent.Phase.POST, partialTicks));
        }
    }
}
