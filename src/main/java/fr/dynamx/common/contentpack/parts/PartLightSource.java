package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.registry.*;
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
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RegisteredSubInfoType(name = "light", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER}, strictName = false)
public class PartLightSource implements ISubInfoType<ModularVehicleInfo>, IDrawablePart<BaseVehicleEntity<?>> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = SubInfoTypeRegistries.WHEELED_VEHICLES)
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("ShapePosition".equals(key))
            return new IPackFilePropertyFixer.FixResult("Position", true);
        return null;
    };

    private final ModularVehicleInfo owner;
    private final String name;

    @PackFileProperty(configNames = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, description = "common.position", required = false)
    private Vector3f position;
    @PackFileProperty(configNames = "Rotation", required = false, defaultValue = "1 0 0 0")
    private Quaternion rotation = new Quaternion();
    @PackFileProperty(configNames = "LightId")
    private int lightId;
    @PackFileProperty(configNames = "PartName")
    private String partName;
    @PackFileProperty(configNames = "Textures", required = false)
    private String[] textures;
    @PackFileProperty(configNames = "Colors", required = false, type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_ARRAY_ORDERED)
    private Vector3f[] colors;
    @PackFileProperty(configNames = "BlinkSequenceTicks", required = false)
    private int[] blinkSequence;
    @PackFileProperty(configNames = "RotateDuration", required = false)
    private int rotateDuration;

    public PartLightSource(ModularVehicleInfo owner, String name) {
        this.owner = owner;
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

    public String getLightName() {
        return name;
    }

    @Override
    public String getName() {
        return "LightSource_" + name;
    }

    @Override
    public String getPackName() {
        return owner.getPackName();
    }

    public int getLightId() {
        return lightId;
    }

    public String getPartName() {
        return partName;
    }

    public String[] getTextures() {
        return textures;
    }

    public int[] getBlinkSequence() {
        return blinkSequence;
    }

    public Vector3f getPosition() {
        return position;
    }

    public Quaternion getRotation() {
        return rotation;
    }

    public int getRotateDuration() {
        return rotateDuration;
    }

    private final Map<Integer, TextureVariantData> textureMap = new HashMap<>();

    public void mapTexture(int blinkStep, TextureVariantData textureVariantData) {
        textureMap.put(blinkStep, textureVariantData);
    }

    public Map<Integer, TextureVariantData> getTextureMap() {
        return textureMap;
    }

    public Vector3f[] getColors() {
        return colors;
    }

    @Override
    public void drawParts(@Nullable BaseVehicleEntity<?> entity, RenderPhysicsEntity<?> render, ModularVehicleInfo packInfo, byte textureId, float partialTicks) {
        //    setLightOn(9, true);
        //    setLightOn(1, false);
        /* Rendering light sources */
        if (MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.Render(VehicleEntityEvent.Render.Type.LIGHTS, (RenderBaseVehicle<?>) render, entity, PhysicsEntityEvent.Phase.PRE, partialTicks, null))) {
            return;
        }
        VehicleLightsModule lights = entity != null ? entity.getModuleByType(VehicleLightsModule.class) : null;
        ObjModelRenderer vehicleModel = DynamXContext.getObjModelRegistry().getModel(packInfo.getModel());
        for (PartLightSource.CompoundLight sources : packInfo.getLightSources().values()) {
            PartLightSource onSource = null;
            if (lights != null) {
                for (PartLightSource source : sources.getSources()) {
                    if (lights.isLightOn(source.getLightId())) {
                        onSource = source;
                    }
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
            GlStateManager.scale(packInfo.getScaleModifier().x, packInfo.getScaleModifier().y, packInfo.getScaleModifier().z);

            if (onSource.getPosition() != null) {
                DynamXRenderUtils.glTranslate(onSource.getPosition());
            }
            GlStateManager.rotate(GlQuaternionPool.get(onSource.getRotation()));

            if (lights != null && lights.isLightOn(onSource.getLightId()) && onSource.getRotateDuration() > 0) {
                float step = ((float) (entity.ticksExisted % onSource.getRotateDuration())) / onSource.getRotateDuration();
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
            render.renderModelGroup(vehicleModel, onSource.getPartName(), entity, texId);
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

    public static class CompoundLight implements IModelTextureVariantsSupplier.IModelTextureVariants {
        private final String partName;
        private final List<PartLightSource> sources = new ArrayList<>();
        private final Map<Byte, TextureVariantData> variantsMap = new HashMap<>();

        public CompoundLight(PartLightSource part) {
            this.partName = part.getPartName();
            addSource(part);
            variantsMap.put((byte) 0, new TextureVariantData("default", (byte) 0)); //todo configurable name
        }

        public void addSource(PartLightSource source) {
            sources.add(source);
            for (TextureVariantData textureVariantData : source.getTextureMap().values()) {
                //todo tester si ça ne s'entre-écrase pas
                variantsMap.put(textureVariantData.getId(), textureVariantData);
            }
        }

        public String getPartName() {
            return partName;
        }

        public List<PartLightSource> getSources() {
            return sources;
        }

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
            List<PartLightSource> sources = getSources();
            for (int i = 0; i < sources.size(); i++) {
                PartLightSource source = sources.get(i);
                if (source.getTextures() != null) {
                    for (int j = 0; j < source.getTextures().length; j++) {
                        TextureVariantData data = new TextureVariantData(source.getTextures()[j], (byte) (1 + i + j));
                        source.mapTexture(j, data);
                    }
                }
            }
        }
    }
}
