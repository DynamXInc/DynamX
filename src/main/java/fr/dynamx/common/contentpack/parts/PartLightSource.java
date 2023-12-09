package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.*;
import fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.client.renders.model.texture.TextureVariantData;
import fr.dynamx.client.renders.scene.EntityRenderContext;
import fr.dynamx.client.renders.scene.SceneBuilder;
import fr.dynamx.client.renders.scene.SceneGraph;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.AbstractLightsModule;
import fr.dynamx.common.entities.vehicles.TrailerEntity;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import lombok.Getter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Contains multiple {@link LightObject}
 */
@RegisteredSubInfoType(name = "MultiLight", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER, SubInfoTypeRegistries.BLOCKS, SubInfoTypeRegistries.PROPS}, strictName = false)
public class PartLightSource extends SubInfoType<ILightOwner<?>> implements ISubInfoTypeOwner<PartLightSource>, IDrawablePart<PackPhysicsEntity<?, ?>, IPhysicsPackInfo>, IModelTextureVariantsSupplier.IModelTextureVariants {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER})
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("PartName".equals(key))
            return new IPackFilePropertyFixer.FixResult("ObjectName", false);
        return null;
    };

    @Getter
    private final String partName;

    @Getter
    private final List<LightObject> sources = new ArrayList<>();

    @Getter
    @PackFileProperty(configNames = "ObjectName")
    protected String objectName;
    @Getter
    @PackFileProperty(configNames = "BaseMaterial", required = false)
    protected String baseMaterial = "default";
    @Getter
    @PackFileProperty(configNames = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, description = "common.position", required = false)
    protected Vector3f position;
    @Getter
    @PackFileProperty(configNames = "Rotation", required = false, defaultValue = "none")
    protected Quaternion rotation = new Quaternion();
    @Getter
    @PackFileProperty(configNames = "DependsOnNode", required = false, description = "PartLightSource.DependsOnNode")
    protected String nodeDependingOnName;

    public PartLightSource(ISubInfoTypeOwner<ILightOwner<?>> owner, String partName) {
        super(owner);
        this.partName = partName;
    }

    @Override
    public void appendTo(ILightOwner<?> owner) {
        owner.addLightSource(this);
    }

    @Nullable
    @Override
    public ILightOwner<?> getOwner() {
        return owner;
    }

    @Override
    public void addModules(PackPhysicsEntity<?, ?> entity, ModuleListBuilder modules) {
        if (!modules.hasModuleOfClass(AbstractLightsModule.class)) {
            if (entity instanceof TrailerEntity)
                modules.add(new AbstractLightsModule.TrailerLightsModule(getOwner(), entity));
            else
                modules.add(new AbstractLightsModule.LightsModule(getOwner()));
        }
    }

    @Override
    public String getName() {
        return "PartLightSource with name " + getPartName();
    }

    @Override
    public String getNodeName() {
        return getPartName();
    }

    @Override
    public void addToSceneGraph(IPhysicsPackInfo packInfo, SceneBuilder<PackPhysicsEntity<?, ?>, IPhysicsPackInfo> sceneBuilder) {
        if (nodeDependingOnName != null) {
            sceneBuilder.addNode(packInfo, this, nodeDependingOnName);
        } else {
            sceneBuilder.addNode(packInfo, this);
        }
    }

    @Override
    public SceneGraph<PackPhysicsEntity<?, ?>, IPhysicsPackInfo> createSceneGraph(Vector3f modelScale, List<SceneGraph<PackPhysicsEntity<?, ?>, IPhysicsPackInfo>> childGraph) {
        return new PartLightNode<>(this, modelScale, (List) childGraph);
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
        throw new IllegalStateException("Cannot add sub property to a light");
    }

    @Override
    public List<ISubInfoType<PartLightSource>> getSubProperties() {
        return Collections.emptyList();
    }

    class PartLightNode<T extends BaseVehicleEntity<?>, A extends IPhysicsPackInfo> extends SceneGraph.Node<T, A> {
        public PartLightNode(PartLightSource lightSource, Vector3f scale, List<SceneGraph<T, A>> linkedChilds) {
            super(lightSource.getPosition(), GlQuaternionPool.newGlQuaternion(lightSource.getRotation()), scale, linkedChilds);
        }

        @Override
        public void render(@Nullable T entity, EntityRenderContext context, A packInfo) {
            /* Rendering light sources */
            AbstractLightsModule lights = entity != null ? entity.getModuleByType(AbstractLightsModule.class) : null;
            GlStateManager.pushMatrix();
            transform();
            /* Rendering light source */
            LightObject onLightObject = null;
            if (lights != null) {
                // Find the first light object that is on
                for (LightObject source : getSources()) {
                    if (lights.isLightOn(source.getLightId())) {
                        onLightObject = source;
                        break;
                    }
                }
            }
            boolean isOn = true;
            if (onLightObject == null) {
                isOn = false;
                onLightObject = getSources().get(0);
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
            //for testing only isOn = onLightObject != null;
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
            if (lights != null && lights.isLightOn(onLightObject.getLightId()) && onLightObject.getRotateDuration() > 0) {
                float step = ((float) (entity.ticksExisted % onLightObject.getRotateDuration())) / onLightObject.getRotateDuration();
                step = step * 360;
                GlStateManager.rotate(step, 0, 1, 0);
            }
            context.getModel().renderGroups(getObjectName(), texId, context.isUseVanillaRender());
            if (entity != null && isOn) {
                int i = entity.getBrightnessForRender();
                int j = i % 65536;
                int k = i / 65536;
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) j, (float) k);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            }
            renderChildren(entity, context, packInfo);
            GlStateManager.popMatrix();
        }

        @Override
        public void renderDebug(@Nullable T entity, EntityRenderContext context, A packInfo) {
            if(DynamXDebugOptions.LIGHTS.isActive()) {
                GlStateManager.pushMatrix();
                transformForDebug();
                RenderGlobal.drawBoundingBox(-0.05f, -0.05f, -0.05f, 0.05f, 0.05f, 0.05f,
                        1, 1, 0, 1);
                GlStateManager.popMatrix();
            }
            super.renderDebug(entity, context, packInfo);
        }
    }
}
