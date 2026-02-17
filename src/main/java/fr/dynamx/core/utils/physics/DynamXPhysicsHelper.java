package fr.dynamx.core.utils.physics;

import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.core.common.entities.PhysicsEntity;
import fr.dynamx.core.utils.maths.DynamXGeometry;
import fr.dynamx.core.utils.maths.DynamXMath;
import fr.dynamx.core.utils.optimization.QuaternionPool;
import fr.dynamx.core.utils.optimization.JmeVector3fPool;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

/**
 * General physics methods
 *
 * @see DynamXGeometry
 */
public class DynamXPhysicsHelper {
    public static float GRAVITY = 9.81f;
    public static final float WATER_DENSITY = 997; //kg/m^3

    public static Vector3f getVelocityAtPoint(Vector3f linearVelocity, Vector3f angularVelocity, Vector3f forcePoint) {
        Vector3f velocityAtPoint = new Vector3f();
        angularVelocity.cross(forcePoint, velocityAtPoint);
        velocityAtPoint.addLocal(linearVelocity);
        return velocityAtPoint;
    }

    /**
     * Method to fast create rigid body <br>
     * Does not adds the body to the physics world
     *
     * @param mass      Mass in kg
     * @param colShape  Shape of the rigid body
     * @param transform Initial transform
     * @param type      The {@link BulletShapeType} of this rigid body
     */
    public static PhysicsRigidBody createRigidBody(float mass, Transform transform, CollisionShape colShape, BulletShapeType<?> type) {
        PhysicsRigidBody rigidBody = new PhysicsRigidBody(colShape, mass);
        rigidBody.setPhysicsTransform(transform);
        rigidBody.setUserObject(type);
        return rigidBody;
    }

    /**
     * Method to fast create rigid body and translate it <br>
     * Does not adds the body to the physics world
     *
     * @param physicsEntity  The entity owning this rigid body
     * @param mass           Mass in kg
     * @param collisionShape Shape of the rigid body
     * @param position       Initial position
     * @param spawnRotation  Initial yaw rotation in degrees
     */
    public static PhysicsRigidBody fastCreateRigidBody(PhysicsEntity<?> physicsEntity, float mass, CollisionShape collisionShape, Vector3f position, float spawnRotation) {
        Quaternion bodyQuaternion = new Quaternion().fromAngleNormalAxis((float) Math.toRadians(-spawnRotation), new Vector3f(0, 1, 0));
        Transform bodyTransform = new Transform(position, bodyQuaternion);

        return createRigidBody(mass, bodyTransform, collisionShape, new BulletShapeType<>(EnumBulletShapeType.BULLET_ENTITY, physicsEntity));
    }

    public static PhysicsRaycastResult castRay(IPhysicsWorld iPhysicsWorld, org.joml.Vector3f fromV, org.joml.Vector3f dirV, Predicate<EnumBulletShapeType> ignoredBody) {
        if(iPhysicsWorld == null) {
            return null;
        }
        JmeVector3fPool.openPool();
        Vector3f from = JmeVector3fPool.get(fromV);
        Vector3f dir = JmeVector3fPool.get(dirV);

        List<PhysicsRayTestResult> results = new LinkedList<>();
        iPhysicsWorld.getDynamicsWorld().rayTest(from, dir, results);

        for (PhysicsRayTestResult result : results) {
            if (!(result.getCollisionObject() instanceof PhysicsRigidBody))
                continue;
            if (!ignoredBody.test(((BulletShapeType<?>) result.getCollisionObject().getUserObject()).getType()))
                continue;

            Vector3f hitPosition = JmeVector3fPool.get();
            DynamXMath.interpolateLinear(result.getHitFraction(), from, dir, hitPosition);

            float distance = result.getHitFraction() * dir.length();

            Vector3f hitNormalInWorld = JmeVector3fPool.get();
            result.getHitNormalLocal(hitNormalInWorld);

            PhysicsRigidBody hitBody = (PhysicsRigidBody) result.getCollisionObject();

            JmeVector3fPool.closePool();
            return new PhysicsRaycastResult(from, dir, hitPosition, distance, hitNormalInWorld, hitBody);
        }
        JmeVector3fPool.closePool();
        return null;
    }

    public static Vector3f getBodyLocalPoint(PhysicsCollisionObject rigidBody, Vector3f pointInWorld) {
        Vector3f bodyLocation = JmeVector3fPool.get();
        rigidBody.getPhysicsLocation(bodyLocation);

        Vector3f pickPosition = pointInWorld.subtract(bodyLocation);

        Quaternion bodyRotation = QuaternionPool.get();
        rigidBody.getPhysicsRotation(bodyRotation);

        return bodyRotation.inverse().multLocal(pickPosition);
    }

    public static void createExplosion(PhysicsEntity<?> physicsEntity, org.joml.Vector3f explosionPosition, double explosionStrength) {
        if (physicsEntity.getPhysicsHandler() != null) {
            PhysicsRigidBody body = (PhysicsRigidBody) physicsEntity.getPhysicsHandler().getCollisionObject();
            Vector3f centerOfMass = physicsEntity.physicsPosition;
            double distance = DynamXGeometry.distanceBetween(centerOfMass, explosionPosition);
            Vector3f direction = centerOfMass.subtract(explosionPosition.x, explosionPosition.y, explosionPosition.z).normalize().add(new Vector3f(0.0f, 2.0f, 0.0f)).normalize();
            double forgeStrength = (1.0D - DynamXMath.clamp((float) (distance / explosionStrength * 2.0f), 0f, 1f)) * 15.0D;
            body.activate();
            body.setLinearVelocity(body.getLinearVelocity(null).add(new Vector3f(
                    (float) (direction.x * forgeStrength),
                    (float) (direction.y * forgeStrength),
                    (float) (direction.z * forgeStrength))));
        }
    }


    public enum EnumPhysicsAxis {
        X, Y, Z, X_ROT, Y_ROT, Z_ROT;

        public static EnumPhysicsAxis fromInteger(int target) {
            if (target >= 0 && target < values().length)
                return values()[target];
            throw new IllegalArgumentException("Invalid axis '" + target + "'");
        }

        public static EnumPhysicsAxis fromString(String targetName) {
            if (NumberUtils.isCreatable(targetName))
                return fromInteger(Integer.parseInt(targetName));
            for (EnumPhysicsAxis axis : values()) {
                if (axis.name().equalsIgnoreCase(targetName)) {
                    return axis;
                }
            }
            throw new IllegalArgumentException("Invalid axis '" + targetName + "'");
        }
    }

}
