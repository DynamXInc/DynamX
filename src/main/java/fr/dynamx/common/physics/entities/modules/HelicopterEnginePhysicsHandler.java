package fr.dynamx.common.physics.entities.modules;

import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.entities.IEnginePhysicsHandler;
import fr.dynamx.api.physics.entities.IPropulsionHandler;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.modules.EngineModule;
import fr.dynamx.common.entities.modules.HelicopterEngineModule;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.parts.engine.Engine;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

/**
 * @see IEnginePhysicsHandler
 * @see EngineModule
 */
public class HelicopterEnginePhysicsHandler implements IEnginePhysicsHandler {
    private final HelicopterEngineModule module;
    private final BaseVehiclePhysicsHandler<?> handler;
    private final IPropulsionHandler propulsionHandler;
    private Engine engine;

    private float accelerationForce;
    private float steeringForce = 0;

    public float upForce = 0;

    public HelicopterEnginePhysicsHandler(HelicopterEngineModule module, BaseVehiclePhysicsHandler<?> handler, IPropulsionHandler propulsionHandler) {
        this.module = module;
        this.handler = handler;
        byte i = 0;
        setEngine(new Engine(module.getEngineInfo()));
        this.propulsionHandler = propulsionHandler;
    }

    public void update() {
        updateTurn0();
        updateMovement();
        setEngineStarted(module.isEngineStarted());

       /* if(handler.getHandledEntity().getRidingEntity() == null) {
            upForce = 0;
        }*/
        if (upForce != 0) {
            handler.activate();
            Vector3f force = Vector3fPool.get(0, upForce, 0);
            force = DynamXGeometry.rotateVectorByQuaternion(force, handler.getRotation());
            //force.addLocal(DynamXContext.getPhysicsWorld().getDynamicsWorld().getGravity(Vector3fPool.get()));
            force.addLocal(0, 700, 0);
            handler.applyForce(Vector3fPool.get(), force);
        }
        if (handler.getHandledEntity().posY >= 1000) {
            handler.getHandledEntity().setDead();
        }
    }

    public static float orderedDx;
    public static float orderedDy;

    public void updateTurn0() {
        if (orderedDx != 0) {
            Vector3f force = Vector3fPool.get(0, -2500 * orderedDx, 0);
            force = DynamXGeometry.rotateVectorByQuaternion(force, handler.getRotation());
            handler.applyTorque(force);
            orderedDx = 0;
        }

        if (orderedDy != 0) {
            Vector3f force = Vector3fPool.get(-2500 * orderedDy, 0, 0);
            force = DynamXGeometry.rotateVectorByQuaternion(force, handler.getRotation());
            handler.applyTorque(force);
            orderedDy = 0;
        }

        if (module.isTurningLeft()) {
            steer(-1);
        } else if (module.isTurningRight()) {
            steer(1);
        }
    }

    public void updateMovement() {
        if (module.isAccelerating()) {
            accelerate(1);
        } else if (module.isReversing()) {
            brake(1f);
        } else if (module.isEngineStarted()) {
            accelerate(0);
        }
    }

    @Override
    public Engine getEngine() {
        return engine;
    }

    @Override //TODO IS FLYING ?
    public boolean isEngaged() {
        return true;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
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

    @Override //TODO THIS IS SHIT
    public void syncActiveGear(int activeGearNum) {
    }

    public void accelerate(float strength) {
        this.accelerationForce = strength;

        handler.setForceActivation(true);
        float grav = -1.02f * handler.getCollisionObject().getMass() * handler.getCollisionObject().getGravity(Vector3fPool.get()).y * DynamXContext.getPhysicsSimulationMode(Side.SERVER).getTimeStep() * 2;
        if (strength > 0) {
            Vector3f force = Vector3fPool.get(0, 800, 0);
            force = DynamXGeometry.rotateVectorByQuaternion(force, handler.getRotation());
            //force.addLocal(DynamXContext.getPhysicsWorld().getDynamicsWorld().getGravity(Vector3fPool.get()));
            force.addLocal(0, grav - 300, 0);
            handler.applyForce(Vector3fPool.get(), force);
        } else {
            Vector3f force = Vector3fPool.get(0, grav, 0);
            force = DynamXGeometry.rotateVectorByQuaternion(force, handler.getRotation());
            //force.addLocal(DynamXContext.getPhysicsWorld().getDynamicsWorld().getGravity(Vector3fPool.get()));
            handler.applyForce(Vector3fPool.get(), force);
        }
    }

    public void brake(float strength) {
        float grav = -1 * handler.getCollisionObject().getMass() * DynamXContext.getPhysicsWorld().getDynamicsWorld().getGravity(Vector3fPool.get()).y * DynamXContext.getPhysicsSimulationMode(Side.SERVER).getTimeStep() * 2;
        if (strength > 0) {
            handler.activate();
            Vector3f force = Vector3fPool.get(0, -400, 0);
            force = DynamXGeometry.rotateVectorByQuaternion(force, handler.getRotation());
            //force.addLocal(DynamXContext.getPhysicsWorld().getDynamicsWorld().getGravity(Vector3fPool.get()));
            force.addLocal(0, grav + 50, 0);
            handler.applyForce(Vector3fPool.get(), force);
        }
    }

    public void steer(float strength) {
        //propulsionHandler.steer(strength);
        if (strength != 0) {
            Vector3f force = Vector3fPool.get(0, 0, 40000 * strength);
            force = DynamXGeometry.rotateVectorByQuaternion(force, handler.getRotation());
            handler.applyTorque(force);
        }
    }
}
