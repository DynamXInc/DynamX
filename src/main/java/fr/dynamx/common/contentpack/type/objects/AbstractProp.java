package fr.dynamx.common.contentpack.type.objects;

import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.ICollisionsContainer;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.obj.IModelTextureVariantsSupplier;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;
import fr.dynamx.common.contentpack.type.MaterialVariantsInfo;
import fr.dynamx.common.contentpack.type.ObjectCollisionsHelper;
import fr.dynamx.common.contentpack.type.ParticleEmitterInfo;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractProp<T extends AbstractProp<?>> extends AbstractItemObject<T, T> implements ICollisionsContainer, ParticleEmitterInfo.IParticleEmitterContainer {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.PROPS})
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("UseHullShape".equals(key))
            return new IPackFilePropertyFixer.FixResult("UseComplexCollisions", true);
        if ("Textures".equals(key))
            return new IPackFilePropertyFixer.FixResult("MaterialVariants", true, true);
        return null;
    };

    @PackFileProperty(configNames = "Translate", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false, defaultValue = "0 0 0")
    @Getter
    @Setter
    protected Vector3f translation = new Vector3f(0, 0, 0);
    @PackFileProperty(configNames = "Scale", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false, defaultValue = "1 1 1")
    @Getter
    @Setter
    protected Vector3f scaleModifier = new Vector3f(1, 1, 1);
    @PackFileProperty(configNames = "RenderDistanceSquared", required = false, defaultValue = "4096")
    @Getter
    @Setter
    protected float renderDistance = 4096;

    @PackFileProperty(configNames = "UseComplexCollisions", required = false, defaultValue = "false", description = "common.UseComplexCollisions")
    @Accessors(fluent = true)
    @Getter
    protected boolean useComplexCollisions = false;
    @Getter
    protected ObjectCollisionsHelper collisionsHelper;

    @Deprecated
    @PackFileProperty(configNames = "Textures", required = false, type = DefinitionType.DynamXDefinitionTypes.STRING_ARRAY_2D)
    protected String[][] texturesArray;

    private final List<ParticleEmitterInfo<?>> particleEmitters = new ArrayList<>();

    public AbstractProp(String packName, String fileName) {
        super(packName, fileName);
        setItemScale(0.3f);
    }

    @Override
    public boolean postLoad(boolean hot) {
        if(hasVaryingTextures() && getMaxTextureMetadata() > 16) {
            DynamXErrorManager.addError(getPackName(), DynamXErrorManager.PACKS_ERRORS, "too_many_variants", ErrorLevel.HIGH, getName(), "You can't use more than 16 variants on blocks !");
        }
        return super.postLoad(hot);
    }

    abstract MaterialVariantsInfo<?> getVariants();

    @Override
    public IModelTextureVariantsSupplier.IModelTextureVariants getTextureVariantsFor(ObjObjectRenderer objObjectRenderer) {
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
    public void addParticleEmitter(ParticleEmitterInfo<?> emitterInfo) {
        particleEmitters.add(emitterInfo);
    }

    @Override
    public List<ParticleEmitterInfo<?>> getParticleEmitters() {
        return particleEmitters;
    }

    public <A extends InteractivePart<?, ?>> List<A> getInteractiveParts() {
        return (List<A>) getPartsByType(InteractivePart.class);
    }
}