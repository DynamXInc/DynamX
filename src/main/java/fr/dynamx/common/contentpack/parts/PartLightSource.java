package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.api.obj.IModelTextureVariantsSupplier;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.client.renders.model.texture.TextureVariantData;
import fr.dynamx.client.renders.vehicle.RenderBaseVehicle;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.VehicleLightsModule;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import lombok.Getter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Contains multiple {@link LightObject}
 */
@RegisteredSubInfoType(name = "MultiLight", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER}, strictName = false)
public class PartLightSource extends SubInfoType<ModularVehicleInfo> implements ISubInfoTypeOwner<PartLightSource>, IDrawablePart<BaseVehicleEntity<?>>, IModelTextureVariantsSupplier.IModelTextureVariants {
    private final String name;

    @Getter
    private final List<LightObject> sources = new ArrayList<>();

    @Getter
    @PackFileProperty(configNames = "PartName")
    protected String partName;
    @Getter
    @PackFileProperty(configNames = "BaseMaterial", required = false)
    protected String baseMaterial = "default";
    @Getter
    @PackFileProperty(configNames = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, description = "common.position", required = false)
    protected Vector3f position;
    @Getter
    @PackFileProperty(configNames = "Rotation", required = false, defaultValue = "1 0 0 0")
    protected Quaternion rotation = new Quaternion();

    public PartLightSource(ModularVehicleInfo owner, String name) {
        super(owner);
        this.name = name;
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        owner.addLightSource(this);
    }

    @Nullable
    @Override
    public ModularVehicleInfo getOwner() {
        return owner;
    }

    @Override
    public void addModules(BaseVehicleEntity<?> entity, ModuleListBuilder modules) {
        if (!modules.hasModuleOfClass(VehicleLightsModule.class)) {
            modules.add(new VehicleLightsModule(entity));
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPackName() {
        return owner.getPackName();
    }

    @Override
    public void drawParts(@Nullable BaseVehicleEntity<?> entity, RenderPhysicsEntity<?> render, ModularVehicleInfo packInfo, byte textureId, float partialTicks) {
        /* Rendering light sources */
        if (MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.LIGHTS, (RenderBaseVehicle<?>) render, entity, PhysicsEntityEvent.Phase.PRE, partialTicks, null))) {
            return;
        }
        VehicleLightsModule lights = entity != null ? entity.getModuleByType(VehicleLightsModule.class) : null;
        ObjModelRenderer vehicleModel = DynamXContext.getObjModelRegistry().getModel(packInfo.getModel());
        for (PartLightSource lightSource : packInfo.getLightSources().values()) {
            LightObject onLightObject = null;
            if (lights != null) {
                // Find the first light object that is on
                for (LightObject source : lightSource.getSources()) {
                    if (lights.isLightOn(source.getLightId())) {
                        onLightObject = source;
                        break;
                    }
                }
            }
            boolean isOn = true;
            if (onLightObject == null) {
                isOn = false;
                onLightObject = lightSource.getSources().get(0);
            }
            // Do blinking
            int activeStep = 0;
            if (isOn && onLightObject.getBlinkSequence() != null) {
                int[] seq = onLightObject.getBlinkSequence();
                int mod = entity.ticksExisted % seq[seq.length - 1];
                isOn = false; //Default state
                for (int i = seq.length - 1; i >= 0; i--) {
                    if (mod > seq[i]) {
                        isOn = i % 2 == 0;
                        activeStep = (byte) (i + 1);
                        break;
                    }
                }
            }
            byte texId = 0;
            if (isOn && !onLightObject.getBlinkTextures().isEmpty()) {
                activeStep = activeStep % onLightObject.getBlinkTextures().size();
                texId = onLightObject.getBlinkTextures().get(activeStep).getId();
            }

            //Set luminescent
            if (isOn) {
                int i = 15728880;
                int j = i % 65536;
                int k = i / 65536;
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) j, (float) k);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            }

            //Render the light
            GlQuaternionPool.openPool();
            GlStateManager.pushMatrix();
            GlStateManager.scale(packInfo.getScaleModifier().x, packInfo.getScaleModifier().y, packInfo.getScaleModifier().z);
            if (lightSource.getPosition() != null)
                DynamXRenderUtils.glTranslate(lightSource.getPosition());
            GlStateManager.rotate(GlQuaternionPool.get(lightSource.getRotation()));
            if (lights != null && lights.isLightOn(onLightObject.getLightId()) && onLightObject.getRotateDuration() > 0) {
                float step = ((float) (entity.ticksExisted % onLightObject.getRotateDuration())) / onLightObject.getRotateDuration();
                step = step * 360;
                GlStateManager.rotate(step, 0, 1, 0);
            }
            render.renderModelGroup(vehicleModel, lightSource.getPartName(), entity, texId);
            GlStateManager.popMatrix();
            GlQuaternionPool.closePool();

            if (entity != null) {
                int i = entity.getBrightnessForRender();
                int j = i % 65536;
                int k = i / 65536;
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) j, (float) k);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
        MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.LIGHTS, (RenderBaseVehicle<?>) render, entity, PhysicsEntityEvent.Phase.POST, partialTicks, null));
    }

    @Override
    public String[] getRenderedParts() {
        return new String[]{getPartName()};
    }

    private final Map<Byte, TextureVariantData> variantsMap = new HashMap<>();

    @Override
    public TextureVariantData getDefaultVariant() {
        return variantsMap.get((byte) 0);
    }

    @Override
    public TextureVariantData getVariant(byte variantId) {
        return variantsMap.getOrDefault(variantId, getDefaultVariant());
    }

    @Override
    public Map<Byte, TextureVariantData> getTextureVariants() {
        return variantsMap;
    }

    /**
     * Post loads this light (computes texture variants)
     */
    public void postLoad() {
        Map<String, TextureVariantData> nameToVariant = new HashMap<>();
        byte nextTextureId = 0;
        TextureVariantData data = new TextureVariantData(baseMaterial, nextTextureId);
        variantsMap.put(data.getId(), data);
        nameToVariant.put(baseMaterial, data);

        List<LightObject> sources = getSources();
        for (LightObject source : sources) {
            if (source.getTextures() != null) {
                source.getBlinkTextures().clear();
                for (int j = 0; j < source.getTextures().length; j++) {
                    String name = source.getTextures()[j];
                    if (nameToVariant.containsKey(name)) {
                        source.getBlinkTextures().add(nameToVariant.get(name));
                    } else {
                        data = new TextureVariantData(name, ++nextTextureId);
                        source.getBlinkTextures().add(data);
                        variantsMap.put(data.getId(), data);
                        nameToVariant.put(name, data);
                    }
                }
            }
        }
    }

    public void addLightSource(LightObject object) {
        sources.add(object);
    }

    @Override
    public void addSubProperty(ISubInfoType<PartLightSource> property) {
    }

    @Override
    public List<ISubInfoType<PartLightSource>> getSubProperties() {
        return Collections.emptyList();
    }
}
