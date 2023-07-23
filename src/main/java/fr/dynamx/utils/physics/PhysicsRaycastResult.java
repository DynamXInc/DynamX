package fr.dynamx.utils.physics;

import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;

/**
 * This class stores different information about a raycast
 */

public class PhysicsRaycastResult {

    /**
     * The vector where the raycast started
     **/
    public final Vector3f fromVec;
    /**
     * The direction of the starting ray
     **/
    public final Vector3f direction;
    /**
     * The hit position in world coordinates
     **/
    public final Vector3f hitPos;
    /**
     * The distance between the first position of the raycast and the hit position
     **/
    public final float distance;
    /**
     * The hit normal in world coordinates
     */
    public final Vector3f hitNormal;
    /**
     * The body hit by the raycast
     **/
    public final PhysicsCollisionObject hitBody;

    public PhysicsRaycastResult(Vector3f fromVec, Vector3f direction, Vector3f hitPos, float distance, Vector3f hitNormal, PhysicsCollisionObject hitBody) {
        this.fromVec = fromVec;
        this.direction = direction;
        this.hitPos = hitPos;
        this.distance = distance;
        this.hitNormal = hitNormal;
        this.hitBody = hitBody;
    }

    @Override
    public String toString() {
        return "PhysicsRaycastResult{" +
                "fromVec=" + fromVec +
                ", direction=" + direction +
                ", hitPos=" + hitPos +
                ", distance=" + distance +
                ", hitNormal=" + hitNormal +
                ", hitBody=" + hitBody +
                '}';
    }
}
