package fr.dynamx.common.entities.modules;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.client.handlers.hud.BoatController;
import fr.dynamx.common.contentpack.type.vehicle.BoatPropellerInfo;
import fr.dynamx.common.entities.vehicles.BoatEntity;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.Objects;

public class BoatPropellerModule extends BasicEngineModule implements IPackInfoReloadListener {
    protected BoatPropellerInfo info;
    protected BoatPropellerHandler physicsHandler;

    public BoatPropellerModule(BoatEntity<?> entityEntity) {
        super(entityEntity);
        onPackInfosReloaded();
    }

    @Override
    public void onPackInfosReloaded() {
        this.info = Objects.requireNonNull(entity.getPackInfo().getSubPropertyByType(BoatPropellerInfo.class));
    }

    public BoatPropellerHandler getPhysicsHandler() {
        return physicsHandler;
    }

    @Override
    public void initPhysicsEntity(@Nullable BaseVehiclePhysicsHandler<?> handler) {
        if (handler != null) {
            physicsHandler = new BoatPropellerHandler();
        }
    }

    @Override
    public void preUpdatePhysics(boolean simulatePhysics) {
        if (simulatePhysics) {
            physicsHandler.update();
        }
    }

    @Nullable
    @Override
    public IVehicleController createNewController() {
        return new BoatController(entity, this);
    }

    @Override
    public boolean isEngineStarted() {
        return true;
    }

    @Override
    protected void playStartingSound() {
        // no
    }

    @Override
    protected String getStartingSound(boolean forInterior) {
        return null;
    }

    @Override
    protected void updateSounds() {

    }

    @Override
    public float getSoundPitch() {
        return 0;
    }

    public class BoatPropellerHandler {
        @Getter
        private float accelerationForce;
        @Getter
        private float steeringForce ;

        public void update() {
            updateTurn0();
            updateMovement();
        }

        public void updateTurn0() {
            float maxSteerForce = 1.0f;
            float turnSpeed = 0.09f;
            if (isTurningLeft()) {
                steeringForce = Math.min(steeringForce + turnSpeed, maxSteerForce);
                // vehicle.getVehicleControl().steer(steeringValue);
                steer(steeringForce);
            } else if (isTurningRight()) {
                steeringForce = Math.max(steeringForce - turnSpeed, -maxSteerForce);
                steer(steeringForce);
            } else {
                turnSpeed *= 4;
                if (steeringForce > 0) {
                    steeringForce -= turnSpeed;
                }
                if (steeringForce < 0) {
                    steeringForce += turnSpeed;
                }
                if (Math.abs(steeringForce) < turnSpeed)
                    steeringForce = 0;
            }
            steer(steeringForce);
        }

        public void updateMovement() {
            accelerationForce = 0;

            // do braking first so it doesn't override engineBraking.
            brake(0);

            if (isAccelerating()) {
                if (entity.getPhysicsHandler().getSpeed(BaseVehiclePhysicsHandler.SpeedUnit.KMH) < -1f) //reversing
                {
                    brake(1f);
                } else if (isEngineStarted()) {
                    accelerate(1);
                } else {
                    accelerate(0);
                }
            } else if (isReversing()) {
                if (entity.getPhysicsHandler().getSpeed(BaseVehiclePhysicsHandler.SpeedUnit.KMH) > 1) //going forward
                {
                    brake(1f);
                } else if (isEngineStarted()) {
                    accelerate(-1);
                } else {
                    accelerate(0);
                }
            } else {
                accelerate(0);
            }
        }

        public void accelerate(float strength) {
            Vector3f look = DynamXGeometry.FORWARD_DIRECTION;
            look = DynamXGeometry.rotateVectorByQuaternion(look, entity.physicsRotation);
            look.multLocal(getAccelerationForce() * strength);
            Vector3f rotatedPos = DynamXGeometry.rotateVectorByQuaternion(info.getPosition(), entity.physicsRotation);
            entity.physicsHandler.getCollisionObject().applyForce(look, rotatedPos);
        }

        public void brake(float strength) {
            Vector3f look = DynamXGeometry.FORWARD_DIRECTION;
            look = DynamXGeometry.rotateVectorByQuaternion(look, entity.physicsRotation);
            look.multLocal(-getBrakeForce() * strength);
            entity.physicsHandler.getCollisionObject().applyForce(look, Vector3fPool.get());
        }

        public void steer(float strength) {
            Vector3f look = Vector3fPool.get(-1, 0, -0.8f);
            look = DynamXGeometry.rotateVectorByQuaternion(look, entity.physicsRotation);
            look.multLocal(getSteerForce() * strength * entity.physicsHandler.getLinearVelocity().length() / 3);
            Vector3f linearFactor = entity.physicsHandler.getCollisionObject().getLinearFactor(Vector3fPool.get());
            Vector3f rotatedPos = DynamXGeometry.rotateVectorByQuaternion(info.getPosition(), entity.physicsRotation);
            entity.physicsHandler.getCollisionObject().applyTorque(rotatedPos.cross(look.multLocal(linearFactor)));
        }

        public float getAccelerationForce() {
            return info.getAccelerationForce();
        }

        public float getBrakeForce() {
            return info.getBrakeForce();
        }

        public float getSteerForce() {
            return info.getSteerForce();
        }
    }
}
