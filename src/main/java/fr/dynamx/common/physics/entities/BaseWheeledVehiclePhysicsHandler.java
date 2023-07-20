package fr.dynamx.common.physics.entities;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.objects.PhysicsVehicle;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.type.vehicle.FrictionPoint;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import jme3utilities.math.MyQuaternion;
import lombok.Getter;

/**
 * Physics handler of {@link BaseVehicleEntity}, for vehicles that have wheels (using the bullet's {@link PhysicsVehicle}) <br>
 * The physics handler is the bridge between the minecraft entity and the physics engine
 *
 * @param <T> The entity type
 */
public abstract class BaseWheeledVehiclePhysicsHandler<T extends BaseVehicleEntity<?>> extends BaseVehiclePhysicsHandler<T> {
    @Getter
    private PhysicsVehicle physicsVehicle;

    public BaseWheeledVehiclePhysicsHandler(T entity) {
        super(entity);
    }

    @Override
    public PhysicsRigidBody createShape(Vector3f position, Quaternion rotation, float spawnRotation) {
        if (MyQuaternion.isZero(rotation)) {
            DynamXMain.log.warn("Resetting physics rotation of entity " + handledEntity);
            rotation = DynamXGeometry.rotationYawToQuaternion(spawnRotation);
        }
        Transform transform = new Transform(position, QuaternionPool.get(rotation));
        ModularVehicleInfo modularVehicleInfo = getHandledEntity().getPackInfo();

        //Don't use this.getPackInfo() : it isn't initialized yet
        physicsVehicle = new PhysicsVehicle(modularVehicleInfo.getPhysicsCollisionShape(), modularVehicleInfo.getEmptyMass());
        physicsVehicle.setPhysicsTransform(transform);
        physicsVehicle.setUserObject(new BulletShapeType<>(EnumBulletShapeType.VEHICLE, getHandledEntity(), physicsVehicle.getCollisionShape()));
        physicsVehicle.setSleepingThresholds(0.3f, 1);
        return physicsVehicle;
    }

    @Override
    public void addToWorld() {
        IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(getHandledEntity().world);
        if (physicsWorld == null) {
            throw new NullPointerException("Physics world is null, wtf " + handledEntity.getEntityWorld() + " " + getCollisionObject());
        }
        physicsWorld.addVehicle((PhysicsVehicle) getCollisionObject());
    }

    @Override
    public void update() {
        super.update();
        if (!handledEntity.getPackInfo().getFrictionPoints().isEmpty() && isBodyActive()) {
            float horizSpeed = Vector3fPool.get(getLinearVelocity().x, 0, getLinearVelocity().z).length();
            for (FrictionPoint f : handledEntity.getPackInfo().getFrictionPoints()) {
                Vector3f pushDown = new Vector3f(-getLinearVelocity().x, -horizSpeed, -getLinearVelocity().z);
                pushDown.multLocal(f.getIntensity());
                applyForce(f.getPosition(), pushDown);
            }
        }
    }

    public float getSpeed(SpeedUnit speedUnit) {
        switch (speedUnit) {
            case KMH:
                return this.physicsVehicle.getCurrentVehicleSpeedKmHour();
            case MPH:
                return this.physicsVehicle.getCurrentVehicleSpeedKmHour() * KMH_TO_MPH;
            default:
                return -1;
        }
    }

    @Override
    public void removePhysicsEntity() {
        if (physicsVehicle != null) {
            DynamXContext.getPhysicsWorld(getHandledEntity().world).removeVehicle(physicsVehicle);
        }
    }
}
