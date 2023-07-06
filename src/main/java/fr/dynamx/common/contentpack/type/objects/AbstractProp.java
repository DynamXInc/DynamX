package fr.dynamx.common.contentpack.type.objects;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.CylinderCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.IShapeContainer;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;
import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.parts.PartShape;
import fr.dynamx.common.contentpack.type.MaterialVariantsInfo;
import fr.dynamx.common.contentpack.type.ParticleEmitterInfo;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.physics.ShapeUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractProp<T extends AbstractProp<?>> extends AbstractItemObject<T, T> implements IShapeContainer, ParticleEmitterInfo.IParticleEmitterContainer {
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
    protected boolean useHullShape = false;
    @Deprecated
    @PackFileProperty(configNames = "Textures", required = false, type = DefinitionType.DynamXDefinitionTypes.STRING_ARRAY_2D)
    protected String[][] texturesArray;

    @Getter
    private final List<MutableBoundingBox> collisionBoxes = new ArrayList<>();
    @Getter
    private final List<PartShape<?>> partShapes = new ArrayList<>();

    private final List<ParticleEmitterInfo<?>> particleEmitters = new ArrayList<>();

    @Getter
    protected CompoundCollisionShape compoundCollisionShape;

    public AbstractProp(String packName, String fileName) {
        super(packName, fileName);
        setItemScale(0.3f);
    }

    @Override
    public boolean postLoad(boolean hot) {
        compoundCollisionShape = new CompoundCollisionShape();
        if (getPartShapes().isEmpty()) {
            DxModelPath modelPath = DynamXUtils.getModelPath(getPackName(), model);
            if (useHullShape) {
                compoundCollisionShape = ShapeUtils.generateComplexModelCollisions(modelPath, "", scaleModifier, new Vector3f(), 0);
            } else {
                ShapeUtils.generateModelCollisions(this, DynamXContext.getDxModelDataFromCache(modelPath), compoundCollisionShape);
            }
        } else {
            getPartShapes().forEach(shape -> {
                getCollisionBoxes().add(shape.getBoundingBox().offset(0.5, 0.5, 0.5));
                switch (shape.getShapeType()) {
                    case BOX:
                        compoundCollisionShape.addChildShape(new BoxCollisionShape(shape.getSize()), shape.getPosition());
                        break;
                    case CYLINDER:
                        compoundCollisionShape.addChildShape(new CylinderCollisionShape(shape.getSize(), 0), shape.getPosition());
                        break;
                    case SPHERE:
                        compoundCollisionShape.addChildShape(new SphereCollisionShape(shape.getSize().x), shape.getPosition());
                        break;
                }
            });
        }
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
    public void addCollisionShape(PartShape<?> partShape) {
        partShapes.add(partShape);
    }

    @Override
    public void addParticleEmitter(ParticleEmitterInfo<?> emitterInfo) {
        particleEmitters.add(emitterInfo);
    }

    @Override
    public List<ParticleEmitterInfo<?>> getParticleEmitters() {
        return particleEmitters;
    }

}