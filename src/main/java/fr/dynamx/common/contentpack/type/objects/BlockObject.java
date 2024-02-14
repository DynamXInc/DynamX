package fr.dynamx.common.contentpack.type.objects;

import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.dxmodel.EnumDxModelFormats;
import fr.dynamx.api.events.CreatePackItemEvent;
import fr.dynamx.api.events.DynamXBlockEvent;
import fr.dynamx.client.renders.scene.SceneBuilder;
import fr.dynamx.client.renders.scene.node.BlockNode;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.contentpack.loader.InfoList;
import fr.dynamx.common.contentpack.parts.ILightOwner;
import fr.dynamx.common.contentpack.parts.PartLightSource;
import fr.dynamx.common.contentpack.type.MaterialVariantsInfo;
import fr.dynamx.common.contentpack.type.ObjectCollisionsHelper;
import fr.dynamx.common.contentpack.type.ParticleEmitterInfo;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.material.Material;
import net.minecraftforge.common.MinecraftForge;

import java.util.*;

public class BlockObject<T extends BlockObject<?>> extends AbstractProp<T> implements ParticleEmitterInfo.IParticleEmitterContainer, ILightOwner<T> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = SubInfoTypeRegistries.BLOCKS)
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

    @Getter
    @Setter
    protected PropObject<?> propObject;

    @PackFileProperty(configNames = "Rotate", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false, defaultValue = "0 0 0")
    @Getter
    @Setter
    protected Vector3f rotation = new Vector3f(0, 0, 0); //Not supported by props

    @PackFileProperty(configNames = "LightLevel", defaultValue = "0", required = false)
    @Getter
    @Setter
    protected float lightLevel;

    @PackFileProperty(configNames = "Material", required = false, defaultValue = "ROCK")
    @Getter
    protected Material material = Material.ROCK;

    /**
     * The light sources of this block
     */
    @Getter
    protected final Map<String, PartLightSource> lightSources = new HashMap<>();

    protected final List<ParticleEmitterInfo<?>> particleEmitters = new ArrayList<>();

    protected SceneNode<?, ?> sceneNode;

    public BlockObject(String packName, String fileName) {
        super(packName, fileName);
        itemIcon = "Block";
        collisionsHelper = new ObjectCollisionsHelper();
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
        collisionsHelper.loadCollisions(this, DynamXUtils.getModelPath(getPackName(), model), "", translation, 0, useComplexCollisions, scaleModifier, ObjectCollisionsHelper.CollisionType.BLOCK);
        if (hasVaryingTextures() && getMaxTextureMetadata() > 16 && (getCreativeTabName() == null || !getCreativeTabName().equalsIgnoreCase("None"))) {
            DynamXErrorManager.addError(getPackName(), DynamXErrorManager.PACKS_ERRORS, "too_many_variants", ErrorLevel.HIGH, getName(), "You can't use more than 16 variants on blocks !");
        }
        return super.postLoad(hot);
    }

    @Override
    @SuppressWarnings("unchecked")
    public IDynamXItem<T> createItem(InfoList<T> loader) {
        CreatePackItemEvent.SimpleBlock<T, ?> event = new CreatePackItemEvent.SimpleBlock(loader, this);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isOverridden()) {
            return event.getObjectItem();
        } else {
            return new DynamXBlock<>((T) this);
        }
    }

    @Override
    public SceneNode<?, ?> getSceneGraph() {
        if (sceneNode == null) {
            if (isModelValid()) {
                DynamXBlockEvent.BuildSceneGraph event = new DynamXBlockEvent.BuildSceneGraph(new SceneBuilder<>(), this, (List) getDrawableParts(), getScaleModifier());
                MinecraftForge.EVENT_BUS.post(event);
                sceneNode = event.getSceneGraphResult();
            } else
                sceneNode = new BlockNode<>(Collections.EMPTY_LIST);
        }
        return sceneNode;
    }

    @Override
    public String getTranslationKey(IDynamXItem<T> item, int itemMeta) {
        return super.getTranslationKey(item, itemMeta).replace("item", "tile");
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

    public boolean isDxModel() {
        return EnumDxModelFormats.isValidFormat(getModel().getPath());
    }

    @Override
    public void addLightSource(PartLightSource source) {
        lightSources.put(source.getObjectName(), source);
        addDrawablePart(source);
    }

    @Override
    public PartLightSource getLightSource(String objectName) {
        return lightSources.get(objectName);
    }
}
