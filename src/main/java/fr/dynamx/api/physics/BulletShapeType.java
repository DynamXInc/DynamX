package fr.dynamx.api.physics;

import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.math.Vector3f;
import fr.dynamx.utils.physics.ShapeUtils;
import lombok.Getter;

import javax.annotation.Nullable;
import java.nio.FloatBuffer;
import java.util.List;

/**
 * All rigid bodies used with DynamX must have an user object that is a BulletShapeType <br>
 * A BulletShapeType gives information about a bullet collision object, used for example to handle collisions with players
 *
 * @param <T> The type of the object using the rigid body
 * @see EnumBulletShapeType
 */
public class BulletShapeType<T> {
    /**
     * The type of the collision object
     */
    @Getter
    private final EnumBulletShapeType type;

    /**
     * The object generating the rigid body
     */
    @Getter
    private final T objectIn;

    /**
     * The debug triangles of this shape. Null if not generated yet.
     */
    @Nullable
    private List<Vector3f> debugTriangles;

    /**
     * @param type     The type of the object, important
     * @param objectIn The object generating the rigid body
     */
    public BulletShapeType(EnumBulletShapeType type, T objectIn) {
        this.type = type;
        this.objectIn = objectIn;
    }

    private void generateDebugTriangles(CollisionShape shape) {
        FloatBuffer[] debugBuffer;
        CompoundCollisionShape compoundCollisionShape = null;
        if (shape instanceof CompoundCollisionShape) {
            compoundCollisionShape = (CompoundCollisionShape) shape;
            debugBuffer = ShapeUtils.getDebugBuffer(compoundCollisionShape);
        } else {
            debugBuffer = new FloatBuffer[]{ShapeUtils.getDebugBuffer(shape)};
        }
        debugTriangles = ShapeUtils.getDebugVectorList(compoundCollisionShape, debugBuffer);
    }

    /**
     * Returns debug triangles for the given shape <br>
     * The debug triangles are generated the first time this method is called, from the given shape, then kept in memory.
     *
     * @param shape The shape to get debug triangles from. <strong>Should be the same as the one used to generate this BulletShapeType.</strong>
     * @return The debug triangles of this shape
     */
    public List<Vector3f> getDebugTriangles(CollisionShape shape) {
        if (debugTriangles == null) { // Performance fix: load only when we need it (in debug mode)
            generateDebugTriangles(shape);
        }
        return debugTriangles;
    }

    @Override
    public String toString() {
        return "BulletShapeType{" +
                type +
                ", objectIn=" + objectIn +
                '}';
    }
}