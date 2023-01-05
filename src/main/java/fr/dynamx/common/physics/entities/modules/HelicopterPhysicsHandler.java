package fr.dynamx.common.physics.entities.modules;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.modules.IEngineModule;
import fr.dynamx.api.physics.entities.IPropulsionHandler;
import fr.dynamx.common.contentpack.parts.PartHandle;
import fr.dynamx.common.contentpack.parts.PartRotor;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.HelicopterPropulsionModule;
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;

/**
 * @see IPropulsionHandler
 * @see WheelsModule
 */
public class HelicopterPhysicsHandler implements IPropulsionHandler
{
    private final HelicopterPropulsionModule module;
    private final BaseVehiclePhysicsHandler<? extends BaseVehicleEntity<?>> handler;

    private static final Vector3f direction = new Vector3f(0, -1, 0);
    private static final Vector3f axle = new Vector3f(-1, 0, 0);

    public HelicopterPhysicsHandler(HelicopterPropulsionModule module, BaseVehiclePhysicsHandler<? extends BaseVehicleEntity<?>> handler) {
        this.module = module;
        this.handler = handler;
    }

    public BaseVehiclePhysicsHandler<? extends BaseVehicleEntity<?>> getHandler() {
        return handler;
    }

    @Override
    public void accelerate(IEngineModule engine, float strength, float speedLimit) {

    }

    @Override
    public void disengageEngine() {
    }

    @Override
    public void brake(float strength) {
    }

    @Override
    public void handbrake(float strength) {
    }

    @Override
    public void steer(float strength) {
    }

    @Override
    public void applyEngineBraking(IEngineModule engine) {
    }


    public void addRotor(PartRotor partRotor) {

    }

    public void removeRotor(PartRotor partRotor) {

    }

    public void removeHandle(PartHandle partHandle) {
    }

    public void addHandle(PartHandle partHandle) {
    }
}
