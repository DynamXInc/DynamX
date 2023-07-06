package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.IInfoOwner;
import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.IShapeContainer;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.api.events.CreatePackItemEvent;
import fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;
import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.client.renders.model.ItemDxModel;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;
import fr.dynamx.client.renders.model.texture.TextureVariantData;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.loader.ObjectLoader;
import fr.dynamx.common.contentpack.parts.ILightOwner;
import fr.dynamx.common.contentpack.parts.PartLightSource;
import fr.dynamx.common.contentpack.parts.PartShape;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.common.contentpack.type.MaterialVariantsInfo;
import fr.dynamx.common.contentpack.type.ParticleEmitterInfo;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.EnumPlayerStandOnTop;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.physics.ShapeUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;

/**
 * All information about a vehicle
 *
 * @see BaseVehicleEntity
 */
public class ModularVehicleInfo extends AbstractItemObject<ModularVehicleInfo, ModularVehicleInfo> implements IPhysicsPackInfo, IModelTextureVariantsSupplier,
        ParticleEmitterInfo.IParticleEmitterContainer, IModelPackObject, IPartContainer<ModularVehicleInfo>, IShapeContainer, ILightOwner<ModularVehicleInfo> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = SubInfoTypeRegistries.WHEELED_VEHICLES)
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("UseHullShape".equals(key))
            return new IPackFilePropertyFixer.FixResult("UseComplexCollisions", true);
        if ("Textures".equals(key))
            return new IPackFilePropertyFixer.FixResult("MaterialVariants", true, true);
        return null;
    };

    @Getter
    @Setter
    private VehicleValidator validator;

    /* == Pack properties == */

    @Getter
    @PackFileProperty(configNames = "DefaultEngine", required = false)
    protected String defaultEngine;
    @Getter
    @PackFileProperty(configNames = "DefaultSounds", required = false)
    protected String defaultSounds;

    @Getter
    @PackFileProperty(configNames = "MaxVehicleSpeed", required = false, defaultValue = "infinite")
    protected float vehicleMaxSpeed = Integer.MAX_VALUE;

    /**
     * The directing wheel id <br>
     * Used to render the steering wheel
     */
    @Getter
    private int directingWheel;

    @Getter
    @Setter
    @PackFileProperty(configNames = "PlayerStandOnTop", required = false, defaultValue = "ALWAYS")
    protected EnumPlayerStandOnTop playerStandOnTop;

    @Getter
    @Setter
    @PackFileProperty(configNames = "DefaultZoomLevel", required = false, defaultValue = "4")
    protected int defaultZoomLevel = 4;

    @Getter
    private final Map<Class<? extends BasePart<?>>, Byte> partIds = new HashMap<>();

    /* == Physics properties == */

    @Getter
    @Setter
    @PackFileProperty(configNames = "EmptyMass")
    protected int emptyMass = 0;
    @Getter
    @Setter
    @PackFileProperty(configNames = "CenterOfGravityOffset", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false)
    protected Vector3f centerOfMass = new Vector3f();

    @Getter
    @Setter
    @PackFileProperty(configNames = "DragCoefficient", required = false)
    protected float dragFactor = 0.0f;

    @PackFileProperty(configNames = "LinearDamping", required = false, defaultValue = "0")
    @Getter
    @Setter
    protected float linearDamping = 0.0f;
    @PackFileProperty(configNames = "AngularDamping", required = false, defaultValue = "0")
    @Getter
    @Setter
    protected float angularDamping = 0.0f;

    @Getter
    @Setter
    @PackFileProperty(configNames = "UseComplexCollisions", required = false, defaultValue = "true", description = "common.UseComplexCollisions")
    protected boolean useHullShape = true;

    /**
     * The collision shape of this vehicle, generated either form the partShapes list, or the obj model of the vehicle (hull shape)
     */
    @Getter
    @Setter
    private CompoundCollisionShape physicsCollisionShape;

    /**
     * The debug buffer for the hull shape of the vehicle (generated from the obj model)
     */
    private List<Vector3f> collisionShapeDebugBuffer;

    /**
     * The shapes of this vehicle, can be used for collisions
     */
    @Getter
    protected final List<PartShape<?>> partShapes = new ArrayList<>();

    /**
     * The friction points of this vehicle
     */
    @Getter
    protected final List<FrictionPoint> frictionPoints = new ArrayList<>();

    /* == Render properties == */

    @Getter
    @PackFileProperty(configNames = "ShapeYOffset", required = false)
    protected float shapeYOffset;

    @Getter
    @PackFileProperty(configNames = "ScaleModifier", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false,
            defaultValue = "1 1 1")
    protected Vector3f scaleModifier = new Vector3f(1, 1, 1);

    /**
     * The particle emitters of this vehicle
     */
    @Getter
    protected final List<ParticleEmitterInfo<?>> particleEmitters = new ArrayList<>();

    /**
     * The light sources of this vehicle
     */
    @Getter
    protected final Map<String, PartLightSource> lightSources = new HashMap<>();

    /**
     * The list of all rendered parts for this vehicle <br>
     * A rendered part will not be rendered with the main part of the obj model <br>
     * The {@link fr.dynamx.api.entities.modules.IPhysicsModule} using this part is responsible to render the part at the right location
     */
    @Getter
    private final List<String> renderedParts = new ArrayList<>();
    @Getter
    private final List<IDrawablePart<?>> drawableParts = new ArrayList<>();

    /**
     * Maps the metadata to the texture data
     */
    @Getter
    private MaterialVariantsInfo<ModularVehicleInfo> variants;

    @Getter
    @Deprecated
    @PackFileProperty(configNames = "Textures", required = false, type = DefinitionType.DynamXDefinitionTypes.STRING_ARRAY_2D)
    private String[][] texturesArray;


    public ModularVehicleInfo(String packName, String fileName, VehicleValidator validator) {
        super(packName, fileName);
        this.validator = validator;
        this.validator.initProperties(this);
        this.setItemScale(0.2f);
    }

    @Override
    public boolean postLoad(boolean hot) {
        DxModelPath modelPath = DynamXUtils.getModelPath(getPackName(), model);
        try {
            if (useHullShape)
                physicsCollisionShape = ShapeUtils.generateComplexModelCollisions(modelPath, "chassis", scaleModifier, centerOfMass, shapeYOffset);
            else {
                physicsCollisionShape = new CompoundCollisionShape();
                List<PartShape> partsByType = getPartsByType(PartShape.class);
                for (PartShape<?> partShape : partsByType) {
                    BoxCollisionShape hullShape = new BoxCollisionShape(partShape.getScale());
                    hullShape.setScale(scaleModifier);
                    physicsCollisionShape.addChildShape(hullShape, new Vector3f(centerOfMass.x, shapeYOffset + centerOfMass.y, centerOfMass.z).add(partShape.getPosition()));
                }
            }
        } catch (Exception e) {
            DynamXErrorManager.addError(getPackName(), DynamXErrorManager.PACKS_ERRORS, "collision_shape_error", ErrorLevel.FATAL, getName(), null, e);
            physicsCollisionShape = null;
            return false;
        }

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
    public IInfoOwner<ModularVehicleInfo> createOwner(ObjectLoader<ModularVehicleInfo, ?> loader) {
        CreatePackItemEvent.VehicleItem<ModularVehicleInfo, ?> event = new CreatePackItemEvent.VehicleItem(loader, this);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isOverridden()) {
            return (IInfoOwner<ModularVehicleInfo>) event.getObjectItem();
        } else {
            return (IInfoOwner<ModularVehicleInfo>) loader.getItem(this);
        }
    }

    public void addModules(BaseVehicleEntity<?> entity, ModuleListBuilder modules) {
        getSubProperties().forEach(sub -> sub.addModules(entity, modules));
        getAllParts().forEach(sub -> sub.addModules(entity, modules));
        getLightSources().values().forEach(compoundLight -> compoundLight.addModules(entity, modules));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void applyItemTransforms(ItemCameraTransforms.TransformType renderType, ItemStack stack, ItemDxModel model) {
        super.applyItemTransforms(renderType, stack, model);
        if (renderType == ItemCameraTransforms.TransformType.GUI)
            GlStateManager.rotate(180, 0, 1, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderItem3D(ItemStack item, ItemCameraTransforms.TransformType renderType) {
        DynamXRenderUtils.renderCar(this, (byte) item.getMetadata());
    }

    @Override
    public boolean canRenderPart(String partName) {
        return !renderedParts.contains(partName);
    }

    @Override
    public Collection<? extends IShapeInfo> getShapes() {
        return partShapes;
    }

    @Override
    public <A extends InteractivePart<?, ?>> List<A> getInteractiveParts() {
        return (List<A>) getPartsByType(InteractivePart.class);
    }

    @Override
    public ItemStack getPickedResult(int metadata) {
        return new ItemStack((Item) getOwners()[0], 1, metadata);
    }

    /**
     * @param clazz The class of the part to return
     * @param <A>   The type of the part to return
     * @return The part with the given type and the given id (wheel index for example), or null
     */
    public <A extends BasePart<ModularVehicleInfo>> A getPartByTypeAndId(Class<A> clazz, byte id) {
        return getPartsByType(clazz).stream().filter(t -> t.getId() == id).findFirst().orElse(null);
    }

    @Override
    public List<Vector3f> getCollisionShapeDebugBuffer() {
        if (collisionShapeDebugBuffer == null)
            collisionShapeDebugBuffer = ShapeUtils.getDebugVectorList(physicsCollisionShape, ShapeUtils.getDebugBuffer(physicsCollisionShape));
        return collisionShapeDebugBuffer;
    }

    @Override
    public PartLightSource getLightSource(String partName) {
        return lightSources.get(partName);
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
    public String getTranslationKey(IInfoOwner<ModularVehicleInfo> item, int itemMeta) {
        if (itemMeta == 0)
            return super.getTranslationKey(item, itemMeta);
        TextureVariantData textureInfo = variants.getVariantsMap().get((byte) itemMeta);
        return super.getTranslationKey(item, itemMeta) + "_" + textureInfo.getName().toLowerCase();
    }

    @Override
    public String getTranslatedName(IInfoOwner<ModularVehicleInfo> item, int itemMeta) {
        if (itemMeta == 0)
            return super.getTranslatedName(item, itemMeta);
        TextureVariantData textureInfo = variants.getVariantsMap().get((byte) itemMeta);
        return super.getTranslatedName(item, itemMeta) + " " + textureInfo.getName();
    }

    @Override
    public String toString() {
        return "ModularVehicleInfo named " + getFullName();
    }

    public void addCollisionShape(PartShape partShape) {
        partShapes.add(partShape);
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
        lightSources.put(source.getPartName(), source);
        addDrawablePart(source);
    }

    @Override
    public void addPart(BasePart<ModularVehicleInfo> part) {
        byte id = (byte) (partIds.getOrDefault(part.getClass(), (byte) -1) + 1);
        part.setId(id);
        partIds.put((Class<? extends BasePart<?>>) part.getClass(), id);
        super.addPart(part);
        if (part instanceof IDrawablePart)
            addDrawablePart((IDrawablePart<?>) part);
    }

    @Override
    public void addSubProperty(ISubInfoType<ModularVehicleInfo> property) {
        super.addSubProperty(property);
        if (property instanceof IDrawablePart)
            addDrawablePart((IDrawablePart<?>) property);
    }

    protected void addDrawablePart(IDrawablePart<?> part) {
        String[] names = part.getRenderedParts();
        if (names.length > 0)
            renderedParts.addAll(Arrays.asList(names));
        if (drawableParts.stream().noneMatch(p -> p.getClass() == part.getClass()))
            drawableParts.add(part);
    }
}