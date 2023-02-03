package fr.dynamx.common.entities;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.common.handlers.CollisionInfo;
import fr.dynamx.utils.optimization.MutableBoundingBox;

import java.util.List;

/**
 * DynamX objects that have complex collisions with players, for example {@link PhysicsEntity} and {@link fr.dynamx.common.blocks.TEDynamXBlock} <br>
 * <br>
 * The collision is handled in the {@link fr.dynamx.api.physics.IRotatedCollisionHandler}
 */
public interface ICollidableObject {
    CollisionInfo getCollisionInfo();
}
