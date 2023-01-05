package fr.dynamx.common.physics.entities.modules;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.physics.entities.IPropulsionHandler;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.type.vehicle.HelicopterPhysicsInfo;
import fr.dynamx.common.entities.modules.EngineModule;
import fr.dynamx.common.entities.modules.HelicopterEngineModule;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.parts.engine.Engine;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;

/**
 * @see EngineModule
 */
public class HelicopterEnginePhysicsHandler implements IPackInfoReloadListener {
    private HelicopterPhysicsInfo physicsInfo;

    private final HelicopterEngineModule module;
    private final BaseVehiclePhysicsHandler<?> handler;

    public HelicopterEnginePhysicsHandler(HelicopterEngineModule module, BaseVehiclePhysicsHandler<?> handler) {
        this.module = module;
        this.handler = handler;
        onPackInfosReloaded();
    }

    @Override
    public void onPackInfosReloaded() {
        physicsInfo = handler.getPackInfo().getSubPropertyByType(HelicopterPhysicsInfo.class);
    }

    public void update() {
        updateAngles();
        updateMovement();
        if (handler.getHandledEntity().posY >= 1000) {
            handler.getHandledEntity().setDead();
        }
    }

    public static float dx;
    public static float dy;


    //force de penchement de l'hélicoptère
    public void updateAngles() {
        if (module.isEngineStarted() && module.getPower() > 0) {
            if (HelicopterEnginePhysicsHandler.dx != 0) {
                Vector3f force = Vector3fPool.get(0, -physicsInfo.getMouseYawForce() * HelicopterEnginePhysicsHandler.dx, physicsInfo.getMouseRollForce() * HelicopterEnginePhysicsHandler.dx);
                force = DynamXGeometry.rotateVectorByQuaternion(force, handler.getRotation());
                handler.applyTorque(force);
                HelicopterEnginePhysicsHandler.dx = 0;
            }
            if (HelicopterEnginePhysicsHandler.dy != 0) {
                Vector3f force = Vector3fPool.get(-physicsInfo.getMousePitchForce() * HelicopterEnginePhysicsHandler.dy, 0, 0);
                force = DynamXGeometry.rotateVectorByQuaternion(force, handler.getRotation());
                handler.applyTorque(force);
                HelicopterEnginePhysicsHandler.dy = 0;
            }
            if (module.isTurningLeft()) {
                roll(-1);
            } else if (module.isTurningRight()) {
                roll(1);
            }
        }

    }

    public void updateMovement() {
        Vector3f gravity = DynamXContext.getPhysicsWorld(handler.getHandledEntity().world).getDynamicsWorld().getGravity(Vector3fPool.get());
        if(module.isEngineStarted()) {
            handler.setForceActivation(true);

            // Gravity
            // todo write in english :)
            // Calcul de l'inclinaison de l'hélicoptère
            // On prend un plan horizontal et on le rotate
            Vector3f plane = Vector3fPool.get(1, 0, 1);
            plane = DynamXGeometry.rotateVectorByQuaternion(plane, handler.getRotation());
            plane = plane.normalize();
            // on récupère la composante verticale qui est proportionnelle à l'inclinaison de l'hélicoptère
            // et on applique une force de gravité proportionnelle
            // 0 si on est à l'horizontale et que power >= minPower
            // minPower + inclinedGravityFactor si on est à 90° à la verticale (décrochage de l'hélico, il tombe)
            float requiredPower = Math.abs(plane.y) * physicsInfo.getInclinedGravityFactor() + physicsInfo.getMinPower();
            float gravFactor = requiredPower - module.getPower();
            if (gravFactor < 0)
                gravFactor = 0;
            gravFactor /= requiredPower;
            gravity.multLocal(gravFactor);
            handler.getCollisionObject().setGravity(gravity);

            // Acceleration
            Vector3f force = Vector3fPool.get(0, physicsInfo.getThrustForce(), 0);
            force = DynamXGeometry.rotateVectorByQuaternion(force, handler.getRotation());
            force.addLocal(0, -physicsInfo.getVerticalThrustCompensation(), 0);
            force.multLocal(module.getPower(), (module.getPower() >= 0.01 && module.isAccelerating()) ? module.getPower() : 0, module.getPower());
            handler.applyForce(Vector3fPool.get(), force);

            //Brake
            if (module.isReversing()) {
                handler.activate();
                force = Vector3fPool.get(0, -physicsInfo.getBrakeForce(), 0);
                force = DynamXGeometry.rotateVectorByQuaternion(force, handler.getRotation());
                handler.applyForce(Vector3fPool.get(), force);
            }
        } else {
            handler.getCollisionObject().setGravity(gravity);
        }
    }

    public void roll(float strength) {
        if (strength != 0) {
            Vector3f force = Vector3fPool.get(0, 0, physicsInfo.getRollForce() * strength);
            force = DynamXGeometry.rotateVectorByQuaternion(force, handler.getRotation());
            handler.applyTorque(force);
        }
    }
}
