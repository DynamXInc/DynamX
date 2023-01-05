package fr.dynamx.common.contentpack.type.objects;

import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
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
import fr.dynamx.api.events.CreatePackItemEvent;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.MaterialVariantsInfo;
import fr.dynamx.common.contentpack.type.ParticleEmitterInfo;
import fr.dynamx.common.contentpack.loader.ObjectLoader;
import fr.dynamx.common.contentpack.loader.PackFilePropertyData;
import fr.dynamx.common.contentpack.loader.SubInfoTypeAnnotationCache;
import fr.dynamx.common.items.ItemProps;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.physics.ShapeUtils;
import lombok.Getter;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RegisteredSubInfoType(name = "prop", registries = SubInfoTypeRegistries.BLOCKS_AND_PROPS, strictName = false)
public class PropObject<T extends PropObject<?>> extends AbstractProp<T> implements IPhysicsPackInfo,
        ISubInfoType<BlockObject<?>>, ParticleEmitterInfo.IParticleEmitterContainer {
    private final BlockObject<?> owner;
    @PackFileProperty(configNames = "EmptyMass")
    @Getter
    protected int emptyMass;
    @PackFileProperty(configNames = "CenterOfGravityOffset", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F)
    @Getter
    protected Vector3f centerOfMass;
    @PackFileProperty(configNames = "SpawnOffset", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false, defaultValue = "0 0.65 0")
    @Getter
    protected Vector3f spawnOffset = new Vector3f(0, 0.65f, 0);
    @PackFileProperty(configNames = "ContinuousCollisionDetection", required = false, defaultValue = "false")
    @Getter
    protected boolean isCCDEnabled;
    @PackFileProperty(configNames = "Friction", required = false, defaultValue = "0.5")
    @Getter
    protected float friction = 0.5f;
    @PackFileProperty(configNames = "Margin", required = false, defaultValue = "0.04")
    @Getter
    protected float margin = 0.04f;
    @PackFileProperty(configNames = "DespawnTime", required = false, defaultValue = "\"-1\" (disabled)")
    @Getter
    protected float despawnTime = -1;
    @PackFileProperty(configNames = "Damping", required = false, defaultValue = "0")
    @Getter
    protected float dampingFactor;
    @PackFileProperty(configNames = "Bounciness", required = false, defaultValue = "0")
    @Getter
    protected float restitutionFactor;
    private List<Vector3f> debugBuffer;

    private List<ParticleEmitterInfo<?>> particleEmitters = new ArrayList<>();

    public PropObject(String packName, String fileName) {
        super(packName, fileName);
        DynamXErrorManager.addPackError(getPackName(), "deprecated_prop", ErrorLevel.LOW, fileName, "Props should now be declared in the corresponding block_" + getName() + ".dynx file");
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
        this.useHullShape = block.useHullShape();
        this.particleEmitters = block.getParticleEmitters();
        getPartShapes().addAll(block.getPartShapes());
    }

    @Override
    public List<PackFilePropertyData<?>> getInitiallyConfiguredProperties() {
        //Don't require properties of the block
        return SubInfoTypeAnnotationCache.getOrLoadData(BlockObject.class).values().stream().filter(PackFilePropertyData::isRequired).collect(Collectors.toList());
    }

    @Override
    public void appendTo(BlockObject<?> owner) {
        owner.propObject = this;
        DynamXObjectLoaders.PROPS.loadItems(this, ContentPackLoader.isHotReloading);
    }

    @Nullable
    @Override
    public BlockObject<?> getOwner() {
        return owner;
    }

    @Override
    public IInfoOwner<T> createOwner(ObjectLoader<T, ?> loader) {
        CreatePackItemEvent.PropsItem<T, ?> event = new CreatePackItemEvent.PropsItem(loader, this);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isOverridden()) {
            return (IInfoOwner<T>) event.getObjectItem();
        } else {
            return new ItemProps(this);
        }
    }

    @Override
    public boolean postLoad(boolean hot) {
        if (owner == null) {
            super.postLoad(hot);
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
        return true;
    }

    @Override
    public MaterialVariantsInfo<?> getVariants() {
        return owner != null ? owner.getVariants() : null;
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
    public <U extends InteractivePart<?, ?>> List<U> getInteractiveParts() {
        return Collections.emptyList();
    }

    @Override
    public CompoundCollisionShape getPhysicsCollisionShape() {
        return compoundCollisionShape;
    }

    @Override
    public void addSubProperty(ISubInfoType<T> property) {
    }

    @Override
    public List<ISubInfoType<T>> getSubProperties() {
        return Collections.emptyList();
    }

    @Override
    public boolean shouldRegisterModel() {
        return owner == null || !model.equals(owner.getModel()); //Don't register the model twice if there is a block owning this prop
    }

    @Override
    public void addParticleEmitter(ParticleEmitterInfo<?> emitterInfo) {
        particleEmitters.add(emitterInfo);
    }

    @Override
    public List<ParticleEmitterInfo<?>> getParticleEmitters() {
        return particleEmitters;
    }

    @Override
    public String toString() {
        return "PropObject named " + getFullName();
    }
}
