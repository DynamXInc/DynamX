package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IInfoOwner;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.IShapeContainer;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.api.events.CreatePackItemEvent;
import fr.dynamx.api.obj.IModelTextureVariantsSupplier;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;
import fr.dynamx.client.renders.model.texture.TextureVariantData;
import fr.dynamx.common.contentpack.loader.BuildableInfoLoader;
import fr.dynamx.common.contentpack.loader.ObjectLoader;
import fr.dynamx.common.contentpack.parts.PartLightSource;
import fr.dynamx.common.contentpack.parts.PartShape;
import fr.dynamx.common.contentpack.type.MaterialVariantsInfo;
import fr.dynamx.common.contentpack.type.ParticleEmitterInfo;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.utils.EnumPlayerStandOnTop;
import fr.dynamx.utils.client.DynamXRenderUtils;
import lombok.Getter;
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
 * @param <T> The implementing class type
 * @see ModularVehicleInfoBuilder
 * @see BaseVehicleEntity
 */
public class ModularVehicleInfo<T extends ModularVehicleInfo<?>> extends AbstractItemObject<T, ModularVehicleInfoBuilder> implements IPhysicsPackInfo, IModelTextureVariantsSupplier,
        ParticleEmitterInfo.IParticleEmitterContainer {
    private final int emptyMass;
    private final float dragFactor;
    private final Vector3f centerOfMass;
    private final Vector3f scaleModifier;

    /**
     * Maps the metadata to the texture data
     */
    @Getter
    private final MaterialVariantsInfo<?> variants;
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
    private final EnumPlayerStandOnTop playerStandOnTop;

    public ModularVehicleInfo(ModularVehicleInfoBuilder builder, int directingWheel, List<String> renderedParts) {
        super(builder.getPackName(), builder.getName());
        this.setDefaultName(builder.defaultName);
        this.setDescription(builder.description);

        this.model = builder.model;
        this.itemScale = builder.itemScale;
        this.item3DRenderLocation = builder.item3DRenderLocation;
        this.creativeTabName = builder.creativeTabName;

        this.emptyMass = builder.emptyMass;
        this.dragFactor = builder.dragFactor;
        this.centerOfMass = builder.centerOfMass;
        this.scaleModifier = builder.scaleModifier;

        this.variants = builder.getSubPropertyByType(MaterialVariantsInfo.class);

        this.parts = (List) builder.parts;
        this.partShapes = builder.partShapes;
        this.subProperties = (List) builder.getSubProperties();
        this.renderedParts = renderedParts;
        this.lightSources = builder.lightSources;
        this.frictionPoints = builder.frictionPoints;
        this.particleEmitters = builder.particleEmitters;

        this.vehicleMaxSpeed = builder.vehicleMaxSpeed;
        this.directingWheel = directingWheel;
        this.defaultZoomLevel = builder.defaultZoomLevel;

        this.physicsCollisionShape = builder.physicsCollisionShape;
        this.collisionShapeDebugBuffer = builder.collisionShapeDebugBuffer;
        this.playerStandOnTop = builder.playerStandOnTop;
    }

    public void addModules(BaseVehicleEntity<?> entity, ModuleListBuilder modules) {
        getSubProperties().forEach(sub -> sub.addModules(entity, modules));
        getAllParts().forEach(sub -> sub.addModules(entity, modules));
        getLightSources().forEach(compoundLight -> compoundLight.getSources().forEach(sub -> sub.addModules(entity, modules)));
    }

    public <A extends InteractivePart<?, ModularVehicleInfoBuilder>> List<A> getInteractiveParts() {
        return (List<A>) getPartsByType(InteractivePart.class);
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
        return variants != null ? variants.getVariantsMap().size() : 1;
    }

    public float getVehicleMaxSpeed() {
        return vehicleMaxSpeed;
    }

    public int getDirectingWheel() {
        return directingWheel;
    }

    public byte getIdForVariant(String variantName) {
        if(variants != null) {
            for (byte i = 0; i < variants.getVariantsMap().size(); i++) {
                if (variants.getVariantsMap().get(i).getName().equalsIgnoreCase(variantName))
                    return i;
            }
        }
        return 0;
    }

    public String getVariantName(byte variantId) {
        if(variants != null) {
            return variants.getVariantsMap().getOrDefault(variantId, variants.getDefaultVariant()).getName();
        }
        return "default";
    }

    @Override
    public void renderItem3D(ItemStack item, ItemCameraTransforms.TransformType renderType) {
        DynamXRenderUtils.renderCar(this, (byte) item.getMetadata());
    }

    public int getEmptyMass() {
        return emptyMass;
    }

    public EnumPlayerStandOnTop getPlayerStandOnTop() {
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
     * @param clazz The class of the part to return
     * @param <A>   The type of the part to return
     * @return The part with the given type and the given id (wheel index for example), or null
     */
    public <A extends BasePart<ModularVehicleInfoBuilder>> A getPartByTypeAndId(Class<A> clazz, byte id) {
        return getPartsByType(clazz).stream().filter(t -> t.getId() == id).findFirst().orElse(null);
    }

    /**
     * @param clazz The class of the ISubInfoTypes to returns
     * @param <A>   The type of the ISubInfoTypes to return
     * @return All the ISubInfoTypes of the given type
     */
    @Override
    public <A extends ISubInfoType<ModularVehicleInfoBuilder>> A getSubPropertyByType(Class<A> clazz) {
        return (A) this.subProperties.stream().filter(p -> clazz.equals(p.getClass())).findFirst().orElseGet(() -> null); //Don't remove the () -> : idea don't understand it
    }

    @Override
    public void addSubProperty(ISubInfoType<ModularVehicleInfoBuilder> property) {
        //Done in ModularVehicleInfoBuilder
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

    @Override
    public String getIconFileName(byte metadata) {
        return variants != null ? variants.getVariantsMap().get(metadata).getName() : super.getIconFileName(metadata);
    }

    @Override
    public IModelTextureVariantsSupplier.IModelTextureVariants getTextureVariantsFor(ObjObjectRenderer objObjectRenderer) {
        PartLightSource.CompoundLight src = getLightSource(objObjectRenderer.getObjObjectData().getName());
        if (src != null)
            return src;
        return getVariants();
    }

    @Override
    public boolean hasVaryingTextures() {
        return getVariants() != null;
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
    public IInfoOwner<T> createOwner(ObjectLoader<T, ?, ?> loader) {
        CreatePackItemEvent.CreateVehicleItemEvent<T, ?> event = new CreatePackItemEvent.CreateVehicleItemEvent(loader, this);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isOverridden()) {
            return (IInfoOwner<T>) event.getSpawnItem();
        } else {
            return (IInfoOwner<T>) ((BuildableInfoLoader<?, T, ?>) loader).getItem((T) this);
        }
    }

    @Override
    public String getTranslationKey(IInfoOwner<T> item, int itemMeta) {
        if (itemMeta == 0 || variants == null)
            return super.getTranslationKey(item, itemMeta);
        TextureVariantData textureInfo = variants.getVariantsMap().get((byte) itemMeta);
        return super.getTranslationKey(item, itemMeta) + "_" + textureInfo.getName().toLowerCase();
    }

    @Override
    public String getTranslatedName(IInfoOwner<T> item, int itemMeta) {
        if (itemMeta == 0 || variants == null)
            return super.getTranslatedName(item, itemMeta);
        TextureVariantData textureInfo = variants.getVariantsMap().get((byte) itemMeta);
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
        return "ModularVehicleInfo named " + getFullName();
    }

    @Override
    public List<BasePart<ModularVehicleInfoBuilder>> getAllParts() {
        return parts;
    }

    @Override
    public void addPart(BasePart<ModularVehicleInfoBuilder> uBasePart) {
        //Done in ModularVehicleInfoBuilder
    }
}