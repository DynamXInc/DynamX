package fr.dynamx.common.physics.entities;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.physics.entities.modules.EnginePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.WheelsPhysicsHandler;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;

/**
 * Physics handler of {@link BaseVehicleEntity} <br>
 * The physics handler is the bridge between the minecraft entity and the physics engine
 *
 * @param <T> The entity type
 * @see BaseWheeledVehiclePhysicsHandler
 */
public abstract class BaseVehiclePhysicsHandler<T extends BaseVehicleEntity<?>> extends PackEntityPhysicsHandler<ModularVehicleInfo, T> {
    public enum SpeedUnit {KMH, MPH}

    public static final float KMH_TO_MPH = 0.62137f;

    private boolean forceActivation;

    public BaseVehiclePhysicsHandler(T entity) {
        super(entity);
    }

    @Override
    public PhysicsRigidBody createShape(Vector3f position, Quaternion rotation, float spawnRotation) {
        Vector3f tmp = Vector3fPool.get(position);
        Transform transform = new Transform(tmp, QuaternionPool.get(rotation));
        ModularVehicleInfo modularVehicleInfo = getHandledEntity().getPackInfo();

        //Don't use this.getPackInfo() : it isn't initialized yet
        PhysicsRigidBody vehicleBody = new PhysicsRigidBody(modularVehicleInfo.getPhysicsCollisionShape(), modularVehicleInfo.getEmptyMass());
        vehicleBody.setPhysicsTransform(transform);
        vehicleBody.setUserObject(new BulletShapeType<>(EnumBulletShapeType.VEHICLE, getHandledEntity()));
        vehicleBody.setSleepingThresholds(0.9f, 1.2f);

        vehicleBody.setDamping(modularVehicleInfo.getLinearDamping(), modularVehicleInfo.getAngularDamping());
        return vehicleBody;
    }

    @Override
    public void update() {
        super.update();
        if (!EnginePhysicsHandler.inTestFullGo && getCollisionObject().getActivationState() == 4 && getHandledEntity().getControllingPassenger() == null) {
            getCollisionObject().setEnableSleep(true);
        }
        if (EnginePhysicsHandler.inTestFullGo)
            getCollisionObject().activate();
    }

    @Override
    public void setForceActivation(boolean force) {
        if (EnginePhysicsHandler.inTestFullGo)
            force = true;
        super.setForceActivation(force);
        forceActivation = force;
        getCollisionObject().setEnableSleep(!forceActivation);
        getCollisionObject().activate();
    }

    public boolean isActivationForced() {
        return forceActivation;
    }

    public float getSpeed(SpeedUnit speedUnit) {
        switch (speedUnit) {
            case KMH:
                return getCollisionObject().getLinearVelocity(Vector3fPool.get()).length() * 3.6f;
            case MPH:
                return getCollisionObject().getLinearVelocity(Vector3fPool.get()).length() * 3.6f * KMH_TO_MPH;
        }
        return 0;
    }

    public WheelsPhysicsHandler getWheels() {
        return null;
    }
}
