package fr.dynamx.common.contentpack.type.objects;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.CylinderCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IShapedObject;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.obj.IObjObject;
import fr.dynamx.common.contentpack.parts.PartShape;
import fr.dynamx.common.obj.ObjModelServer;
import fr.dynamx.common.obj.texture.TextureData;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.physics.ShapeUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractProp<T extends AbstractProp<?>> extends AbstractItemObject<T> implements IShapedObject {
    @PackFileProperty(configNames = "Translate", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false, defaultValue = "0 0 0")
    protected Vector3f translation = new Vector3f(0, 0, 0);
    @PackFileProperty(configNames = "Scale", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false, defaultValue = "1 1 1")
    protected Vector3f scaleModifier = new Vector3f(1, 1, 1);
    @PackFileProperty(configNames = "RenderDistanceSquared", required = false, defaultValue = "4096")
    protected float renderDistance = 4096;
    @PackFileProperty(configNames = "UseComplexCollisions", oldNames = {"UseHullShape"}, required = false, defaultValue = "false", description = "common.UseComplexCollisions")
    protected boolean useHullShape = false;
    /*@PackFileProperty(configNames = "CollisionType", required = false, defaultValue = "Simple", type = DefinitionType.DynamXDefinitionTypes.COLLISION_TYPE)
    protected EnumCollisionType collisionType = EnumCollisionType.SIMPLE;*/
    @PackFileProperty(configNames = "Textures", required = false, type = DefinitionType.DynamXDefinitionTypes.STRING_ARRAY_2D)
    protected String[][] texturesArray;

    private final List<BasePart<?>> parts = new ArrayList<>();
    private final List<MutableBoundingBox> collisionBoxes = new ArrayList<>();
    private final List<PartShape<?>> partShapes = new ArrayList<>();
    private final Map<Byte, TextureData> textures = new HashMap<>();

    protected CompoundCollisionShape compoundCollisionShape;

    private int maxTextureMetadata;

    public AbstractProp(String packName, String fileName) {
        super(packName, fileName);
        itemScale = 0.3f;
    }

    @Override
    public void setShapeErrored() {
        //DO STH ?
    }

    @Override
    public void generateShape() {
        compoundCollisionShape = new CompoundCollisionShape();
        if (getPartShapes().isEmpty()) {
            if (useHullShape) {
                compoundCollisionShape = ShapeUtils.generateComplexModelCollisions(DynamXUtils.getModelPath(getPackName(), model), "", scaleModifier, new Vector3f(), 0);
            } else {
                ShapeUtils.generateModelCollisions(this, ObjModelServer.createServerObjModel(DynamXUtils.getModelPath(getPackName(), getModel())), compoundCollisionShape);
            }
        } else {
            getPartShapes().forEach(shape -> {
                getCollisionBoxes().add(shape.getBoundingBox().offset(0.5,0.5,0.5));
                switch (shape.getShapeType()){
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
    }

    @Override
    public void onComplete(boolean hotReload) {
        textures.clear();
        textures.put((byte) 0, new TextureData("Default", (byte) 0, getName()));
        if (texturesArray != null) {
            byte id = 1;
            for (String[] info : texturesArray) {
                textures.put(id, new TextureData(info[0], id, info[1] == null ? "dummy" : info[1]));
                id++;
            }
        }
        int texCount = 0;
        for (TextureData data : textures.values()) {
            if (data.isItem())
                texCount++;
        }
        this.maxTextureMetadata = texCount;
    }

    @Nullable
    @Override
    public Map<Byte, TextureData> getTexturesFor(IObjObject object) {
        return textures;
    }

    public int getMaxTextureMetadata() {
        return maxTextureMetadata;
    }

    @Override
    public boolean hasCustomTextures() {
        return textures.size() > 1;
    }

    @Override
    public Vector3f getScaleModifier() {
        return scaleModifier;
    }

    public void setScaleModifier(Vector3f scale) {
        this.scaleModifier = scale;
    }

    @Override
    public void addPart(BasePart<?> tBasePart) {
        parts.add(tBasePart);
    }

    @Override
    public void addCollisionShape(PartShape<?> partShape) {
        partShapes.add(partShape);
    }

    public <A extends BasePart<?>> List<A> getPartsByType(Class<A> clazz) {
        return (List<A>) this.parts.stream().filter(p -> clazz.equals(p.getClass())).collect(Collectors.toList());
    }

    public List<BasePart<?>> getParts() {
        return parts;
    }

    public Vector3f getTranslation() {
        return translation;
    }

    public void setTranslation(Vector3f translation) {
        this.translation = translation;
    }

    public float getRenderDistance() {
        return renderDistance;
    }

    public void setRenderDistance(float renderDistance) {
        this.renderDistance = renderDistance;
    }

    public boolean doesUseHullShape() {
        return useHullShape;
    }

    public CompoundCollisionShape getCompoundCollisionShape() {
        return compoundCollisionShape;
    }

    public List<MutableBoundingBox> getCollisionBoxes() {
        return collisionBoxes;
    }

    public List<PartShape<?>> getPartShapes() {
        return partShapes;
    }
}