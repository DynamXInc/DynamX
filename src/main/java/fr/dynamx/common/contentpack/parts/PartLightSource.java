package fr.dynamx.common.contentpack.parts;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.ICollisionsContainer;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.*;
import fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.client.handlers.ClientEventHandler;
import fr.dynamx.client.renders.model.texture.TextureVariantData;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.IRenderContext;
import fr.dynamx.client.renders.scene.SceneBuilder;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.client.renders.scene.node.SimpleNode;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.AbstractLightsModule;
import fr.dynamx.common.entities.vehicles.TrailerEntity;
import fr.dynamx.common.objloader.data.DxModelData;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import lombok.Getter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Contains multiple {@link LightObject}
 */
@Getter
@RegisteredSubInfoType(name = "MultiLight", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER, SubInfoTypeRegistries.BLOCKS, SubInfoTypeRegistries.PROPS}, strictName = false)
public class PartLightSource extends SubInfoType<ILightOwner<?>> implements ISubInfoTypeOwner<PartLightSource>, IDrawablePart<Object, IModelPackObject>, IModelTextureVariantsSupplier.IModelTextureVariants {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER, SubInfoTypeRegistries.BLOCKS, SubInfoTypeRegistries.PROPS})
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("PartName".equals(key))
            return new IPackFilePropertyFixer.FixResult("ObjectName", false);
        return null;
    };

    private final String partName;

    private final List<LightObject> sources = new ArrayList<>();

    @PackFileProperty(configNames = "ObjectName")
    protected String objectName;
    @PackFileProperty(configNames = "BaseMaterial", required = false)
    protected String baseMaterial = "default";
    /**
     * The position of this part, relative to the 3D model. <br>
     * If null, it will be read from the 3D model (if possible, see readPositionFromModel method).
     */
    @PackFileProperty(configNames = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, description = "common.position", required = false, defaultValue = "From model")
    protected Vector3f position;
    /**
     * The rotation of this part, relative to the 3D model. <br>
     * If null, it will be read from the 3D model (if possible, see readPositionFromModel method).
     */
    @PackFileProperty(configNames = "Rotation", required = false, defaultValue = "From model")
    protected Quaternion rotation;
    /**
     * Indicates if the position and rotation were read from the 3D model, or set by the user. <br>
     * Changes the behavior of the rendering.
     */
    protected boolean isAutomaticPosition;
    @PackFileProperty(configNames = "DependsOnNode", required = false, description = "PartLightSource.DependsOnNode")
    protected String nodeDependingOnName;

    public PartLightSource(ISubInfoTypeOwner<ILightOwner<?>> owner, String partName) {
        super(owner);
        this.partName = partName;
    }

    /**
     * If this is a rotating light, this method reads the position and rotation from the 3D model owning this part. <br>
     * If the configured position is null, this method reads the position. <br>
     * If the configured position and rotation are null, this method also reads the rotation (only for GLTF models). <br>
     * <br>
     * If this isn't a rotating light, we don't need to do any transform to render it, so we don't need its position and rotation.
     *
     * @param model The 3D model owning this part
     */
    public void readPositionFromModel(ResourceLocation model) {
        if (getPosition() == null && sources.stream().anyMatch(s -> s.getRotateDuration() > 0)) { //If the light isn't moving on itself, we don't need its position (it can be 0 0 0), but if it rotates, we need to place the rotation point correctly: we need the pos of the light
            DxModelData modelData = DynamXContext.getDxModelDataFromCache(DynamXUtils.getModelPath(getPackName(), model));
            if (modelData != null) {
                position = DynamXUtils.readPartPosition(modelData, getObjectName(), true);
                if (getRotation() == null && position != null)
                    rotation = DynamXUtils.readPartRotation(modelData, getObjectName());
            }
            if (getPosition() == null) {
                DynamXErrorManager.addPackError(getPackName(), "position_not_found_in_model", ErrorLevel.HIGH, owner.getName(), "3D object " + getObjectName() + " for part " + getName());
            } else {
                isAutomaticPosition = true;
            }
        }
    }

    @Override
    public void appendTo(ILightOwner<?> owner) {
        if (owner instanceof AbstractItemObject)
            readPositionFromModel(((AbstractItemObject) owner).getModel());
        if (position == null) {
            INamedObject parent = getRootOwner();
            DynamXErrorManager.addPackError(getPackName(), "required_property", ErrorLevel.HIGH, parent.getName(), "Position in " + getName());
            position = new Vector3f();
        } else {
            position.multLocal(((ICollisionsContainer) owner).getScaleModifier());
        }
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
    public void addToSceneGraph(IModelPackObject packInfo, SceneBuilder<IRenderContext, IModelPackObject> sceneBuilder) {
        if (nodeDependingOnName != null) {
            sceneBuilder.addNode(packInfo, this, nodeDependingOnName);
        } else {
            sceneBuilder.addNode(packInfo, this);
        }
    }

    @Override
    public SceneNode<IRenderContext, IModelPackObject> createSceneGraph(Vector3f modelScale, List<SceneNode<IRenderContext, IModelPackObject>> childGraph) {
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

    class PartLightNode<A extends IModelPackObject> extends SimpleNode<IRenderContext, A> {
        public PartLightNode(PartLightSource lightSource, Vector3f scale, List<SceneNode<IRenderContext, A>> linkedChilds) {
            super(lightSource.getPosition(), lightSource.getRotation() != null ? GlQuaternionPool.newGlQuaternion(lightSource.getRotation()) : null, PartLightSource.this.isAutomaticPosition, scale, linkedChilds);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void render(IRenderContext context, A packInfo) {
            /* Rendering light sources */
            boolean isEntity = context instanceof BaseRenderContext.EntityRenderContext && ((BaseRenderContext.EntityRenderContext) context).getEntity() != null;
            AbstractLightsModule lights = isEntity ? ((BaseRenderContext.EntityRenderContext) context).getEntity().getModuleByType(AbstractLightsModule.class) :
                    context instanceof BaseRenderContext.BlockRenderContext && ((BaseRenderContext.BlockRenderContext) context).getTileEntity() != null ? ((BaseRenderContext.BlockRenderContext) context).getTileEntity().getLightsModule() : null;
            transformToRotationPoint();
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
                int mod = ClientEventHandler.MC.getRenderViewEntity().ticksExisted % seq[seq.length - 1];
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
                float step = ((float) (ClientEventHandler.MC.getRenderViewEntity().ticksExisted % onLightObject.getRotateDuration())) / onLightObject.getRotateDuration();
                step = step * (FastMath.PI * 2);
                transform.rotate(step, 0, 1, 0);
            }
            GlStateManager.pushMatrix();
            GlStateManager.multMatrix(ClientDynamXUtils.getMatrixBuffer(transform));
            transformToPartPos();
            context.getModel().renderGroup(getObjectName(), texId, context.isUseVanillaRender());
            if (isEntity && isOn) {
                int i = ((BaseRenderContext.EntityRenderContext) context).getEntity().getBrightnessForRender();
                int j = i % 65536;
                int k = i / 65536;
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) j, (float) k);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            }
            GlStateManager.popMatrix();
            renderChildren(context, packInfo);
        }

        @Override
        public void renderDebug(IRenderContext context, A packInfo) {
            if (DynamXDebugOptions.LIGHTS.isActive()) {
                GlStateManager.pushMatrix();
                transformForDebug();
                RenderGlobal.drawBoundingBox(-0.05f, -0.05f, -0.05f, 0.05f, 0.05f, 0.05f,
                        1, 1, 0, 1);
                GlStateManager.popMatrix();
            }
            super.renderDebug(context, packInfo);
        }
    }
}
