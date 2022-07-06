package fr.dynamx.common.entities;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.utils.optimization.MutableBoundingBox;

import java.util.List;

/**
 * DynamX objects that have complex collisions with players, for example {@link PhysicsEntity} and {@link fr.dynamx.common.blocks.TEDynamXBlock} <br>
 * <br>
 * The collision is handled in the {@link fr.dynamx.api.physics.IRotatedCollisionHandler}
 */
public interface ICollidableObject {
    /**
     * @return The collision boxes composing this entity, with no rotation applied, but at the objet position <br>
     *     Used for collisions with players and other entities <br>
     *     The list is not modified by callers of the function
     */
    List<MutableBoundingBox> getCollisionBoxes();

    /**
     * @return The rotation of the collidable object
     */
    Quaternion getCollidableRotation();

    /**
     * @return The offset of the collision boxes
     */
    Vector3f getCollisionOffset();
}
