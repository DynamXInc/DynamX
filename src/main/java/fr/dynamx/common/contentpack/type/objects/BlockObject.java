package fr.dynamx.common.contentpack.type.objects;

import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IInfoOwner;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.events.CreatePackItemEvent;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.contentpack.loader.ObjectLoader;
import fr.dynamx.common.contentpack.parts.ILightOwner;
import fr.dynamx.common.contentpack.parts.PartLightSource;
import fr.dynamx.common.contentpack.type.MaterialVariantsInfo;
import fr.dynamx.common.contentpack.type.ParticleEmitterInfo;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.material.Material;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockObject<T extends BlockObject<?>> extends AbstractProp<T> implements ParticleEmitterInfo.IParticleEmitterContainer, ILightOwner<T> {
    /**
     * List of owned {@link ISubInfoType}s
     */
    protected final List<ISubInfoType<T>> subProperties = new ArrayList<>();
    protected PropObject<?> propObject;

    @PackFileProperty(configNames = "Rotate", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false, defaultValue = "0 0 0")
    @Getter
    @Setter
    protected Vector3f rotation = new Vector3f(0, 0, 0); //Not supported by props

    @PackFileProperty(configNames = "LightLevel", defaultValue = "0", required = false)
    @Getter
    protected float lightLevel;

    @PackFileProperty(configNames = "Material", required = false, defaultValue = "ROCK")
    @Getter
    protected Material material;

    protected boolean shouldBeBatched;

    /**
     * The light sources of this block
     */
    @Getter
    protected final Map<String, PartLightSource> lightSources = new HashMap<>();

    private final List<ParticleEmitterInfo<?>> particleEmitters = new ArrayList<>();

    public BlockObject(String packName, String fileName) {
        super(packName, fileName);
        this.itemIcon = "Block";
    }

    @Override
    public MaterialVariantsInfo<?> getVariants() {
        return getSubPropertyByType(MaterialVariantsInfo.class);
    }

    @Override
    public boolean postLoad(boolean hot) {
        if (texturesArray != null)
            new MaterialVariantsInfo(this, texturesArray).appendTo(this);
        //Map lights
        lightSources.values().forEach(PartLightSource::postLoad);
        return super.postLoad(hot);
    }

    @Override
    @SuppressWarnings("unchecked")
    public IInfoOwner<T> createOwner(ObjectLoader<T, ?> loader) {
        CreatePackItemEvent.SimpleBlock<T, ?> event = new CreatePackItemEvent.SimpleBlock(loader, this);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isOverridden()) {
            return (IInfoOwner<T>) event.getObjectItem();
        } else {
            return new DynamXBlock<>((T) this, material != null ? material : Material.ROCK);
        }
    }

    @Override
    public String getTranslationKey(IInfoOwner<T> item, int itemMeta) {
        return super.getTranslationKey(item, itemMeta).replace("item", "tile");
    }

    @Override
    public void addSubProperty(ISubInfoType<T> property) {
        subProperties.add(property);
    }

    @Override
    public List<ISubInfoType<T>> getSubProperties() {
        return subProperties;
    }

    public CompoundCollisionShape getCompoundCollisionShape() {
        return compoundCollisionShape;
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
        return "BlockObject named " + getFullName();
    }

    public boolean isObj() {
        return getModel().getPath().endsWith(".obj");
    }

    @Override
    public void addPart(BasePart<T> tBasePart) {

    }

    @Override
    public void addLightSource(PartLightSource source) {
        lightSources.put(source.getPartName(), source);
    }

    @Override
    public PartLightSource getLightSource(String partName) {
        return lightSources.get(partName);
    }

    @Override
    public boolean shouldBeBatched() {
        return shouldBeBatched;
    }
}
