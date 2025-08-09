package fr.dynamx.core.common.contentpack.type.objects;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.ICollisionsContainer;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;
import fr.dynamx.core.client.renders.model.renderer.ObjObjectRenderer;
import fr.dynamx.core.common.contentpack.type.MaterialVariantsInfo;
import fr.dynamx.core.common.contentpack.type.ObjectCollisionsHelper;
import fr.dynamx.core.common.contentpack.type.ParticleEmitterInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractProp<T extends AbstractProp<T>> extends AbstractItemObject<T, T> implements ICollisionsContainer, ParticleEmitterInfo.IParticleEmitterContainer {
    @Getter
    @Setter
    @PackFileProperty(configNames = "Translate", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false, defaultValue = "0 0 0")
    protected Vector3f translation = new Vector3f(0, 0, 0);
    @Getter
    @Setter
    @PackFileProperty(configNames = "Scale", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false, defaultValue = "1 1 1")
    protected Vector3f scaleModifier = new Vector3f(1, 1, 1);
    @Getter
    @Setter
    @PackFileProperty(configNames = "RenderDistanceSquared", required = false, defaultValue = "4096")
    protected float renderDistanceSquared = 4096;

    @Getter
    @Accessors(fluent = true)
    @PackFileProperty(configNames = "UseComplexCollisions", required = false, defaultValue = "false", description = "common.UseComplexCollisions")
    protected boolean useComplexCollisions = false;
    @Getter
    protected ObjectCollisionsHelper collisionsHelper;

    @Deprecated
    @PackFileProperty(configNames = "Textures", required = false, type = DefinitionType.DynamXDefinitionTypes.STRING_ARRAY_2D)
    protected String[][] texturesArray;

    private final List<ParticleEmitterInfo<?>> particleEmitters = new ArrayList<>();

    public AbstractProp(String packName, String fileName) {
        super(packName, fileName);
    }

    abstract MaterialVariantsInfo<?> getVariants();

    @Override
    public IModelTextureVariantsSupplier.IModelTextureVariants getTextureVariantsFor(ObjObjectRenderer objObjectRenderer) {
        return getVariants();
    }

    @Override
    public boolean hasTextureVariants() {
        return getVariants() != null;
    }

    @Override
    public byte getMaxVariantId() {
        return (byte) (hasTextureVariants() ? getVariants().getVariantsMap().size() : 1);
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
    public <A extends InteractivePart<?, ?>> List<A> getInteractiveParts() {
        return (List<A>) getPartsByType(InteractivePart.class);
    }

    @Override
    public float getBaseItemScale() {
        return 0.3f;
    }
}