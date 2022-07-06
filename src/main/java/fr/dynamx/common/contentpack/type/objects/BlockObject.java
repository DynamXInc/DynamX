package fr.dynamx.common.contentpack.type.objects;

import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IInfoOwner;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.events.CreatePackItemEvent;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.contentpack.loader.ObjectLoader;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;

public class BlockObject<T extends BlockObject<?>> extends AbstractProp<T> implements ISubInfoTypeOwner<BlockObject<?>> {
    /**
     * List of owned {@link ISubInfoType}s
     */
    protected final List<ISubInfoType<BlockObject<?>>> subProperties = new ArrayList<>();
    protected PropObject<?> propObject;

    @PackFileProperty(configNames = "Rotate", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false, defaultValue = "0 0 0")
    protected Vector3f rotation = new Vector3f(0, 0, 0); //Not supported by props

    @PackFileProperty(configNames = "LightLevel", defaultValue = "0", required = false)
    protected float lightLevel;

    public BlockObject(String packName, String fileName) {
        super(packName, fileName);
        this.itemIcon = "Block";
    }

    @Override
    @SuppressWarnings("unchecked")
    public IInfoOwner<T> createOwner(ObjectLoader<T, ?, ?> loader) {
        CreatePackItemEvent.CreateSimpleBlockEvent event = new CreatePackItemEvent.CreateSimpleBlockEvent((ObjectLoader<BlockObject<?>, DynamXBlock<BlockObject<?>>, BlockObject<?>>) loader, this);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isOverridden()) {
            return (IInfoOwner<T>) event.getSpawnItem();
        } else {
            return new DynamXBlock(this);
        }
    }

    @Override
    public String getTranslationKey(IInfoOwner<T> item, int itemMeta) {
        return super.getTranslationKey(item, itemMeta).replace("item", "tile");
    }

    @Override
    public void addSubProperty(ISubInfoType<BlockObject<?>> property) {
        subProperties.add(property);
    }

    @Override
    public List<ISubInfoType<BlockObject<?>>> getSubProperties() {
        return subProperties;
    }

    public CompoundCollisionShape getCompoundCollisionShape() {
        return compoundCollisionShape;
    }

    public Vector3f getRotation() {
        return rotation;
    }

    public void setRotation(Vector3f rotation) {
        this.rotation = rotation;
    }

    @Override
    public String toString() {
        return "BlockObject named "+getFullName();
    }

    public boolean isObj() {
        return getModel().endsWith(".obj");
    }

    public float getLightLevel() {
        return lightLevel;
    }
}
