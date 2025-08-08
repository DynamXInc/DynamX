package fr.dynamx.common.entities;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.utils.optimization.MutableBoundingBox;

import javax.annotation.Nullable;
import java.util.List;

/**
 * DynamX objects that have complex collisions with players, for example {@link PhysicsEntity} and {@link TEDynamXBlock} <br>
 * <br>
 * The collision is handled in the {@link fr.dynamx.api.physics.IRotatedCollisionHandler}
 */
public interface IDynamXObject {
    /**
     * @return The collision boxes composing this entity, with no rotation applied, but at the objet position <br>
     * Used for collisions with players and other entities <br>
     * The list is not modified by callers of the function
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

    /**
     * @return The pack info of this object. Can be null for non-pack objects (ragdolls for example)
     * @param <A> The type of the pack info
     */
    @Nullable
    <A extends IPartContainer<?>> A getPackInfo();
}
