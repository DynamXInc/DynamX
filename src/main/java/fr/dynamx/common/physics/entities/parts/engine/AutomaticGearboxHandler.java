package fr.dynamx.common.physics.entities.parts.engine;

import fr.dynamx.api.physics.entities.IGearBoxHandler;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.physics.entities.modules.EnginePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.WheelsPhysicsHandler;
import fr.dynamx.common.physics.entities.parts.wheel.WheelPhysics;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.maths.DynamXMath;
import lombok.RequiredArgsConstructor;
import net.minecraftforge.fml.relauncher.Side;

@RequiredArgsConstructor
public class AutomaticGearboxHandler implements IGearBoxHandler {
    private final EnginePhysicsHandler vehicle;
    private final GearBox gearBox;
    private final WheelsPhysicsHandler wheels;
    private float targetRPM;

    public void update(float currentAcceleration) {
        if (gearBox == null)
            return;
        if (vehicle.getEngine().isStarted()) {
            float revs = vehicle.getEngine().getRevs() * vehicle.getEngine().getMaxRevs();
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
            }
            if (gearBox.getActiveGearNum() != 0) //une vitesse est passée, on get les rpm correspondant à la vitesse, dans la gamme de rpm "autorisés"
            {
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
                wheelRotationSpeed *= 3.6 * 20 * 0.05f / DynamXContext.getPhysicsSimulationMode(Side.SERVER).getTimeStep();
                if (gearChanged && oldGear != 0) {
                    revs = vehicle.getEngine().getRevs();
                    targetRPM = gearBox.getRPM(vehicle.getEngine(), wheelRotationSpeed);
                    targetRPM = DynamXMath.clamp(targetRPM, 0, 1);
                } else if (changeCounter > 0) {
                    //targetRPM = gearBox.getRPM(vehicle.getEngine(), vehicle.getSpeed(Vehicle.SpeedUnit.KMH));
                    //targetRPM = DynamXMath.clamp(targetRPM, 0, 1);

                    revs = vehicle.getEngine().getRevs();
                    float drev = targetRPM - revs;
                    drev /= gearBox.getGearChangeTime();
                    revs = revs + drev;
                } else {
                    revs = gearBox.getRPM(vehicle.getEngine(), wheelRotationSpeed);
                    revs = DynamXMath.clamp(revs, 0, 1);
                }
            } else {
                targetRPM = gearBox.getActiveGear().getRpmStart() / vehicle.getEngine().getMaxRevs();
                revs = vehicle.getEngine().getRevs();
                if (revs != targetRPM) {
                    float drev = targetRPM - revs;
                    drev /= DynamXConfig.gearChangeDelay; //Interpolation : take some ticks to come back to required speed
                    revs = revs + drev;
                }
            }
            revs = DynamXMath.clamp(revs, 0, 1);
            vehicle.getEngine().setRevs(revs);
        } else {
            gearBox.setActiveGearNum(0);
            vehicle.getEngine().setRevs(0);
        }
    }
}
