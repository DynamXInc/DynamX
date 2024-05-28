package fr.dynamx.common.physics.entities.parts.engine;

import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.modules.engines.BoatPropellerModule;
import fr.dynamx.common.physics.entities.BoatPhysicsHandler;
import fr.dynamx.common.physics.entities.modules.EnginePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.WheelsPhysicsHandler;
import fr.dynamx.common.physics.entities.parts.wheel.WheelPhysics;
import fr.hermes.forge1122.dynamx.DynamXConfig;
import fr.dynamx.utils.maths.DynamXMath;
import lombok.RequiredArgsConstructor;
import net.minecraftforge.fml.relauncher.Side;

@RequiredArgsConstructor
public abstract class AutomaticGearboxHandler {
    private final Engine engine;
    private final GearBox gearBox;
    private float targetRPM;

    public void update(float currentAcceleration) {
        if (gearBox == null)
            return;
        if (engine.isStarted()) {
            float revs = engine.getRevs() * engine.getMaxRevs();
            GearBox.GearData gear = gearBox.getActiveGear();
            boolean gearChanged = false;
            int changeCounter = gearBox.updateGearChangeCounter();
            int oldGear = gearBox.getActiveGearNum();
            if (changeCounter <= 2) {
                if (revs > gear.getRpmEnd() - 100) //Sur-régime : on passe la vitesse supérieure
                {
                    gearChanged = gearBox.increaseGear();
                } else if (revs < gear.getRpmStart() + 100) //Sous-régime : on diminue la vitesse
                {
                    gearChanged = gearBox.decreaseGear();
                }
            }
            if (gearBox.getActiveGearNum() == 0 && currentAcceleration != 0) //Accération en étant au point mort : on passe la première
            {
                gearBox.setActiveGearNum(currentAcceleration > 0 ? 1 : -1);
                gearChanged = false;
            } else if (setNeutralWhenNotAccelerating() && currentAcceleration == 0 && gearBox.getActiveGearNum() != 0) //On accélère pas : on passe au point mort
            {
                gearBox.setActiveGearNum(0);
                gearChanged = true;
            }
            if (gearBox.getActiveGearNum() != 0) //une vitesse est passée, on get les rpm correspondant à la vitesse, dans la gamme de rpm "autorisés"
            {
                float vehicleSpeed = getVehicleSpeed();
                if (gearChanged && oldGear != 0) {
                    revs = engine.getRevs();
                    targetRPM = gearBox.getRPM(engine, vehicleSpeed);
                    targetRPM = DynamXMath.clamp(targetRPM, 0, 1);
                } else if (changeCounter > 0) {
                    //targetRPM = gearBox.getRPM(engine, vehicle.getSpeed(Vehicle.SpeedUnit.KMH));
                    //targetRPM = DynamXMath.clamp(targetRPM, 0, 1);

                    revs = engine.getRevs();
                    float drev = targetRPM - revs;
                    drev /= gearBox.getGearChangeTime();
                    revs = revs + drev;
                } else {
                    revs = gearBox.getRPM(engine, vehicleSpeed);
                    revs = DynamXMath.clamp(revs, 0, 1);
                }
            } else {
                targetRPM = gearBox.getActiveGear().getRpmStart() / engine.getMaxRevs();
                revs = engine.getRevs();
                if (revs != targetRPM) {
                    float drev = targetRPM - revs;
                    drev /= DynamXConfig.gearChangeDelay; //Interpolation : take some ticks to come back to required speed
                    revs = revs + drev;
                }
            }
            revs = DynamXMath.clamp(revs, 0, 1);
            engine.setRevs(revs);
        } else {
            gearBox.setActiveGearNum(0);
            engine.setRevs(0);
        }
    }

    protected boolean setNeutralWhenNotAccelerating() {
        return false;
    }

    /**
     * Used to compute the revs of the engine
     *
     * @return The speed of the vehicle, in km/h (for example, the speed at the wheel)
     */
    protected abstract float getVehicleSpeed();

    public static class CarGearBox extends AutomaticGearboxHandler {
        private final WheelsPhysicsHandler wheels;

        public CarGearBox(EnginePhysicsHandler vehicle, GearBox gearBox, WheelsPhysicsHandler wheels) {
            super(vehicle.getEngine(), gearBox);
            this.wheels = wheels;
        }

        @Override
        protected float getVehicleSpeed() {
            float wheelRotationSpeed = 0;
            int j = 0;
            for (int i = 0; i < wheels.getNumWheels(); i++) {
                WheelPhysics wheelPhysics = wheels.getWheel(i);
                if (wheelPhysics.isDrivingWheel()) {
                    wheelRotationSpeed += wheelPhysics.getDeltaRotation() * wheelPhysics.getPhysicsWheel().getRadius();
                    j++;
                }
            }
            wheelRotationSpeed /= j;
            wheelRotationSpeed *= (float) (3.6 * 20 * 0.05f / DynamXContext.getPhysicsSimulationMode(Side.SERVER).getTimeStep());
            return wheelRotationSpeed;
        }
    }

    public static class BoatGearBox extends AutomaticGearboxHandler {
        private final BoatPhysicsHandler<?> vehicle;

        public BoatGearBox(BoatPhysicsHandler<?> vehicle, BoatPropellerModule.BoatPropellerHandler enginePhysics, GearBox gearBox) {
            super(enginePhysics.getEngine(), gearBox);
            this.vehicle = vehicle;
        }

        @Override
        protected float getVehicleSpeed() {
            return vehicle.getSpeedOnZAxisInBoatSpace();
        }

        @Override
        protected boolean setNeutralWhenNotAccelerating() {
            return true;
        }
    }
}
