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
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.api.events.CreatePackItemEvent;
import fr.dynamx.api.obj.IModelTextureSupplier;
import fr.dynamx.api.obj.IObjObject;
import fr.dynamx.api.obj.ObjModelPath;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.loader.ObjectLoader;
import fr.dynamx.common.contentpack.parts.*;
import fr.dynamx.common.contentpack.type.ParticleEmitterInfo;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.obj.texture.TextureData;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.EnumPlayerStandOnTop;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.physics.ShapeUtils;
import lombok.Getter;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import java.util.*;

/**
 * All information about a vehicle
 *
 * @see BaseVehicleEntity
 */
//TODO CLEAN THIS CLASS
public class ModularVehicleInfo extends AbstractItemObject<ModularVehicleInfo, ModularVehicleInfo> implements IPhysicsPackInfo, IModelTextureSupplier,
        ParticleEmitterInfo.IParticleEmitterContainer, IPartContainer<ModularVehicleInfo>, IShapeContainer {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = SubInfoTypeRegistries.WHEELED_VEHICLES)
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("UseHullShape".equals(key))
            return new IPackFilePropertyFixer.FixResult("UseComplexCollisions", true);
        return null;
    };

    @Getter
    @PackFileProperty(configNames = "EmptyMass")
    protected int emptyMass;
    @Getter
    @PackFileProperty(configNames = "DragCoefficient")
    protected float dragFactor;
    @Getter
    @PackFileProperty(configNames = "PlayerStandOnTop", required = false, defaultValue = "ALWAYS")
    protected EnumPlayerStandOnTop playerStandOnTop;
    @Getter
    @PackFileProperty(configNames = "ShapeYOffset", required = false)
    protected float shapeYOffset;

    /**
     * The particle emitters of this vehicle
     */
    @Getter
    protected final List<ParticleEmitterInfo<?>> particleEmitters = new ArrayList<>();
    /**
     * The shapes of this vehicle, can be used for collisions
     */
    @Getter
    protected final List<PartShape<?>> partShapes = new ArrayList<>();
    /**
     * The light sources of this vehicle
     */
    @Getter
    protected final Map<String, PartLightSource.CompoundLight> lightSources = new HashMap<>();
    /**
     * The friction points of this vehicle
     */
    @Getter
    protected final List<FrictionPoint> frictionPoints = new ArrayList<>();
    /**
     * The list of all rendered parts for this vehicle <br>
     * A rendered part will not be rendered with the main part of the obj model <br>
     * The {@link fr.dynamx.api.entities.modules.IPhysicsModule} using this part is responsible to render the part at the right location
     */
    @Getter
    private final List<String> renderedParts = new ArrayList<>();

    @Getter
    @PackFileProperty(configNames = "CenterOfGravityOffset", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F)
    protected Vector3f centerOfMass;
    @Getter
    @PackFileProperty(configNames = "ScaleModifier", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false,
            defaultValue = "1 1 1")
    protected Vector3f scaleModifier = new Vector3f(1, 1, 1);

    @Getter
    @PackFileProperty(configNames = "DefaultEngine", required = false)
    private String defaultEngine;
    @Getter
    @PackFileProperty(configNames = "DefaultSounds", required = false)
    private String defaultSounds;

    @Getter
    @PackFileProperty(configNames = "ItemScale", required = false, description = "common.itemscale", defaultValue = "0.2")
    protected float itemScale = 0.2f;
    @Getter
    @PackFileProperty(configNames = "MaxVehicleSpeed", required = false, defaultValue = "infinite")
    protected float vehicleMaxSpeed = Integer.MAX_VALUE;
    @Getter
    @PackFileProperty(configNames = "UseComplexCollisions", required = false, defaultValue = "true", description = "common.UseComplexCollisions")
    protected boolean useHullShape = true;
    @Getter
    @PackFileProperty(configNames = "Textures", required = false, type = DefinitionType.DynamXDefinitionTypes.STRING_ARRAY_2D)
    private String[][] texturesArray;
    @Getter
    @PackFileProperty(configNames = "DefaultZoomLevel", required = false, defaultValue = "4")
    protected int defaultZoomLevel = 4;

    /**
     * Maps the metadata to the texture data
     */
    @Getter
    private Map<Byte, TextureData> textures = new HashMap<>();
    /**
     * The number of textures available for the vehicle
     */
    @Getter
    private int maxTextureMetadata;

    /**
     * The directing wheel id <br>
     * Used to render the steering wheel
     */
    @Getter
    private int directingWheel;
    /**
     * The collision shape of this vehicle, generated either form the partShapes list, or the obj model of the vehicle (hull shape)
     */
    @Getter
    private CompoundCollisionShape physicsCollisionShape;
    /**
     * The debug buffer for the hull shape of the vehicle (generated from the obj model)
     */
    @Getter
    private List<Vector3f> collisionShapeDebugBuffer;

    public ModularVehicleInfo(String packName, String fileName) {
        super(packName, fileName);
    }

    public void addModules(BaseVehicleEntity<?> entity, ModuleListBuilder modules) {
        getSubProperties().forEach(sub -> sub.addModules(entity, modules));
        getAllParts().forEach(sub -> sub.addModules(entity, modules));
        getLightSources().values().forEach(compoundLight -> compoundLight.getSources().forEach(sub -> sub.addModules(entity, modules)));
    }

    @Override
    public Collection<? extends IShapeInfo> getShapes() {
        return partShapes;
    }

    @Override
    public <A extends InteractivePart<?, ?>> List<A> getInteractiveParts() {
        return (List<A>) getPartsByType(InteractivePart.class);
    }

    public PartLightSource.CompoundLight getLightSource(String partName) {
        return lightSources.get(partName);
    }

    public byte getIdForTexture(String textureName) {
        for (byte i = 0; i < textures.size(); i++) {
            if (textures.get(i).getName().equalsIgnoreCase(textureName))
                return i;
        }
        return 0;
    }

    @Override
    public void renderItem3D(ItemStack item, ItemCameraTransforms.TransformType renderType) {
        DynamXRenderUtils.renderCar(this, (byte) item.getMetadata());
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
    public String getIconFileName(byte metadata) {
        return getTextures().get(metadata).getIconName();
    }

    @Override
    public Map<Byte, TextureData> getTexturesFor(IObjObject object) {
        PartLightSource.CompoundLight src = getLightSource(object.getName());
        if (src != null) {
            Map<Byte, TextureData> ret = new HashMap<>();
            ret.put((byte) 0, new TextureData("Default", (byte) 0));
            List<PartLightSource> sources = src.getSources();
            for (PartLightSource source : sources) {
                for (TextureData textureData : source.getTextureMap().values()) {
                    ret.put(textureData.getId(), textureData);
                }
            }
            return ret;
        }
        return getTextures();
    }

    @Override
    public boolean hasCustomTextures() {
        return getTextures().size() > 1;
    }

    @Override
    public boolean canRenderPart(String partName) {
        return !renderedParts.contains(partName);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public IInfoOwner<ModularVehicleInfo> createOwner(ObjectLoader<ModularVehicleInfo, ?> loader) {
        CreatePackItemEvent.CreateVehicleItemEvent<ModularVehicleInfo, ?> event = new CreatePackItemEvent.CreateVehicleItemEvent(loader, this);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isOverridden()) {
            return (IInfoOwner<ModularVehicleInfo>) event.getSpawnItem();
        } else {
            return (IInfoOwner<ModularVehicleInfo>) loader.getItem(this);
        }
    }

    @Override
    public String getTranslationKey(IInfoOwner<ModularVehicleInfo> item, int itemMeta) {
        if (itemMeta == 0)
            return super.getTranslationKey(item, itemMeta);
        TextureData textureInfo = getTextures().get((byte) itemMeta);
        return super.getTranslationKey(item, itemMeta) + "_" + textureInfo.getName().toLowerCase();
    }

    @Override
    public String getTranslatedName(IInfoOwner<ModularVehicleInfo> item, int itemMeta) {
        if (itemMeta == 0)
            return super.getTranslatedName(item, itemMeta);
        TextureData textureInfo = getTextures().get((byte) itemMeta);
        return super.getTranslatedName(item, itemMeta) + " " + textureInfo.getName();
    }

    @Override
    public String toString() {
        return "ModularVehicleInfo named " + getFullName();
    }

    // Methods from ModularVehicleInfoBuilder

    private byte seatID, wheelID, doorID;

    public void arrangeSeatID(PartSeat seat) {
        seat.setId(seatID++);
    }

    public void arrangeDoorID(PartDoor door) {
        door.setId(doorID++);
    }

    public void arrangeWheelID(PartWheel wheel) {
        wheel.setId(wheelID++);
    }

    public void addCollisionShape(PartShape partShape) {
        partShapes.add(partShape);
    }

    /**
     * Prevents the added parts from being rendered with the main obj model of the vehicle <br>
     * The {@link fr.dynamx.api.entities.modules.IPhysicsModule} using this part is responsible to render the part at the right location
     *
     * @param parts The parts to hide when rendering the main obj model
     */
    public void addRenderedParts(String... parts) {
        renderedParts.addAll(Arrays.asList(parts));
    }

    /**
     * Adds a light source to this vehicle
     *
     * @param source The light source to add
     */
    public void addLightSource(PartLightSource source) {
        if (lightSources.containsKey(source.getPartName()))
            lightSources.get(source.getPartName()).addSource(source);
        else
            lightSources.put(source.getPartName(), new PartLightSource.CompoundLight(source));
    }

    @Override
    public boolean postLoad(boolean hot) {
        return build(DynamXObjectLoaders.WHEELS.getInfos(), DynamXObjectLoaders.ENGINES.getInfos(), DynamXObjectLoaders.SOUNDS.getInfos());
    }

    /**
     * <p>
     * Creates a final {@link ModularVehicleInfo} from the properties of this builder
     *
     * @param wheels  The loaded wheels
     * @param engines The loaded engines
     * @param sounds  The loaded sounds
     * @return A new, fresh, vehicle
     */
    public boolean build(Map<String, PartWheelInfo> wheels, Map<String, EngineInfo> engines, Map<String, SoundListInfo> sounds) {
        ObjModelPath modelPath = DynamXUtils.getModelPath(getPackName(), model);
        try {
            if (useHullShape)
                physicsCollisionShape = ShapeUtils.generateComplexModelCollisions(modelPath, "chassis", scaleModifier, centerOfMass, shapeYOffset);
            else {
                physicsCollisionShape = new CompoundCollisionShape();
                List<PartShape> partsByType = getPartsByType(PartShape.class);
                for (PartShape partShape : partsByType) {
                    BoxCollisionShape hullShape = new BoxCollisionShape(partShape.getScale());
                    hullShape.setScale(scaleModifier);
                    physicsCollisionShape.addChildShape(hullShape, new Vector3f(centerOfMass.x, shapeYOffset + centerOfMass.y, centerOfMass.z).add(partShape.getPosition()));
                }
            }
            collisionShapeDebugBuffer = ShapeUtils.getDebugVectorList(physicsCollisionShape, ShapeUtils.getDebugBuffer(physicsCollisionShape));
        } catch (Exception e) {
            DynamXErrorManager.addError(getPackName(), DynamXErrorManager.PACKS__ERRORS, "collision_shape_error", ErrorLevel.FATAL, getName(), null, e);
            return false;
        }

        System.out.println("Search: " + wheels + " " + engines + " " + sounds);
        //Attach wheels and verify handbrake (V. 2.13.5)
        boolean hasHandbrake = false;
        int directingWheel = -1;
        List<PartWheel> partsByType = getPartsByType(PartWheel.class);
        for (int i = 0; i < partsByType.size(); i++) {
            PartWheel partWheel = partsByType.get(i);
            System.out.println("item " + defaultEngine + " " + partWheel.getDefaultWheelName() + " " + defaultSounds);
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
            for (PartWheel partWheel : this.getPartsByType(PartWheel.class)) {
                if (!partWheel.isDrivingWheel())
                    partWheel.setHandBrakingWheel(true);
            }
        }
        //Attach engine
        if (defaultEngine != null) {
            EngineInfo engine = engines.get(defaultEngine);
            if (engine == null)
                throw new IllegalArgumentException("Engine " + defaultEngine + " of " + getFullName() + " was not found, check file names and previous loading errors !");
            engine.appendTo(this);
            //And sounds
            if (defaultSounds != null) {
                SoundListInfo engineSound = sounds.get(defaultSounds);
                if (engineSound == null)
                    throw new IllegalArgumentException("Engine sounds " + defaultSounds + " of " + getFullName() + " were not found, check file names and previous loading errors !");
                engine.setSounds(engineSound.getSoundsIn());
            }
        }
        //Map textures
        Map<Byte, TextureData> bakedTextures = this.textures;
        int textureCount = 1;
        bakedTextures.put((byte) 0, new TextureData("Default", (byte) 0, getName()));
        if (texturesArray != null) {
            byte id = 1;
            for (String[] info : texturesArray) {
                TextureData variant = new TextureData(info[0], id, info[1]);
                bakedTextures.put(id, variant);
                id++;
                if (variant.isItem())
                    textureCount++;
            }
        }
        this.textures = bakedTextures;
        this.maxTextureMetadata = textureCount;
        //Map lights
        for (PartLightSource.CompoundLight src : lightSources.values()) {
            if (src != null) {
                List<PartLightSource> sources = src.getSources();
                for (int i = 0; i < sources.size(); i++) {
                    PartLightSource source = sources.get(i);
                    if (source.getTextures() != null) {
                        for (int j = 0; j < source.getTextures().length; j++) {
                            TextureData data = new TextureData(source.getTextures()[j], (byte) (1 + i + j));
                            source.mapTexture(j, data);
                        }
                    }
                }
            }
        }
        return true;
    }

    public void addFrictionPoint(FrictionPoint frictionPoint) {
        frictionPoints.add(frictionPoint);
    }

    @Override
    public void addParticleEmitter(ParticleEmitterInfo<?> particleEmitterInfo) {
        particleEmitters.add(particleEmitterInfo);
    }

    /*@Nullable
    @Override
    public ModularVehicleInfo<T> getOwner() {
        return null;
    }*/
}