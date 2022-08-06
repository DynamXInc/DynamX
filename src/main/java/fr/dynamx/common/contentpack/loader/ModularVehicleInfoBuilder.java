package fr.dynamx.common.contentpack.loader;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.IShapedObject;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.render.Enum3DRenderLocation;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.obj.ObjModelPath;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.ModularVehicleInfo;
import fr.dynamx.common.contentpack.parts.*;
import fr.dynamx.common.contentpack.type.PartWheelInfo;
import fr.dynamx.common.contentpack.type.ParticleEmitterInfo;
import fr.dynamx.common.contentpack.type.vehicle.EngineInfo;
import fr.dynamx.common.contentpack.type.vehicle.FrictionPoint;
import fr.dynamx.common.contentpack.type.vehicle.SoundListInfo;
import fr.dynamx.common.obj.texture.TextureData;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.physics.ShapeUtils;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Builder of {@link ModularVehicleInfo} <br>
 * Responsible for loading all the configuration/properties of the vehicle and creating a final object
 */
public class ModularVehicleInfoBuilder extends SubInfoTypeOwner.Vehicle implements IShapedObject, INamedObject, ParticleEmitterInfo.IParticleEmitterContainer {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = SubInfoTypeRegistries.WHEELED_VEHICLES)
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("UseHullShape".equals(key))
            return new IPackFilePropertyFixer.FixResult("UseComplexCollisions", true);
        return null;
    };

    private final String packName, fileName;

    @PackFileProperty(configNames = "Name")
    private String defaultName;
    @PackFileProperty(configNames = "Description")
    private String description;
    @PackFileProperty(configNames = "EmptyMass")
    private int emptyMass;
    @PackFileProperty(configNames = "DragCoefficient")
    private float dragFactor;
    @PackFileProperty(configNames = "Model", description = "common.model", defaultValue = "obj/name_of_vehicle/name_of_model.obj")
    private String model;
    @PackFileProperty(configNames = "ShapeYOffset", required = false)
    private float shapeYOffset;
    @PackFileProperty(configNames = {"CreativeTabName", "CreativeTab", "TabName"}, required = false, defaultValue = "CreativeTab of DynamX", description = "common.creativetabname")
    protected String creativeTabName;

    /**
     * The particle emitters of this vehicle
     */
    private final List<ParticleEmitterInfo<?>> particleEmitters = new ArrayList<>();
    /**
     * The parts of this vehicle (wheels, seats, doors...)
     */
    private final List<BasePart<?>> parts = new ArrayList<>();
    /**
     * The shapes of this vehicle, can be used for collisions
     */
    private final List<PartShape<?>> partShapes = new ArrayList<>();
    /**
     * The light sources of this vehicle
     */
    private final Map<String, PartLightSource.CompoundLight> lightSources = new HashMap<>();
    /**
     * The friction points of this vehicle
     */
    private final List<FrictionPoint> frictionPoints = new ArrayList<>();
    /**
     * The list of all rendered parts for this vehicle <br>
     * A rendered part will not be rendered with the main part of the obj model <br>
     * The {@link fr.dynamx.api.entities.modules.IPhysicsModule} using this part is responsible to render the part at the right location
     */
    private final List<String> renderedParts = new ArrayList<>();

    @PackFileProperty(configNames = "CenterOfGravityOffset", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F)
    private Vector3f centerOfMass;
    @PackFileProperty(configNames = "ScaleModifier", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false,
            defaultValue = "1 1 1")
    private final Vector3f scaleModifier = new Vector3f(1, 1, 1);

    @PackFileProperty(configNames = "DefaultEngine", required = false)
    private String defaultEngine;
    @PackFileProperty(configNames = "DefaultSounds", required = false)
    private String defaultSounds;

    @PackFileProperty(configNames = "ItemScale", required = false, description = "common.itemscale", defaultValue = "0.2")
    private final float itemScale = 0.2f;
    @PackFileProperty(configNames = "Item3DRenderLocation", required = false, description = "common.item3D", defaultValue = "all")
    private final Enum3DRenderLocation item3DRenderLocation = Enum3DRenderLocation.ALL;
    @PackFileProperty(configNames = "MaxVehicleSpeed", required = false, defaultValue = "infinite")
    private final float vehicleMaxSpeed = Integer.MAX_VALUE;
    @PackFileProperty(configNames = "UseComplexCollisions", required = false, defaultValue = "true", description = "common.UseComplexCollisions")
    private final boolean useHullShape = true;
    @PackFileProperty(configNames = "Textures", required = false, type = DefinitionType.DynamXDefinitionTypes.STRING_ARRAY_2D)
    private String[][] texturesArray;
    @PackFileProperty(configNames = "DefaultZoomLevel", required = false, defaultValue = "4")
    private final int defaultZoomLevel = 4;

    /**
     * The collision shape of this vehicle, generated either form the partShapes list, or the obj model of the vehicle (hull shape)
     */
    private CompoundCollisionShape physicsCollisionShape;
    /**
     * The debug buffer for the hull shape of the vehicle (generated from the obj model)
     */
    private List<Vector3f> collisionShapeDebugBuffer;
    /**
     * If something wrong happened building the vehicle (preventing it from loading)
     */
    private boolean errored;

    public ModularVehicleInfoBuilder(String packName, String fileName) {
        this.packName = packName;
        this.fileName = fileName;
    }

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

    @Override
    public void addPart(BasePart<?> partToAdd) {
        parts.add(partToAdd);
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

    /**
     * @param clazz The class of the parts to return
     * @param <T>   The type of the parts to return
     * @return All the parts of the given type
     */
    public <T extends BasePart<?>> List<T> getPartsByType(Class<T> clazz) {
        return (List<T>) this.parts.stream().filter(p -> clazz.equals(p.getClass())).collect(Collectors.toList());
    }

    @Override
    public void generateShape() {
        ObjModelPath modelPath = DynamXUtils.getModelPath(getPackName(), model);
        if (useHullShape)
            physicsCollisionShape = ShapeUtils.generateComplexModelCollisions(modelPath, "chassis", scaleModifier, centerOfMass, shapeYOffset);
        else {
            physicsCollisionShape = new CompoundCollisionShape();
            for (PartShape<?> partShape : getPartsByType(PartShape.class)) {
                BoxCollisionShape hullShape = new BoxCollisionShape(partShape.getScale());
                hullShape.setScale(scaleModifier);
                physicsCollisionShape.addChildShape(hullShape, new Vector3f(centerOfMass.x, shapeYOffset + centerOfMass.y, centerOfMass.z).add(partShape.getPosition()));
            }
        }
        collisionShapeDebugBuffer = ShapeUtils.getDebugVectorList(physicsCollisionShape, ShapeUtils.getDebugBuffer(physicsCollisionShape));
    }

    @Override
    public void setShapeErrored() {
        errored = true;
    }

    @Override
    public Vector3f getScaleModifier() {
        return scaleModifier;
    }

    /**
     * Creates a final {@link ModularVehicleInfo} from the properties of this builder
     *
     * @param wheels  The loaded wheels
     * @param engines The loaded engines
     * @param sounds  The loaded sounds
     * @return A new, fresh, vehicle
     */
    public ModularVehicleInfo<?> build(Map<String, PartWheelInfo> wheels, Map<String, EngineInfo> engines, Map<String, SoundListInfo> sounds) {
        //Attach wheels and verify handbrake (V. 2.13.5)
        boolean hasHandbrake = false;
        int directingWheel = -1;
        List<PartWheel> partsByType = getPartsByType(PartWheel.class);
        for (int i = 0; i < partsByType.size(); i++) {
            PartWheel partWheel = partsByType.get(i);
            System.out.println("Want wheel "+ partWheel.getDefaultWheelName()+" in "+ wheels);
            partWheel.setDefaultWheelInfo(this, wheels.get(partWheel.getDefaultWheelName()));
            if (partWheel.isHandBrakingWheel())
                hasHandbrake = true;
            if (directingWheel == -1 && partWheel.isWheelIsSteerable())
                directingWheel = i;
        }
        if (directingWheel == -1)
            directingWheel = 0;
        if (!hasHandbrake) {
            for (PartWheel partWheel : getPartsByType(PartWheel.class)) {
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
        Map<Byte, TextureData> bakedTextures = new HashMap<>();
        bakedTextures.put((byte) 0, new TextureData("Default", (byte) 0, getName()));
        if (texturesArray != null) {
            byte id = 1;
            for (String[] info : texturesArray) {
                bakedTextures.put(id, new TextureData(info[0], id, info[1]));
                id++;
            }
        }
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
        return new ModularVehicleInfo(defaultName, getPackName(), getName(), description, emptyMass, dragFactor, model, centerOfMass, scaleModifier, bakedTextures, parts, partShapes, subProperties, lightSources,
                frictionPoints, particleEmitters, vehicleMaxSpeed, directingWheel, itemScale, item3DRenderLocation, FMLCommonHandler.instance().getSide().isClient() ? renderedParts : null, physicsCollisionShape, collisionShapeDebugBuffer, creativeTabName, defaultZoomLevel);
    }

    @Override
    public String getName() {
        return fileName;
    }

    @Override
    public String toString() {
        return "ModularVehicleInfoBuilder{" +
                "packName='" + getPackName() + '\'' +
                ", fileName='" + getName() + '\'' +
                '}';
    }

    @Override
    public boolean isErrored() {
        return errored;
    }

    @Override
    public ModularVehicleInfo<?> build() {
        return build(DynamXObjectLoaders.WHEELS.infos, DynamXObjectLoaders.ENGINES.infos, DynamXObjectLoaders.SOUNDS.infos);
    }

    @Override
    public String getPackName() {
        return packName;
    }

    @Override
    public String getFullName() {
        return packName + "." + fileName;
    }

    public void addFrictionPoint(FrictionPoint frictionPoint) {
        frictionPoints.add(frictionPoint);
    }

    @Override
    public void addParticleEmitter(ParticleEmitterInfo<?> particleEmitterInfo) {
        particleEmitters.add(particleEmitterInfo);
    }

    @Override
    public List<ParticleEmitterInfo<?>> getParticleEmitters() {
        return particleEmitters;
    }
}