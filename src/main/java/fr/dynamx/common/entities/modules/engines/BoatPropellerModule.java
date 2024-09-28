package fr.dynamx.common.entities.modules.engines;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.client.handlers.hud.BoatController;
import fr.dynamx.common.contentpack.type.vehicle.BoatEngineInfo;
import fr.dynamx.common.contentpack.type.vehicle.BoatPropellerInfo;
import fr.dynamx.common.contentpack.type.vehicle.GearInfo;
import fr.dynamx.common.entities.vehicles.BoatEntity;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.BoatPhysicsHandler;
import fr.dynamx.common.physics.entities.parts.engine.AutomaticGearboxHandler;
import fr.dynamx.common.physics.entities.parts.engine.Engine;
import fr.dynamx.common.physics.entities.parts.engine.GearBox;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public class BoatPropellerModule extends BasicEngineModule implements IPackInfoReloadListener {
    @Getter
    protected BoatEngineInfo engineInfo;
    protected BoatPropellerInfo info;
    protected BoatPropellerHandler propellerPhysicsHandler;
    protected BoatPhysicsHandler<?> boatPhysicsHandler;
    @Getter
    protected float bladeAngle;

    public BoatPropellerModule(BoatEntity<?> entityEntity) {
        super(entityEntity);
        onPackInfosReloaded();
    }

    @Override
    public void onPackInfosReloaded() {
        this.info = Objects.requireNonNull(entity.getPackInfo().getSubPropertyByType(BoatPropellerInfo.class));
        this.engineInfo = entity.getPackInfo().getSubPropertyByType(BoatEngineInfo.class);
        if (propellerPhysicsHandler != null)
            propellerPhysicsHandler.onPackInfosReloaded();
        super.onPackInfosReloaded();
    }

    public BoatPropellerHandler getPropellerPhysicsHandler() {
        return propellerPhysicsHandler;
    }

    @Override
    public void initPhysicsEntity(@Nullable BaseVehiclePhysicsHandler<?> handler) {
        if (handler != null) {
            propellerPhysicsHandler = new BoatPropellerHandler();
            boatPhysicsHandler = (BoatPhysicsHandler<?>) entity.getPhysicsHandler();
        }
    }

    @Override
    public void preUpdatePhysics(boolean simulatePhysics) {
        if (simulatePhysics) {
            propellerPhysicsHandler.update();
        }
    }

    @Override
    public void postUpdatePhysics(boolean simulatingPhysics) {
        super.postUpdatePhysics(simulatingPhysics);
        if (simulatingPhysics && engineInfo != null) {
            this.getEngineProperties()[VehicleEntityProperties.EnumEngineProperties.REVS.ordinal()] = propellerPhysicsHandler.getEngine().getRevs();
            this.getEngineProperties()[VehicleEntityProperties.EnumEngineProperties.ACTIVE_GEAR.ordinal()] = propellerPhysicsHandler.getGearBox().getActiveGearNum();
        }
    }

    @Nullable
    @Override
    public IVehicleController createNewController() {
        return new BoatController(entity, this);
    }

    @Override
    public boolean isEngineStarted() {
        return !hasEngine() || super.isEngineStarted();
    }

    public boolean hasEngine() {
        return engineInfo != null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateEntity() {
        super.updateEntity();
        if (hasEngine()) {
            //float targetPower = this.getEngineProperties()[VehicleEntityProperties.EnumEngineProperties.REVS.ordinal()];
            //curPower = curPower + (targetPower - curPower) / 60; //3-seconds interpolation
            bladeAngle += getRevs();
        }
    }

    public float getRevs() {
        return !hasEngine() ? 0 : this.getEngineProperties()[VehicleEntityProperties.EnumEngineProperties.REVS.ordinal()];
    }

    public class BoatPropellerHandler implements IPackInfoReloadListener {
        @Getter
        @Setter
        private Engine engine;
        @Getter
        @Setter
        private GearBox gearBox;
        private AutomaticGearboxHandler gearBoxHandler;

        @Getter
        private float physicsAccelerationForce;
        @Getter
        private float physicsSteeringForce;

        protected BoatPropellerHandler() {
            onPackInfosReloaded();
        }

        @Override
        public void onPackInfosReloaded() {
            if (hasEngine()) {
                engine = new Engine(getEngineInfo());
                List<GearInfo> gears = getEngineInfo().gears;
                gearBox = new GearBox(gears.size());
                for (int i = 0; i < gears.size(); i++) {
                    GearInfo gear = gears.get(i);
                    gearBox.setGear(i, gear.getSpeedRange()[0], gear.getSpeedRange()[1], gear.getRpmRange()[0], gear.getRpmRange()[1]);
                }
                //TODO move this (cf EnginePhysicsHandler)
                gearBoxHandler = new AutomaticGearboxHandler.BoatGearBox((BoatPhysicsHandler<?>) entity.physicsHandler, this, gearBox);// propulsionHandler.createGearBox(module, this);
            } else {
                engine = null;
                gearBox = null;
                gearBoxHandler = null;
            }
        }

        public void update() {
            if (entity.isInWater()) {
                updateTurn0();
                updateMovement();
            } else {
                physicsSteeringForce = 0;
                accelerate(0);
            }
            if (!hasEngine())
                return;
            setEngineStarted(BoatPropellerModule.this.isEngineStarted());
            if (gearBoxHandler != null)
                gearBoxHandler.update(physicsAccelerationForce);
        }

        public void updateTurn0() {
            float maxSteerForce = 1.0f;
            float turnSpeed = 0.09f;
            if (isTurningLeft()) {
                physicsSteeringForce = Math.min(physicsSteeringForce + turnSpeed, maxSteerForce);
                // vehicle.getVehicleControl().steer(steeringValue);
                steer(physicsSteeringForce);
            } else if (isTurningRight()) {
                physicsSteeringForce = Math.max(physicsSteeringForce - turnSpeed, -maxSteerForce);
                steer(physicsSteeringForce);
            } else {
                turnSpeed *= 4;
                if (physicsSteeringForce > 0) {
                    physicsSteeringForce -= turnSpeed;
                }
                if (physicsSteeringForce < 0) {
                    physicsSteeringForce += turnSpeed;
                }
                if (Math.abs(physicsSteeringForce) < turnSpeed)
                    physicsSteeringForce = 0;
            }
            steer(physicsSteeringForce);
        }

        public void updateMovement() {
            physicsAccelerationForce = 0;
            // do braking first so it doesn't override engineBraking.
            brake(0);
            if (isAccelerating()) {
                if (boatPhysicsHandler.getSpeedOnZAxisInBoatSpace() < -1f) //reversing
                {
                    brake(-1f);
                } else if (isEngineStarted()) {
                    accelerate(1);
                } else {
                    accelerate(0);
                }
            } else if (isReversing()) {
                if (boatPhysicsHandler.getSpeedOnZAxisInBoatSpace() > 1) //going forward
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
            this.physicsAccelerationForce = strength;
            if (hasEngine()) {
                float power = getEngine().getPowerOutputAtRevs() / 1000;
                strength = power * strength;
            }
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
            Vector3f look = Vector3fPool.get(boatPhysicsHandler.getSpeedOnZAxisInBoatSpace() < 0 ? 1 : -1, 0, 0);
            look = DynamXGeometry.rotateVectorByQuaternion(look, entity.physicsRotation);
            look.multLocal(getSteerForce() * strength);
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

        public void setEngineStarted(boolean started) {
            if (engine == null) {
                return;
            }
            if (started && !engine.isStarted()) {
                engine.setStarted(true);
            } else if (!started && engine.isStarted()) {
                engine.setStarted(false);
            }
        }
    }
}
