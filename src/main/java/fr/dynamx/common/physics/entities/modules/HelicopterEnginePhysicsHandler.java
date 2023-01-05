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

    public static float AngleBack;
    public static float AngleFront;


    //force de penchement de l'hélicoptère
    public void updateTurn0() {
        float dx = AngleBack;
        float dy = AngleFront;
        float pitchSpeed = handler.getAngularVelocity().x;
        float rollSpeed = handler.getAngularVelocity().z;


        if(engine.getPower()>0&&engine.isStarted()) {
            if (AngleBack != 0) {
                Vector3f force = Vector3fPool.get(0, -2800 * AngleBack, 0);
                force = DynamXGeometry.rotateVectorByQuaternion(force, handler.getRotation());
                handler.applyTorque(force);
                AngleBack = 0;
            }

            if (AngleFront != 0) {
                Vector3f force = Vector3fPool.get(-1000 * AngleFront, 0, 0);
                force = DynamXGeometry.rotateVectorByQuaternion(force, handler.getRotation());
                handler.applyTorque(force);
                AngleFront = 0;
            }
            if (module.isTurningLeft()) {
                steer(-1);
            } else if (module.isTurningRight()) {
                steer(1);
            }
        }

    }

    public void updateMovement() {

        if (module.getPower()>=0.01 && module.isAccelerating()) {
            accelerate(module.getPower());
        } else if (module.isReversing()) {
            brake(1f);
            accelerate(0);
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
        //float grav = -0.98F*handler.getCollisionObject().getMass() * handler.getCollisionObject().getGravity(Vector3fPool.get()).y * DynamXContext.getPhysicsSimulationMode(Side.SERVER).getTimeStep() * 2;

        Vector3f plane = Vector3fPool.get(1, 0, 1);
        plane = DynamXGeometry.rotateVectorByQuaternion(plane, handler.getRotation());
        plane = plane.normalize();
        float requiredPower = Math.abs(plane.y)*2 + 0.4f;

        Vector3f gravity = DynamXContext.getPhysicsWorld().getDynamicsWorld().getGravity(Vector3fPool.get());
        float gravFactor = requiredPower - module.getPower();
        if(gravFactor < 0) {
            gravFactor = 0;
        }
        gravFactor /= requiredPower;
        gravity.multLocal(gravFactor);
        handler.getCollisionObject().setGravity(gravity);

        if (true){//strength > 0||AngleFront!=0) {
            Vector3f force = Vector3fPool.get(0, 3000, 0);
            force = DynamXGeometry.rotateVectorByQuaternion(force, handler.getRotation());
            /*if(force.y >= 2100 && strength == 0 && handler.getHandledEntity().getControllingPassenger() == null) {
                handler.getCollisionObject().setGravity(DynamXContext.getPhysicsWorld().getDynamicsWorld().getGravity(Vector3fPool.get()));
                return;
            }*/
            //accela
            //force.addLocal(DynamXContext.getPhysicsWorld().getDynamicsWorld().getGravity(Vector3fPool.get()));
            //poids monter descendre
            force.addLocal(0, -2500, 0);
            force.multLocal(module.getPower(), strength, module.getPower());
            handler.applyForce(Vector3fPool.get(), force);
        } /*else {
            Vector3f force = Vector3fPool.get(0, grav, 0);
            force = DynamXGeometry.rotateVectorByQuaternion(force, handler.getRotation());
            //force.addLocal(DynamXContext.getPhysicsWorld().getDynamicsWorld().getGravity(Vector3fPool.get()));
            handler.applyForce(Vector3fPool.get(), force);
        }*/
    }

    public void brake(float strength) {
        //float grav = -1 * handler.getCollisionObject().getMass() * DynamXContext.getPhysicsWorld().getDynamicsWorld().getGravity(Vector3fPool.get()).y * DynamXContext.getPhysicsSimulationMode(Side.SERVER).getTimeStep() * 2;
        if (strength > 0) {
            handler.activate();
            Vector3f force = Vector3fPool.get(0, -200, 0);
            force = DynamXGeometry.rotateVectorByQuaternion(force, handler.getRotation());
            //force.addLocal(DynamXContext.getPhysicsWorld().getDynamicsWorld().getGravity(Vector3fPool.get()));
            //force.addLocal(0, grav + 50, 0);
            handler.applyForce(Vector3fPool.get(), force);
        }
    }

    // rotation
    public void steer(float strength) {
        //propulsionHandler.steer(strength);
        //* la vitesse de l'hélicoptère/la vitesse max de l'hélicoptère
        float speed = handler.getLinearVelocity().length() / module.getEngineInfo().getMaxRevs();
        if (strength != 0) {
            Vector3f force = Vector3fPool.get(0, 0, 4000*strength );
            force = DynamXGeometry.rotateVectorByQuaternion(force, handler.getRotation());
            handler.applyTorque(force);
        }
    }
}
