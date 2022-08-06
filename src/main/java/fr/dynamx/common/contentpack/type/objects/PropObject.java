package fr.dynamx.common.contentpack.type.objects;

import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.ErrorTrackingService;
import fr.dynamx.api.contentpack.object.IInfoOwner;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.loader.ModularVehicleInfoBuilder;
import fr.dynamx.common.contentpack.loader.ObjectLoader;
import fr.dynamx.common.contentpack.loader.PackFilePropertyData;
import fr.dynamx.common.contentpack.loader.SubInfoTypeAnnotationCache;
import fr.dynamx.common.items.ItemProps;
import fr.dynamx.utils.DynamXLoadingTasks;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.physics.ShapeUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RegisteredSubInfoType(name = "prop", registries = SubInfoTypeRegistries.BLOCKS_AND_PROPS, strictName = false)
public class PropObject<T extends BlockObject<?>> extends AbstractProp<T> implements IPhysicsPackInfo, ISubInfoType<BlockObject<?>>, ISubInfoTypeOwner<BlockObject<?>> {
    private final BlockObject<?> owner;
    @PackFileProperty(configNames = "EmptyMass")
    private int emptyMass;
    @PackFileProperty(configNames = "CenterOfGravityOffset", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F)
    private Vector3f centerOfMass;
    @PackFileProperty(configNames = "SpawnOffset", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false, defaultValue = "0 0.65 0")
    private final Vector3f spawnOffset = new Vector3f(0, 0.65f, 0);
    @PackFileProperty(configNames = "ContinuousCollisionDetection", required = false, defaultValue = "false")
    private boolean isCCDEnabled;
    @PackFileProperty(configNames = "Friction", required = false, defaultValue = "0.5")
    private final float friction = 0.5f;
    @PackFileProperty(configNames = "Margin", required = false, defaultValue = "0.04")
    private final float margin = 0.04f;
    @PackFileProperty(configNames = "DespawnTime", required = false, defaultValue = "\"-1\" (disabled)")
    private final float despawnTime = -1;
    @PackFileProperty(configNames = "Damping", required = false, defaultValue = "0")
    private float dampingFactor;
    @PackFileProperty(configNames = "Bounciness", required = false, defaultValue = "0")
    private float restitutionFactor;
    private List<Vector3f> debugBuffer;

    public PropObject(String packName, String fileName) {
        super(packName, fileName);
        DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.PACK, getPackName(), "Deprecated prop utilisation in " + fileName, "Props should now be declared in the corresponding block_" + getName() + ".dynx file", ErrorTrackingService.TrackedErrorLevel.LOW);
        owner = null;
        this.itemIcon = "Prop";
    }

    public PropObject(ISubInfoTypeOwner<BlockObject<?>> owner, String fileName) {
        super(owner.getPackName(), fileName);
        this.itemIcon = "Prop";
        BlockObject<?> block = (BlockObject<?>) owner;
        this.owner = block;
        this.setDefaultName(block.getDefaultName());
        this.setDescription(block.getDescription());
        this.model = block.getModel();
        this.itemScale = block.getItemScale();
        this.itemTranslate = block.getItemTranslate();
        this.itemRotate = block.getItemRotate();
        this.item3DRenderLocation = block.get3DItemRenderLocation();
        this.translation = block.getTranslation();
        this.scaleModifier = block.getScaleModifier();
        this.renderDistance = block.getRenderDistance();
        this.creativeTabName = block.getCreativeTabName();
        this.useHullShape = block.doesUseHullShape();
        this.texturesArray = block.texturesArray;
        getPartShapes().addAll(block.getPartShapes());
    }

    @Override
    public List<PackFilePropertyData<?>> getInitiallyConfiguredProperties() {
        //Don't require required properties of the block
        return SubInfoTypeAnnotationCache.getOrLoadData(BlockObject.class).values().stream().filter(PackFilePropertyData::isRequired).collect(Collectors.toList());
    }

    @Override
    public void appendTo(BlockObject<?> partInfo) {
        partInfo.propObject = this;
        DynamXObjectLoaders.PROPS.loadItems(this, ContentPackLoader.isHotReloading);
    }

    @Override
    public IInfoOwner<T> createOwner(ObjectLoader<T, ?, ?> loader) {
        return new ItemProps(this);
    }

    @Override
    public void generateShape() {
        if (owner == null) {
            super.generateShape();
        } else {
            compoundCollisionShape = owner.compoundCollisionShape;
            for (MutableBoundingBox blockBox : owner.getCollisionBoxes()) {
                MutableBoundingBox propBox = new MutableBoundingBox(blockBox);
                propBox.offset(-0.5, -0.5f, -0.5);
                getCollisionBoxes().add(propBox);
            }
        }
        compoundCollisionShape.setMargin(margin);
        debugBuffer = ShapeUtils.getDebugVectorList(compoundCollisionShape, ShapeUtils.getDebugBuffer(compoundCollisionShape));
    }

    public CompoundCollisionShape getCompoundCollisionShape() {
        return compoundCollisionShape;
    }

    public int getEmptyMass() {
        return emptyMass;
    }

    public Vector3f getSpawnOffset() {
        return spawnOffset;
    }

    public float getDespawnTime() {
        return despawnTime;
    }

    public float getMargin() {
        return margin;
    }

    public boolean isCCDEnabled() {
        return isCCDEnabled;
    }

    public float getFriction() {
        return friction;
    }

    @Override
    public Vector3f getCenterOfMass() {
        return centerOfMass;
    }

    public float getDampingFactor() {
        return dampingFactor;
    }

    public float getRestitutionFactor() {
        return restitutionFactor;
    }

    @Override
    public Collection<? extends IShapeInfo> getShapes() {
        return getCollisionBoxes();
    }

    @Override
    public List<Vector3f> getCollisionShapeDebugBuffer() {
        return debugBuffer;
    }

    @Override
    public <U extends InteractivePart<?, ModularVehicleInfoBuilder>> List<U> getInteractiveParts() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public void addSubProperty(ISubInfoType<BlockObject<?>> property) {
    }

    @Override
    public List<ISubInfoType<BlockObject<?>>> getSubProperties() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public boolean shouldRegisterModel() {
        return owner == null || !model.equals(owner.getModel()); //Don't register the model twice if there is a block owning this prop
    }

    @Override
    public String toString() {
        return "PropObject named " + getFullName();
    }
}
