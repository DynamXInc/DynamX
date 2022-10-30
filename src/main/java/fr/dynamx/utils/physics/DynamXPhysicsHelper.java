package fr.dynamx.utils.physics;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.TransformPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.util.math.MathHelper;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

/**
 * General physics methods
 *
 * @see DynamXGeometry
 */
public class DynamXPhysicsHelper {
    public static final Vector3f GRAVITY = new Vector3f(0.0f, -9.81f, 0.0f);
    public static final int X_ROTATION_DOF = 3 + PhysicsSpace.AXIS_X;
    public static final int Y_ROTATION_DOF = 3 + PhysicsSpace.AXIS_Y;
    public static final int Z_ROTATION_DOF = 3 + PhysicsSpace.AXIS_Z;

    public static Vector3f getVelocityAtPoint(Vector3f linearVel, Vector3f rotationalVel, Vector3f point) {
        Vector3f velocityAtPoint = new Vector3f();
        rotationalVel.cross(point, velocityAtPoint);
        velocityAtPoint.addLocal(linearVel);
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

    public static PhysicsRaycastResult castRay(Vector3f from, Vector3f dir, Predicate<EnumBulletShapeType> ignoredBody) {
        Vector3fPool.openPool();
        List<PhysicsRayTestResult> results = new LinkedList<>();
        IPhysicsWorld iPhysicsWorld = DynamXContext.getPhysicsWorld();
        if (iPhysicsWorld != null) {
            iPhysicsWorld.getDynamicsWorld().rayTest(from, dir, results);
        }

        for (PhysicsRayTestResult result : results) {

            if (!(result.getCollisionObject() instanceof PhysicsRigidBody))
                continue;
            if (!ignoredBody.test(((BulletShapeType<?>) result.getCollisionObject().getUserObject()).getType()))
                continue;

            Vector3f hitPosition = Vector3fPool.get();
            DynamXMath.interpolateLinear(result.getHitFraction(), from, dir, hitPosition);

            float distance = result.getHitFraction() * dir.length();

            Vector3f hitNormalInWorld = Vector3fPool.get();
            result.getHitNormalLocal(hitNormalInWorld);

            PhysicsRigidBody hitBody = (PhysicsRigidBody) result.getCollisionObject();

            return new PhysicsRaycastResult(from, dir, hitPosition, distance, hitNormalInWorld, hitBody);
        }
        Vector3fPool.closePool();

        return null;
    }

    public static Vector3f getBodyLocalPoint(PhysicsCollisionObject rigidBody, Vector3f pointInWorld) {
        Vector3f bodyLocation = Vector3fPool.get();
        rigidBody.getPhysicsLocation(bodyLocation);

        Vector3f pickPosition = pointInWorld.subtract(bodyLocation);

        Quaternion bodyRotation = QuaternionPool.get();
        rigidBody.getPhysicsRotation(bodyRotation);

        return bodyRotation.inverse().multLocal(pickPosition);
    }

    public static void createExplosion(PhysicsEntity<?> physicsEntity, Vector3f explosionPosition, double explosionStrength) {
        if (physicsEntity.getPhysicsHandler() != null) {
            PhysicsRigidBody body = (PhysicsRigidBody) physicsEntity.getPhysicsHandler().getCollisionObject();
            Vector3f centerOfMass = physicsEntity.physicsPosition;
            double distance = DynamXGeometry.distanceBetween(explosionPosition, centerOfMass);
            Vector3f direction = centerOfMass.subtract(explosionPosition).normalize().add(new Vector3f(0.0f, 2.0f, 0.0f)).normalize();
            double forgeStrength = (1.0D - MathHelper.clamp(distance / explosionStrength * 2.0D, 0.0D, 1.0D)) * 15.0D;
            body.activate();
            body.setLinearVelocity(body.getLinearVelocity(null).add(new Vector3f(
                    (float) (direction.x * forgeStrength),
                    (float) (direction.y * forgeStrength),
                    (float) (direction.z * forgeStrength))));
        }
    }


    public static Vector3f calculateConvexCenter(HullCollisionShape shape, Vector3f planePos, Vector3f planeNormal) {
        Vector3f sum = Vector3fPool.get();
        float[] copyHullVertices = shape.copyHullVertices();
        for (int i = 0; i < copyHullVertices.length; i++) {
            Vector3f point = Vector3fPool.get(copyHullVertices[i], copyHullVertices[i + 1], copyHullVertices[i + 2]);
            point.subtractLocal(planePos);
            if (point.dot(planeNormal) < 0) {
                sum.addLocal(point);
            }
            sum.divideLocal(shape.countHullVertices());
        }

        return sum;
    }

    public static Vector3f calculateBuoyantCenter(PhysicsRigidBody rigidBody, Vector3f planePos, Vector3f planeNormal) {
        Vector3f center = Vector3fPool.get();

        Transform rigidBodyTrans = TransformPool.get();
        Vector3f relPlanePos = Vector3fPool.get();
        rigidBodyTrans.invert().transformVector(planePos, relPlanePos);
        Vector3f relPlaneNormal = DynamXGeometry.rotateVectorByQuaternion(planeNormal, rigidBodyTrans.getRotation());

        CollisionShape collisionShape = rigidBody.getCollisionShape();
        //COMPOUND_SHAPE_PROXYTYPE = 31, replace this with an instanceof ?
        if (collisionShape.getShapeType() == 31) {
            CompoundCollisionShape compoundCollisionShape = (CompoundCollisionShape) collisionShape;
            for (ChildCollisionShape childCollisionShape : compoundCollisionShape.listChildren()) {
                //CONVEX_HULL_SHAPE_PROXYTYPE = 4, replace this with an instanceof ?
                if (childCollisionShape.getShape().getShapeType() == 4) {
                    center.addLocal(calculateConvexCenter((HullCollisionShape) childCollisionShape.getShape(), relPlanePos, relPlaneNormal));
                }
            }
            int numChildren = compoundCollisionShape.countChildren();
            if (numChildren > 0) {
                center.divideLocal(numChildren);
            }
        }
        return center;
    }
}
