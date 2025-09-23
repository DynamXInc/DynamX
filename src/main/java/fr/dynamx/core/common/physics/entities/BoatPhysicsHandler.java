package fr.dynamx.core.common.physics.entities;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.core.common.entities.vehicles.BoatEntity;
import fr.dynamx.core.utils.maths.DynamXGeometry;
import fr.hermes.forge.JmeVector3fPool;

public class BoatPhysicsHandler<T extends BoatEntity<?>> extends BaseVehiclePhysicsHandler<T> {
    public BoatPhysicsHandler(T entity) {
        super(entity);
    }

    @Override
    public PhysicsRigidBody createShape(Vector3f position, Quaternion rotation, float spawnRotation) {
        PhysicsRigidBody shape = super.createShape(position, rotation, spawnRotation);
        shape.setEnableSleep(false);
        return shape;
    }

    public float getSpeedOnZAxisInBoatSpace() {
        Vector3f linearVelocity = getCollisionObject().getLinearVelocity(JmeVector3fPool.get());
        // velocity in body space
        Vector3f velocity = DynamXGeometry.rotateVectorByQuaternion(linearVelocity, getRotation().inverse());
        return (velocity.z + Math.abs(velocity.x)) * 3.6f;
    }
}
