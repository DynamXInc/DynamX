package fr.dynamx.common.contentpack.type;

import com.jme3.bullet.collision.shapes.*;
import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.obj.ObjModelPath;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.objloader.data.ObjModelData;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.physics.ShapeUtils;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class ObjectCollisionsHelper
{
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

    public void loadCollisions(INamedObject object, ObjModelPath modelPath, String partName, Vector3f centerOfMass, float shapeYOffset, boolean useComplexCollisions, Vector3f scaleModifier, CollisionType type) {
        try {
            if (useComplexCollisions) {
                // Case 1: complex collisions
                physicsCollisionShape = ShapeUtils.generateComplexModelCollisions(modelPath, partName, scaleModifier, centerOfMass, shapeYOffset);
            }
            if(getShapes().isEmpty()) {
                // Case 2: No shapes (doesn't depends on complex collisions)
                ObjModelData objModelData = DynamXContext.getObjModelDataFromCache(modelPath);
                if(!useComplexCollisions) {
                    // Case 2.1: No shapes and no complex collisions: generate physics collisions automatically from the obj model (with part shapes)
                    if (type == CollisionType.VEHICLE)
                        throw new UnsupportedOperationException("Automatic physics collisions (UseComplexCollisions = false when no PartShape is added) are not supported for vehicles");
                    physicsCollisionShape = new CompoundCollisionShape();
                }
                // else Case 2.2: No shapes and complex collisions: generate part shapes from the obj model
                objModelData.getObjObjects().forEach(objObject -> {
                    if(!partName.isEmpty() && !objObject.getName().toLowerCase().contains(partName.toLowerCase()))
                        return;
                    if(!useComplexCollisions) {
                        Vector3f half = objObject.getMesh().getDimension().multLocal(scaleModifier);
                        if (half.x != 0 || half.y != 0 || half.z != 0) {
                            physicsCollisionShape.addChildShape(new BoxCollisionShape(half), objObject.getMesh().getCenter().addLocal(centerOfMass));
                        }
                    }
                    MutableBoundingBox box = new MutableBoundingBox(objObject.getMesh().getDimension().mult(scaleModifier)).offset(objObject.getMesh().getCenter());
                    shapes.add(new IShapeInfo() {
                        @Override
                        public Vector3f getPosition() {
                            return objObject.getMesh().getCenter();
                        }

                        @Override
                        public Vector3f getSize() {
                            return objObject.getMesh().getDimension().multLocal(scaleModifier);
                        }

                        @Override
                        public MutableBoundingBox getBoundingBox() {
                            return box;
                        }
                    });
                });
            } else if(!useComplexCollisions) {
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
            if(type == CollisionType.BLOCK) {
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
        if(EMPTY_COLLISION_SHAPE == null)
            EMPTY_COLLISION_SHAPE = new CompoundCollisionShape();
        return EMPTY_COLLISION_SHAPE;
    }
}
