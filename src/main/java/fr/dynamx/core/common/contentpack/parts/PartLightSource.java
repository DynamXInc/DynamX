package fr.dynamx.core.common.contentpack.parts;

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
import fr.dynamx.core.client.handlers.ClientEventHandler;
import fr.dynamx.core.client.renders.model.texture.TextureVariantData;
import fr.dynamx.core.client.renders.scene.BaseRenderContext;
import fr.dynamx.core.client.renders.scene.IRenderContext;
import fr.dynamx.core.client.renders.scene.SceneBuilder;
import fr.dynamx.core.client.renders.scene.node.SceneNode;
import fr.dynamx.core.client.renders.scene.node.SimpleNode;
import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.common.blocks.TEDynamXBlock;
import fr.dynamx.core.common.contentpack.type.MaterialVariantsInfo;
import fr.dynamx.core.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.core.common.entities.PackPhysicsEntity;
import fr.dynamx.core.common.entities.modules.AbstractLightsModule;
import fr.dynamx.core.common.entities.vehicles.TrailerEntity;
import fr.dynamx.core.common.objloader.data.DxModelData;
import fr.dynamx.core.utils.DynamXUtils;
import fr.dynamx.core.utils.debug.DynamXDebugOptions;
import fr.dynamx.core.utils.errors.DynamXErrorManager;
import fr.hermes.api.mc.utils.HmResourceLocation;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Contains multiple {@link LightObject}
 */
@Setter
@Getter
@RegisteredSubInfoType(name = "MultiLight", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER, SubInfoTypeRegistries.BLOCKS, SubInfoTypeRegistries.PROPS}, strictName = false)
public class PartLightSource extends SubInfoType<ILightOwner<?>> implements ISubInfoTypeOwner<PartLightSource>, IDrawablePart<IModelPackObject> {
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

    /**
     * The base, off, material of the light <br>
     * By default, it will use the primary material given in the obj model <br>
     * Overridden by MaterialVariants BaseMaterial, <strong>if defined</strong>
     */
    @PackFileProperty(configNames = "BaseMaterial", required = false, defaultValue = "MaterialVariantsInfo value, or primary material configured in the model")
    protected String baseMaterial;

    /**
     * Maps the metadata to the texture data <br>
     * Optional on light sources, but should be used to have different "off" state textures, according to the vehicle/block/prop variants
     */
    private MaterialVariantsInfo<PartLightSource> variants;

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
    public void readPositionFromModel(HmResourceLocation model) {
        if (getPosition() != null) {
            return;
        }
        //If the light isn't moving on itself, we don't need its position (it can be 0 0 0), but if it rotates, we need to place the rotation point correctly: we need the pos of the light
        if (sources.stream().noneMatch(s -> s.getRotateDuration() > 0)) {
            position = new Vector3f();
            return;
        }
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
    public void addBlockModules(TEDynamXBlock blockEntity, ModuleListBuilder modules) {
        addModules(null, modules);
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

    /**
     * Post loads this light (computes texture variants)
     */
    public void postLoad(boolean hotReload) {
        configureLightTextureVariants(hotReload);
    }

    /**
     * Computes texture variants of this lights <br>
     * It adds the variants configured on the light, and the owner's variants, if any
     */
    public void configureLightTextureVariants(boolean hotReload) {
        TextureVariantData textureVariant;
        Map<String, TextureVariantData> nameToVariant = new HashMap<>();
        // Create material variants if not set by the user
        if (variants == null) {
            variants = new MaterialVariantsInfo<>(this);
            textureVariant = new TextureVariantData(baseMaterial != null ? baseMaterial : "default", (byte) 0);
            variants.addVariant(textureVariant, hotReload);
        } else if (baseMaterial != null && variants.getBaseMaterial().equalsIgnoreCase("default")) {
            // Add base light state, if customized here but not in MaterialVariantsInfo yet
            variants.setBaseMaterial(baseMaterial);
            textureVariant = new TextureVariantData(baseMaterial, (byte) 0);
            variants.addVariant(textureVariant, hotReload);
        }
        // Add known variants to the nameToVariant map, and compute the last used variantId
        // The last used variant id is either the max variant id of the light owner, or the max id of the variants configured here
        AtomicReference<Byte> nextTextureId = new AtomicReference<>(owner instanceof IModelTextureVariantsSupplier ? ((IModelTextureVariantsSupplier) owner).getMaxVariantId() : 0);
        variants.getTextureVariants().forEach((id, variant) -> {
            if (nameToVariant.containsKey(variant.getName())) {
                return;
            }
            nameToVariant.put(variant.getName(), variant);
            if (variant.getId() >= nextTextureId.get()) {
                nextTextureId.set((byte) (variant.getId() + 1));
            }
        });

        // Add "on" light variants
        List<LightObject> sources = getSources();
        for (LightObject source : sources) {
            if (source.getTextures() == null) {
                continue;
            }
            source.getBlinkTextures().clear();
            // For each configured light textures
            for (int j = 0; j < source.getTextures().length; j++) {
                String name = source.getTextures()[j];
                if (nameToVariant.containsKey(name)) {
                    // Texture shared with other sources
                    source.getBlinkTextures().add(nameToVariant.get(name));
                } else {
                    // Add a new texture to the light
                    textureVariant = new TextureVariantData(name, nextTextureId.getAndSet((byte) (nextTextureId.get() + 1)));
                    source.getBlinkTextures().add(textureVariant);
                    variants.addVariant(textureVariant, hotReload);
                    nameToVariant.put(name, textureVariant);
                }
            }
        }
    }

    public void addLightSource(LightObject object) {
        sources.add(object);
    }

    @Override
    public void addSubProperty(ISubInfoType<PartLightSource> property) {
        if (property instanceof MaterialVariantsInfo) {
            variants = (MaterialVariantsInfo<PartLightSource>) property;
            return;
        }
        throw new IllegalStateException("Cannot add sub property to a light");
    }

    @Override
    public List<ISubInfoType<PartLightSource>> getSubProperties() {
        return Collections.emptyList();
    }

    class PartLightNode<A extends IModelPackObject> extends SimpleNode<IRenderContext, A> {
        public PartLightNode(PartLightSource lightSource, Vector3f scale, List<SceneNode<IRenderContext, A>> linkedChilds) {
            super(lightSource.getPosition(), lightSource.getRotation() != null ? lightSource.getRotation() : null, PartLightSource.this.isAutomaticPosition, scale, linkedChilds);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void render(IRenderContext context, A packInfo, Matrix4f parentTransform) {
            /* Rendering light sources */
            boolean isEntity = context instanceof BaseRenderContext.EntityRenderContext && ((BaseRenderContext.EntityRenderContext) context).getEntity() != null;
            AbstractLightsModule lights = isEntity ? ((BaseRenderContext.EntityRenderContext) context).getEntity().getModuleByType(AbstractLightsModule.class) :
                    context instanceof BaseRenderContext.BlockRenderContext && ((BaseRenderContext.BlockRenderContext) context).getTileEntity() != null ? ((BaseRenderContext.BlockRenderContext) context).getTileEntity().getModuleByType(AbstractLightsModule.class) : null;
            transformToRotationPoint(parentTransform);
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
            // Compute the texture id:
            byte texId;
            if (isOn && !onLightObject.getBlinkTextures().isEmpty()) {
                // If on, use the on texture at this step
                activeStep = activeStep % onLightObject.getBlinkTextures().size();
                texId = onLightObject.getBlinkTextures().get(activeStep).getId();
            } else {
                // If off, get the current object/owner variant, and check that it exists for the light. Else, use the default variant.
                // This assumes "on" variants have variant ids strictly bigger than variants of the object/owner.
                texId = context.getTextureId();
                if (!getVariants().hasVariant(texId)) {
                    texId = 0;
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
            //Render the light
            if (lights != null && lights.isLightOn(onLightObject.getLightId()) && onLightObject.getRotateDuration() > 0) {
                float step = ((float) (ClientEventHandler.MC.getRenderViewEntity().ticksExisted % onLightObject.getRotateDuration())) / onLightObject.getRotateDuration();
                step = step * (FastMath.PI * 2);
                transform.rotate(step, 0, 1, 0);
            }

            GlStateManager.pushMatrix();
            glTransformToPartPos();
            context.getModel().renderGroup(getObjectName(), texId, context.isUseVanillaRender());
            if (isEntity && isOn) {
                int i = ((BaseRenderContext.EntityRenderContext) context).getEntity().getBrightnessForRender();
                int j = i % 65536;
                int k = i / 65536;
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) j, (float) k);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            } else if (context instanceof BaseRenderContext.BlockRenderContext && isOn) {
                int i = ClientEventHandler.MC.world.getCombinedLight(((BaseRenderContext.BlockRenderContext) context).getTileEntity().getPos(), 0);
                int j = i % 65536;
                int k = i / 65536;
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) j, (float) k);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            }
            GlStateManager.popMatrix();
            renderChildren(context, packInfo, transform);
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
