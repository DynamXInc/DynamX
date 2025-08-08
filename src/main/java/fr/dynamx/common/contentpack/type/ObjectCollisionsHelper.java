package fr.dynamx.common.contentpack.type;

import com.jme3.bullet.collision.shapes.*;
import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.api.dxmodel.EnumDxModelFormats;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.objloader.data.DxModelData;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.physics.ShapeUtils;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class ObjectCollisionsHelper {
    private static CompoundCollisionShape EMPTY_COLLISION_SHAPE;

    /**
     * The collision shape of this object, generated either form the partShapes list, or the obj model of the object (hull shape/complex collisions)
     */
    @Getter
    private CompoundCollisionShape physicsCollisionShape;

    /**
     * The shapes of this object, can be used for collisions
     */
    @Getter
    protected final List<IShapeInfo> shapes = new ArrayList<>();

    public void addCollisionShape(IShapeInfo partShape) {
        shapes.add(partShape);
    }

    public void loadCollisions(INamedObject object, DxModelPath modelPath, String partName, Vector3f centerOfMass, float shapeYOffset, boolean useComplexCollisions, Vector3f scaleModifier, CollisionType type) {
        useComplexCollisions = useComplexCollisions && modelPath.getFormat() != EnumDxModelFormats.JSON;
        try {
            if (useComplexCollisions) {
                // Case 1: complex collisions
                physicsCollisionShape = ShapeUtils.generateComplexModelCollisions(modelPath, partName, scaleModifier, centerOfMass, shapeYOffset);
            }
            if (getShapes().isEmpty()) {
                if (modelPath.getFormat() == EnumDxModelFormats.JSON) {
                    // We can't generate collisions from a json model
                    return;
                }
                // Case 2: No shapes (doesn't depends on complex collisions)
                DxModelData dxModelData = DynamXContext.getDxModelDataFromCache(modelPath);
                if (!useComplexCollisions) {
                    // Case 2.1: No shapes and no complex collisions: generate physics collisions automatically from the obj model (with part shapes)
                    if (type == CollisionType.VEHICLE)
                        throw new UnsupportedOperationException("Automatic physics collisions (UseComplexCollisions = false when no PartShape is added) are not supported for vehicles");
                    physicsCollisionShape = new CompoundCollisionShape();
                }

                // Case 2.2: No shapes and complex collisions: generate part shapes from the obj model
                boolean finalUseComplexCollisions = useComplexCollisions;
                dxModelData.getMeshNames().forEach(meshName -> {
                    if (!partName.isEmpty() && !meshName.contains(partName.toLowerCase()))
                        return;
                    Vector3f dimension = dxModelData.getMeshDimension(meshName, new Vector3f()).multLocal(scaleModifier);
                    if (dimension.x == 0 && dimension.y == 0 && dimension.z == 0)
                        return;
                    Vector3f center = dxModelData.getMeshCenter(meshName, new Vector3f()).multLocal(scaleModifier);
                    if (!finalUseComplexCollisions) {
                        physicsCollisionShape.addChildShape(new BoxCollisionShape(dimension), center.add(centerOfMass));
                    }
                    MutableBoundingBox box = new MutableBoundingBox(dimension).offset(center);
                    shapes.add(new IShapeInfo() {
                        @Override
                        public Vector3f getPosition() {
                            return center;
                        }

                        @Override
                        public Vector3f getSize() {
                            return dimension;
                        }

                        @Override
                        public MutableBoundingBox getBoundingBox() {
                            return box;
                        }
                    });
                });
            } else if (!useComplexCollisions) {
                // Case 3: no complex collisions and shapes
                // nb: the scale modifier is already applied to the part shapes
                physicsCollisionShape = new CompoundCollisionShape();
                getShapes().forEach(shape -> {
                    CollisionShape collisionShape;
                    switch (shape.getShapeType()) {
                        case BOX:
                            collisionShape = new BoxCollisionShape(shape.getSize());
                            break;
                        case CYLINDER:
                            collisionShape = new CylinderCollisionShape(shape.getSize(), 0);
                            break;
                        case SPHERE:
                            collisionShape = new SphereCollisionShape(shape.getSize().x);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + shape.getShapeType());
                    }
                    physicsCollisionShape.addChildShape(collisionShape, new Vector3f(centerOfMass.x, centerOfMass.y, centerOfMass.z).add(shape.getPosition()));
                });
            }
            if (type == CollisionType.BLOCK) {
                getShapes().replaceAll(sh -> new IShapeInfo() {
                    @Override
                    public Vector3f getPosition() {
                        return sh.getPosition().add(0.5f, 1.5f, 0.5f).addLocal(centerOfMass);
                    }

                    @Override
                    public Vector3f getSize() {
                        return sh.getSize();
                    }

                    @Override
                    public MutableBoundingBox getBoundingBox() {
                        return new MutableBoundingBox(sh.getBoundingBox()).offset(0.5, 1.5, 0.5).offset(centerOfMass);
                    }
                });
            }
        } catch (Exception e) {
            DynamXErrorManager.addError(object.getPackName(), DynamXErrorManager.PACKS_ERRORS, "collision_shape_error", ErrorLevel.FATAL, object.getName(), null, e);
            physicsCollisionShape = null;
        }
    }

    public boolean hasPhysicsCollisions() {
        return physicsCollisionShape != null;
    }

    public ObjectCollisionsHelper copy() {
        ObjectCollisionsHelper copy = new ObjectCollisionsHelper();
        copy.shapes.addAll(shapes);
        return copy;
    }

    public enum CollisionType {
        VEHICLE,
        BLOCK,
        PROP
    }

    public static CompoundCollisionShape getEmptyCollisionShape() {
        if (EMPTY_COLLISION_SHAPE == null)
            EMPTY_COLLISION_SHAPE = new CompoundCollisionShape();
        return EMPTY_COLLISION_SHAPE;
    }
}
