package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.ICollisionsContainer;
import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.api.events.CreatePackItemEvent;
import fr.dynamx.api.events.client.BuildSceneGraphEvent;
import fr.dynamx.client.renders.model.ItemDxModel;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;
import fr.dynamx.client.renders.model.texture.TextureVariantData;
import fr.dynamx.client.renders.scene.node.EntityNode;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.loader.InfoList;
import fr.dynamx.common.contentpack.parts.ILightOwner;
import fr.dynamx.common.contentpack.parts.PartLightSource;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.common.contentpack.type.MaterialVariantsInfo;
import fr.dynamx.common.contentpack.type.ObjectCollisionsHelper;
import fr.dynamx.common.contentpack.type.ParticleEmitterInfo;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.EnumPlayerStandOnTop;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Matrix4f;

import java.util.*;

/**
 * All information about a vehicle
 *
 * @see BaseVehicleEntity
 */
@Getter
public class ModularVehicleInfo extends AbstractItemObject<ModularVehicleInfo, ModularVehicleInfo> implements IPhysicsPackInfo, IModelTextureVariantsSupplier,
        ParticleEmitterInfo.IParticleEmitterContainer, IModelPackObject, IPartContainer<ModularVehicleInfo>, ICollisionsContainer, ILightOwner<ModularVehicleInfo> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER})
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("UseHullShape".equals(key))
            return new IPackFilePropertyFixer.FixResult("UseComplexCollisions", true);
        if ("Textures".equals(key))
            return new IPackFilePropertyFixer.FixResult("MaterialVariants", true, true);
        if ("ItemTranslate".equals(key))
            return new IPackFilePropertyFixer.FixResult("ItemTransforms block", true, true);
        if ("ItemRotate".equals(key))
            return new IPackFilePropertyFixer.FixResult("ItemTransforms block", true, true);
        return null;
    };

    @Setter
    private VehicleValidator validator;

    /* == Pack properties == */

    @PackFileProperty(configNames = "DefaultEngine", required = false)
    protected String defaultEngine;
    @PackFileProperty(configNames = "DefaultSounds", required = false)
    protected String defaultSounds;

    @PackFileProperty(configNames = "MaxVehicleSpeed", required = false, defaultValue = "infinite")
    protected float vehicleMaxSpeed = Integer.MAX_VALUE;

    /**
     * The directing wheel id <br>
     * Used to render the steering wheel
     */
    private int directingWheel;

    @Getter
    @Setter
    @PackFileProperty(configNames = "PlayerStandOnTop", required = false, defaultValue = "ALWAYS")
    protected EnumPlayerStandOnTop playerStandOnTop = EnumPlayerStandOnTop.ALWAYS;

    @Getter
    @Setter
    @PackFileProperty(configNames = "DefaultZoomLevel", required = false, defaultValue = "4")
    protected int defaultZoomLevel = 4;

    /* == Physics properties == */

    @Getter
    @Setter
    @PackFileProperty(configNames = "EmptyMass")
    protected int emptyMass;
    @Getter
    @Setter
    @PackFileProperty(configNames = "CenterOfGravityOffset", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false)
    protected Vector3f centerOfMass;

    @Getter
    @Setter
    @PackFileProperty(configNames = "DragCoefficient", required = false)
    protected float dragFactor;

    @Getter
    @Setter
    @PackFileProperty(configNames = "LinearDamping", required = false, defaultValue = "0.5 for helicopters, 0 for others")
    protected float linearDamping;
    @Getter
    @Setter
    @PackFileProperty(configNames = "AngularDamping", required = false, defaultValue = "0.9 for helicopters, 0.5 for boats, 0 for others")
    protected float angularDamping;

    @PackFileProperty(configNames = "InWaterLinearDamping", required = false, defaultValue = "0.6")
    protected float inWaterLinearDamping = 0.6f;
    @PackFileProperty(configNames = "InWaterAngularDamping", required = false, defaultValue = "0.9 for helicopters, 0.6 for others")
    protected float inWaterAngularDamping = 0.6f;

    @Getter
    @Setter
    @PackFileProperty(configNames = "UseComplexCollisions", required = false, defaultValue = "true", description = "common.UseComplexCollisions")
    protected boolean useComplexCollisions = true;

    /**
     * The shapes of this vehicle, can be used for collisions
     */
    @Getter
    protected ObjectCollisionsHelper collisionsHelper = new ObjectCollisionsHelper();

    /**
     * The friction points of this vehicle
     */
    protected final List<FrictionPoint> frictionPoints = new ArrayList<>();

    /* == Render properties == */

    @PackFileProperty(configNames = "ShapeYOffset", required = false)
    protected float shapeYOffset;

    @PackFileProperty(configNames = "ScaleModifier", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false,
            defaultValue = "1 1 1")
    protected Vector3f scaleModifier = new Vector3f(1, 1, 1);

    @Setter
    @PackFileProperty(configNames = "RenderDistanceSquared", required = false, defaultValue = "-1")
    protected float renderDistance = -1;

    /**
     * The particle emitters of this vehicle
     */
    protected final List<ParticleEmitterInfo<?>> particleEmitters = new ArrayList<>();

    /**
     * The light sources of this vehicle
     */
    protected final Map<String, PartLightSource> lightSources = new HashMap<>();

    /**
     * Maps the metadata to the texture data
     */
    private MaterialVariantsInfo<ModularVehicleInfo> variants;

    protected SceneNode<?, ?> sceneGraph;

    @Deprecated
    @PackFileProperty(configNames = "Textures", required = false, type = DefinitionType.DynamXDefinitionTypes.STRING_ARRAY_2D)
    private String[][] texturesArray;


    public ModularVehicleInfo(String packName, String fileName, VehicleValidator validator) {
        super(packName, fileName);
        this.validator = validator;
        this.validator.initProperties(this);
    }

    @Override
    public boolean postLoad(boolean hot) {
        DxModelPath modelPath = DynamXUtils.getModelPath(getPackName(), model);
        collisionsHelper.loadCollisions(this, modelPath, "chassis", centerOfMass, shapeYOffset, useComplexCollisions, scaleModifier, ObjectCollisionsHelper.CollisionType.VEHICLE);

        //Attach wheels and verify handbrake (V. 2.13.5)
        Map<String, PartWheelInfo> wheels = DynamXObjectLoaders.WHEELS.getInfos();
        boolean hasHandbrake = false;
        int directingWheel = -1;
        List<PartWheel> partsByType = getPartsByType(PartWheel.class);
        for (int i = 0; i < partsByType.size(); i++) {
            PartWheel partWheel = partsByType.get(i);
            partWheel.setDefaultWheelInfo(wheels.get(partWheel.getDefaultWheelName()));
            if (partWheel.isHandBrakingWheel())
                hasHandbrake = true;
            if (directingWheel == -1 && partWheel.isWheelIsSteerable())
                directingWheel = i;
        }
        if (directingWheel == -1)
            directingWheel = 0;
        this.directingWheel = directingWheel;
        if (!hasHandbrake) {
            for (PartWheel partWheel : partsByType) {
                if (!partWheel.isDrivingWheel())
                    partWheel.setHandBrakingWheel(true);
            }
        }
        //Attach engine
        if (defaultEngine != null) {
            BaseEngineInfo engine = DynamXObjectLoaders.ENGINES.findOrLoadInfo(defaultEngine, validator.getEngineClass());
            if (engine == null)
                throw new IllegalArgumentException("Engine " + defaultEngine + " of " + getFullName() + " was not found, check file names and previous loading errors !");
            engine.appendTo(this);
        }
        variants = getSubPropertyByType(MaterialVariantsInfo.class);
        //Map textures
        //Backward compatibility with 3.3.0
        //Will be removed
        if (texturesArray != null) {
            variants = new MaterialVariantsInfo(this, texturesArray);
            variants.appendTo(this);
        }
        //Map lights
        lightSources.values().forEach(PartLightSource::postLoad);
        //Post-load sub-properties
        if (!super.postLoad(hot))
            return false;
        //Validate vehicle type
        validator.validate(this);
        return true;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public IDynamXItem<ModularVehicleInfo> createItem(InfoList<ModularVehicleInfo> loader) {
        CreatePackItemEvent.VehicleItem<ModularVehicleInfo, ?> event = new CreatePackItemEvent.VehicleItem(loader, this);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isOverridden()) {
            return event.getObjectItem();
        } else {
            return validator.getSpawnItem(this);
        }
    }

    @Override
    public void addModules(PackPhysicsEntity<?, ?> entity, ModuleListBuilder modules) {
        getSubProperties().forEach(sub -> sub.addModules(entity, modules));
        getAllParts().forEach(sub -> sub.addModules(entity, modules));
        getLightSources().values().forEach(compoundLight -> compoundLight.addModules(entity, modules));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void applyItemTransforms(ItemCameraTransforms.TransformType renderType, ItemStack stack, ItemDxModel model, Matrix4f transform) {
        super.applyItemTransforms(renderType, stack, model, transform);
        if (renderType == ItemCameraTransforms.TransformType.GUI)
            transform.rotate((float) Math.PI, 0, 1, 0);
    }

    @Override
    public <A extends InteractivePart<?, ?>> List<A> getInteractiveParts() {
        return (List<A>) getPartsByType(InteractivePart.class);
    }

    @Override
    public ItemStack getPickedResult(int metadata) {
        return new ItemStack((Item) getItems()[0], 1, metadata);
    }

    @Override
    public SceneNode<?, ?> getSceneGraph() {
        if (sceneGraph == null) {
            if (isModelValid()) {
                BuildSceneGraphEvent.BuildEntityScene event = new BuildSceneGraphEvent.BuildEntityScene(this, (List) getDrawableParts(), getScaleModifier());
                MinecraftForge.EVENT_BUS.post(event);
                sceneGraph = event.getSceneGraphResult();
            } else
                sceneGraph = new EntityNode<>(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        }
        return sceneGraph;
    }

    @Override
    public PartLightSource getLightSource(String objectName) {
        return lightSources.get(objectName);
    }

    public byte getIdForVariant(String variantName) {
        if (variants != null) {
            for (byte i = 0; i < variants.getVariantsMap().size(); i++) {
                if (variants.getVariantsMap().get(i).getName().equalsIgnoreCase(variantName))
                    return i;
            }
        }
        return 0;
    }

    public String getVariantName(byte variantId) {
        if (variants != null) {
            return variants.getVariantsMap().getOrDefault(variantId, variants.getDefaultVariant()).getName();
        }
        return "default";
    }

    @Override
    public String getIconFileName(byte metadata) {
        return variants != null ? variants.getVariantsMap().get(metadata).getName() : super.getIconFileName(metadata);
    }

    @Override
    public IModelTextureVariantsSupplier.IModelTextureVariants getTextureVariantsFor(ObjObjectRenderer objObjectRenderer) {
        PartLightSource src = getLightSource(objObjectRenderer.getObjObjectData().getName());
        if (src != null)
            return src;
        return getVariants();
    }

    @Override
    public boolean hasVaryingTextures() {
        return getVariants() != null;
    }

    public int getMaxTextureMetadata() {
        return hasVaryingTextures() ? getVariants().getVariantsMap().size() : 1;
    }

    @Override
    public String getTranslationKey(IDynamXItem<ModularVehicleInfo> item, int itemMeta) {
        if (itemMeta == 0)
            return super.getTranslationKey(item, itemMeta);
        TextureVariantData textureInfo = variants.getVariantsMap().get((byte) itemMeta);
        return super.getTranslationKey(item, itemMeta) + "_" + textureInfo.getName().toLowerCase();
    }

    @Override
    public String getTranslatedName(IDynamXItem<ModularVehicleInfo> item, int itemMeta) {
        if (itemMeta == 0)
            return super.getTranslatedName(item, itemMeta);
        TextureVariantData textureInfo = variants.getVariantsMap().get((byte) itemMeta);
        return super.getTranslatedName(item, itemMeta) + " " + textureInfo.getName();
    }

    @Override
    public String toString() {
        return "ModularVehicleInfo named " + getFullName();
    }

    public void addFrictionPoint(FrictionPoint frictionPoint) {
        frictionPoints.add(frictionPoint);
    }

    @Override
    public void addParticleEmitter(ParticleEmitterInfo<?> particleEmitterInfo) {
        particleEmitters.add(particleEmitterInfo);
    }

    /**
     * Adds a light source to this vehicle
     *
     * @param source The light source to add
     */
    @Override
    public void addLightSource(PartLightSource source) {
        lightSources.put(source.getObjectName(), source);
        addDrawablePart(source);
    }

    @Override
    public float getBaseItemScale() {
        return 0.2f;
    }
}