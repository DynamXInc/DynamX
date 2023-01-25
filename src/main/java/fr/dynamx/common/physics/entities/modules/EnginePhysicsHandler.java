package fr.dynamx.common.physics.entities.modules;

import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.common.contentpack.type.vehicle.GearInfo;
import fr.dynamx.common.entities.modules.CarEngineModule;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.parts.engine.AutomaticGearboxHandler;
import fr.dynamx.common.physics.entities.parts.engine.Engine;
import fr.dynamx.common.physics.entities.parts.engine.GearBox;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @see IEnginePhysicsHandler
 * @see CarEngineModule
 */
public class EnginePhysicsHandler implements IPackInfoReloadListener {
    //TODO HANDLE STEERING IN CAR PHYSICS HANDLER
    private final CarEngineModule module;
    private final BaseVehiclePhysicsHandler<?> handler;
    private final WheelsPhysicsHandler propulsionHandler;
    @Getter
    @Setter
    private Engine engine;
    @Getter
    @Setter
    private GearBox gearBox;
    private AutomaticGearboxHandler gearBoxHandler;
    @Getter
    private float accelerationForce;
    @Getter
    private float steeringForce ;

    public EnginePhysicsHandler(CarEngineModule module, BaseVehiclePhysicsHandler<?> handler, WheelsPhysicsHandler propulsionHandler) {
        this.module = module;
        this.handler = handler;
        this.propulsionHandler = propulsionHandler;
        onPackInfosReloaded();
    }

    @Override
    public void onPackInfosReloaded() {
        engine = new Engine(module.getEngineInfo());
        List<GearInfo> gears = module.getEngineInfo().gears;
        gearBox = new GearBox(gears.size());
        for (int i = 0; i < gears.size(); i++) {
            GearInfo gear = gears.get(i);
            gearBox.setGear(i, gear.getSpeedRange()[0], gear.getSpeedRange()[1], gear.getRpmRange()[0], gear.getRpmRange()[1]);
        }
        //TODO BOUGER ça
        if (propulsionHandler instanceof WheelsPhysicsHandler)
            gearBoxHandler = new AutomaticGearboxHandler(this, gearBox, (WheelsPhysicsHandler) propulsionHandler);// propulsionHandler.createGearBox(module, this);
    }

    public void update() {
        switch (module.getEngineInfo().steeringMethod) {
            case 0:
                updateTurn0();
                break;
            case 1:
                updateTurn1();
                break;
            case 2:
                updateTurn2();
                break;
        }
        updateMovement();
        setEngineStarted(module.isEngineStarted());
        if (gearBoxHandler != null)
            gearBoxHandler.update(accelerationForce);
    }

    public void updateTurn0() {
        float maxSteerForce = 1.0f;
        float turnSpeed = module.getEngineInfo().getTurnSpeed();
        if (module.isTurningLeft()) {
            steeringForce = Math.min(steeringForce + turnSpeed, maxSteerForce);
            // vehicle.getVehicleControl().steer(steeringValue);
            steer(steeringForce);
        } else if (module.isTurningRight()) {
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

    public static float steeringBase = 0.3f, steeringInc = 0.04f, maxSteering = 1.2f, deSteeringFactor = 3;

    private float steeringTimeG = steeringBase;
    private float steeringTimeD = steeringBase;

    public void updateTurn1() {
        float maxSteerForce = 1.0f;
        float turnSpeed = 1;//module.getEngineInfo().getTurnSpeed();
        if (module.isTurningLeft()) {
            steeringTimeD = steeringBase;
            if (steeringTimeG < maxSteering)
                steeringTimeG += steeringInc;
            //System.out.println("G "+steeringTimeG+" "+steeringForce);
            steeringForce = Math.min(turnSpeed * steeringTimeG, maxSteerForce);
            // vehicle.getVehicleControl().steer(steeringValue);
            steer(steeringForce);
        } else if (module.isTurningRight()) {
            steeringTimeG = steeringBase;
            if (steeringTimeD < maxSteering)
                steeringTimeD += steeringInc;
            steeringForce = Math.max(-turnSpeed * steeringTimeD, -maxSteerForce);
            steer(steeringForce);
        } else {
            steeringTimeG = steeringBase;
            steeringTimeD = steeringBase;
            deSteeringFactor = 0.3f;
            turnSpeed *= deSteeringFactor;
            //turnSpeed *= (1.4-steeringTime);
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

    private float steeringTime;

    public void updateTurn2() {
        float maxSteerForce = 1.0f;
        float turnSpeed = 1;//module.getEngineInfo().getTurnSpeed();
        if (module.isTurningLeft()) {
            steeringTimeD = steeringBase;
            if (steeringTimeG < maxSteering)
                steeringTimeG += steeringInc;
            //System.out.println("G "+steeringTimeG+" "+steeringForce);
            steeringForce = Math.min(turnSpeed * steeringTimeG, maxSteerForce);
            // vehicle.getVehicleControl().steer(steeringValue);
            steer(steeringForce);
        } else if (module.isTurningRight()) {
            steeringTimeG = steeringBase;
            if (steeringTimeD < maxSteering)
                steeringTimeD += steeringInc;
            steeringForce = Math.max(-turnSpeed * steeringTimeD, -maxSteerForce);
            steer(steeringForce);
        } else {
            steeringTimeG = steeringBase;
            steeringTimeD = steeringBase;
            deSteeringFactor = 0.3f;
            turnSpeed *= deSteeringFactor;
            //turnSpeed *= (1.4-steeringTime);
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

    public static boolean inTestFullGo;

    public void updateMovement() {
        accelerationForce = 0;

        // do braking first so it doesn't override engineBraking.
        if (module.isHandBraking() && !inTestFullGo) {
            handbrake(1);
        } else {
            brake(0);
        }

        if (module.isAccelerating()) {
            if (inTestFullGo) {
                module.setSpeedLimit(80);
            }
            if (handler.getSpeed(BaseVehiclePhysicsHandler.SpeedUnit.KMH) < -1f) //reversing
            {
                disengageEngine();
                brake(1f);
            } else if (module.isEngineStarted()) {
                accelerate(1);
            } else {
                applyEngineBraking();
                accelerate(0);
            }
        } else if (module.isReversing()) {
            if (handler.getSpeed(BaseVehiclePhysicsHandler.SpeedUnit.KMH) > 1) //going forward
            {
                disengageEngine();
                brake(1f);
            } else if (module.isEngineStarted()) {
                accelerate(-1);
            } else {
                applyEngineBraking();
                accelerate(0);
            }
        } else {
            applyEngineBraking();
            accelerate(0);
        }
    }

    public boolean isEngaged() {
        return getGearBox().getActiveGearNum() != 0;
    }

    public void setEngineStarted(boolean started) {
        if (engine != null) {
            if (started) {
                startEngine();
            } else {
                stopEngine();
            }
        }
    }

    public void startEngine() {
        if (!engine.isStarted()) {
            engine.setStarted(true);
        }
    }

    public void stopEngine() {
        if (engine.isStarted()) {
            engine.setStarted(false);
        }
    }

    public void syncActiveGear(int activeGearNum) {
        gearBox.syncActiveGearNum(activeGearNum);
    }

    public void accelerate(float strength) {
        this.accelerationForce = strength;
        propulsionHandler.accelerate(module, strength, module.getRealSpeedLimit());
    }

    public void disengageEngine() {
        propulsionHandler.disengageEngine();
    }

    public void brake(float strength) {
        propulsionHandler.brake(strength);
    }

    public void handbrake(float strength) {
        propulsionHandler.handbrake(strength);
    }

    public void steer(float strength) {
        propulsionHandler.steer(strength);
    }

    public void applyEngineBraking() {
        propulsionHandler.applyEngineBraking(module);
    }
}
