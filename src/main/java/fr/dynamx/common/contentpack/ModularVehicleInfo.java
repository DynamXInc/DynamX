package fr.dynamx.common.contentpack;

import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IInfoOwner;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.object.render.Enum3DRenderLocation;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.api.events.CreatePackItemEvent;
import fr.dynamx.api.obj.IModelTextureSupplier;
import fr.dynamx.api.obj.IObjObject;
import fr.dynamx.common.contentpack.loader.BuildableInfoLoader;
import fr.dynamx.common.contentpack.loader.ModularVehicleInfoBuilder;
import fr.dynamx.common.contentpack.loader.ObjectLoader;
import fr.dynamx.common.contentpack.parts.PartLightSource;
import fr.dynamx.common.contentpack.parts.PartShape;
import fr.dynamx.common.contentpack.type.ParticleEmitterInfo;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.common.contentpack.type.vehicle.FrictionPoint;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.items.ItemModularEntity;
import fr.dynamx.common.obj.texture.TextureData;
import fr.dynamx.utils.client.DynamXRenderUtils;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * All information about a vehicle
 *
 * @param <U> The implementing class type
 * @see ModularVehicleInfoBuilder
 * @see BaseVehicleEntity
 */
public class ModularVehicleInfo<U extends ModularVehicleInfo<?>> extends AbstractItemObject<U> implements IPhysicsPackInfo, IModelTextureSupplier,
        ParticleEmitterInfo.IParticleEmitterContainer {
    private final int emptyMass;
    private final String playerStandOnTop;
    private final float dragFactor;
    private final Vector3f centerOfMass;
    private final Vector3f scaleModifier;

    /**
     * Maps the metadata to the texture data
     */
    private final Map<Byte, TextureData> textures;
    /**
     * The number of textures available for the vehicle
     */
    private final int maxTextureMetadata;
    /**
     * The parts of this vehicle (wheels, seats, doors...)
     */
    private final List<BasePart<ModularVehicleInfoBuilder>> parts;
    /**
     * The shapes of this vehicle, can be used for collisions
     */
    private final List<PartShape<?>> partShapes;
    /**
     * List of owned {@link ISubInfoType}s
     */
    private final List<ISubInfoType<ModularVehicleInfoBuilder>> subProperties;
    /**
     * The list of all rendered parts for this vehicle <br>
     * A rendered part will not be rendered with the main part of the obj model <br>
     * The {@link fr.dynamx.api.entities.modules.IPhysicsModule} using this part is responsible to render the part at the right location
     */
    private final List<String> renderedParts;
    /**
     * The light sources of this vehicle
     */
    private final Map<String, PartLightSource.CompoundLight> lightSources;
    /**
     * The friction points of this vehicle
     */
    private final List<FrictionPoint> frictionPoints;
    /**
     * The particle emitters of this vehicle
     */
    private final List<ParticleEmitterInfo<?>> particleEmitters;
    /**
     * Vehicle max speed in km/h
     */
    private final float vehicleMaxSpeed;
    /**
     * The directing wheel id <br>
     * Used to render the steering wheel
     */
    private final int directingWheel;
    /**
     * Default zoom level in third person view
     */
    private final int defaultZoomLevel;
    /**
     * The collision shape of this vehicle, generated either form the partShapes list, or the obj model of the vehicle (hull shape)
     */
    private final CompoundCollisionShape physicsCollisionShape;
    /**
     * The debug buffer for the hull shape of the vehicle (generated from the obj model)
     */
    private final List<Vector3f> collisionShapeDebugBuffer;

    public ModularVehicleInfo(String defaultName, String packName, String fileName, String description, int emptyMass, String playerStandOnTop, float dragFactor, String model, Vector3f centerOfMass,
                              Vector3f scaleModifier, Map<Byte, TextureData> textures, List<BasePart<ModularVehicleInfoBuilder>> parts, List<PartShape<?>> partShapes,
                              List<ISubInfoType<ModularVehicleInfoBuilder>> subProperties, Map<String, PartLightSource.CompoundLight> lightSources, List<FrictionPoint> frictionPoints,
                              List<ParticleEmitterInfo<?>> particleEmitters, float vehicleMaxSpeed, int directingWheel, float itemScale, Enum3DRenderLocation item3DRenderLocation,
                              List<String> renderedParts, CompoundCollisionShape collisionShape, List<Vector3f> debugBuffer, String creativeTabName, int defaultZoomLevel) {
        super(packName, fileName);
        this.frictionPoints = frictionPoints;
        this.particleEmitters = particleEmitters;
        this.vehicleMaxSpeed = vehicleMaxSpeed;
        this.directingWheel = directingWheel;
        this.renderedParts = renderedParts;
        this.setDefaultName(defaultName);
        this.lightSources = lightSources;
        this.physicsCollisionShape = collisionShape;
        this.collisionShapeDebugBuffer = debugBuffer;

        this.setDescription(description);

        this.scaleModifier = scaleModifier;

        this.emptyMass = emptyMass;
        this.playerStandOnTop = playerStandOnTop;
        this.dragFactor = dragFactor;
        this.model = model;
        this.centerOfMass = centerOfMass;

        this.textures = textures;
        this.parts = parts;
        this.partShapes = partShapes;
        this.subProperties = subProperties;

        this.itemScale = itemScale;
        this.item3DRenderLocation = item3DRenderLocation;

        int texCount = 0;
        for (TextureData data : textures.values()) {
            if (data.isItem())
                texCount++;
        }
        this.maxTextureMetadata = texCount;

        this.creativeTabName = creativeTabName;
        this.defaultZoomLevel = defaultZoomLevel;
    }

    public void addModules(BaseVehicleEntity<?> entity, ModuleListBuilder modules) {
        getSubProperties().forEach(sub -> sub.addModules(entity, modules));
        getParts().forEach(sub -> sub.addModules(entity, modules));
        getLightSources().forEach(compoundLight -> compoundLight.getSources().forEach(sub -> sub.addModules(entity, modules)));
    }

    public List<BasePart<ModularVehicleInfoBuilder>> getParts() {
        return parts;
    }

    public <T extends InteractivePart<?, ModularVehicleInfoBuilder>> List<T> getInteractiveParts() {
        return (List<T>) getPartsByType(InteractivePart.class);
    }

    public List<PartShape<?>> getPartShapes() {
        return partShapes;
    }

    public PartLightSource.CompoundLight getLightSource(String partName) {
        return lightSources.get(partName);
    }

    public Collection<PartLightSource.CompoundLight> getLightSources() {
        return lightSources.values();
    }

    public int getMaxTextureMetadata() {
        return maxTextureMetadata;
    }

    public float getVehicleMaxSpeed() {
        return vehicleMaxSpeed;
    }

    public int getDirectingWheel() {
        return directingWheel;
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

    public int getEmptyMass() {
        return emptyMass;
    }

    public String getPlayerStandOnTop() {
        return playerStandOnTop;
    }

    public float getDragFactor() {
        return dragFactor;
    }

    @Override
    public Vector3f getCenterOfMass() {
        return centerOfMass;
    }

    @Override
    public Collection<? extends IShapeInfo> getShapes() {
        return partShapes;
    }

    /**
     * @param clazz The class of the parts to return
     * @param <T> The type of the parts to return
     * @return All the parts of the given type
     */
    public <T extends BasePart<ModularVehicleInfoBuilder>> List<T> getPartsByType(Class<T> clazz) {
        return (List<T>) this.parts.stream().filter(p -> clazz.isAssignableFrom(p.getClass())).collect(Collectors.toList());
    }

    /**
     * @param clazz The class of the part to return
     * @param <T> The type of the part to return
     * @return The part with the given type and the given id (wheel index for example), or null
     */
    public <T extends BasePart<ModularVehicleInfoBuilder>> T getPartByTypeAndId(Class<T> clazz, byte id) {
        return getPartsByType(clazz).stream().filter(t -> t.getId() == id).findFirst().orElse(null);
    }

    /**
     * @param clazz The class of the ISubInfoTypes to returns
     * @param <T> The type of the ISubInfoTypes to return
     * @return All the ISubInfoTypes of the given type
     */
    public <T extends ISubInfoType<ModularVehicleInfoBuilder>> T getSubPropertyByType(Class<T> clazz) {
        return (T) this.subProperties.stream().filter(p -> clazz.equals(p.getClass())).findFirst().orElseGet(() -> null); //Don't remove the () -> : idea don't understand it
    }

    public List<ISubInfoType<ModularVehicleInfoBuilder>> getSubProperties() {
        return subProperties;
    }

    public CompoundCollisionShape getPhysicsCollisionShape() {
        return physicsCollisionShape;
    }

    public List<Vector3f> getCollisionShapeDebugBuffer() {
        return collisionShapeDebugBuffer;
    }

    public Map<Byte, TextureData> getTextures() {
        return textures;
    }

    @Override
    public String getIconFileName(byte metadata) {
        return getTextures().get(metadata).getIconName();
    }

    @Override
    public Map<Byte, TextureData> getTexturesFor(IObjObject object) {
        PartLightSource.CompoundLight src = getLightSource(object.getName());
        if(src != null) {
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

    public Vector3f getScaleModifier() {
        return scaleModifier;
    }

    public int getDefaultZoomLevel() {
        return defaultZoomLevel;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public IInfoOwner<U> createOwner(ObjectLoader<U, ?, ?> loader) {
        CreatePackItemEvent.CreateVehicleItemEvent event = new CreatePackItemEvent.CreateVehicleItemEvent((ObjectLoader<ModularVehicleInfo<?>, ItemModularEntity<ModularVehicleInfo<?>>, ModularVehicleInfoBuilder>) loader, this);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isOverridden()) {
            return (ItemModularEntity<U>) event.getSpawnItem();
        } else {
            return (IInfoOwner<U>) ((BuildableInfoLoader<?, U, ?>) loader).getItem((U) this);
        }
    }

    @Override
    public String getTranslationKey(IInfoOwner<U> item, int itemMeta) {
        if (itemMeta == 0)
            return super.getTranslationKey(item, itemMeta);
        TextureData textureInfo = getTextures().get((byte) itemMeta);
        return super.getTranslationKey(item, itemMeta) + "_" + textureInfo.getName().toLowerCase();
    }

    @Override
    public String getTranslatedName(IInfoOwner<U> item, int itemMeta) {
        if (itemMeta == 0)
            return super.getTranslatedName(item, itemMeta);
        TextureData textureInfo = getTextures().get((byte) itemMeta);
        return super.getTranslatedName(item, itemMeta) + " " + textureInfo.getName();
    }

    public List<FrictionPoint> getFrictionPoints() {
        return frictionPoints;
    }

    @Override
    public List<ParticleEmitterInfo<?>> getParticleEmitters() {
        return particleEmitters;
    }

    @Override
    public String toString() {
        return "ModularVehicleInfo named "+getFullName();
    }
}