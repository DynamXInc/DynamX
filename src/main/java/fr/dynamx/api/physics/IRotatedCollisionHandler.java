package fr.dynamx.api.physics;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Provides helper methods for rotated collisions and handles collisions with entities <br>
 *      Interface used for protection system
 */
public interface IRotatedCollisionHandler
{
    /**
     * Rotates the provided pos
     *
     * @param pos a pos array {x, y, z} that will be modified
     * @return the rotated double array (the modified pos array)
     */
    Vector3f rotate(Vector3f pos, Quaternion rotation);

    /**
     * Rotates a bounding box, creating the biggest axis aligned box containing the rotated input box
     *
     * @param offset The offset of the box, from the origin of the rotation (0,0,0)
     * @param pos The position of the box ( to the box is translated to offset for rotations), not modified
     * @param from The box to rotate
     * @return A new AxisAlignedBB, an axis aligned box containing the rotated input box and at the same position
     */
    AxisAlignedBB rotateBB(Vector3f offset, Vector3f pos, AxisAlignedBB from, Quaternion rotation);

    /**
     * Rotates a mutable bounding box, creating the biggest axis aligned box containing the rotated input box
     *
     * @param pos The position of the box (the box is translated to 0,0,0 for rotations), not modified
     * @param from The box to rotate, modified by the function
     * @return A new MutableBoundingBox, an axis aligned box containing the rotated input box and at the same position
     */
    MutableBoundingBox rotateBB(Vector3f pos, MutableBoundingBox from, Quaternion rotation);

    /**
     * @return the updated motion of entity after colliding it with physics entities
     */
    double[] handleCollisionWithBulletEntities(Entity entity, double mx, double my, double mz);

    /**
     * @return True if the last call to handleCollisionWithBulletEntities has changed the entity motion
     */
    boolean motionHasChanged();
}
