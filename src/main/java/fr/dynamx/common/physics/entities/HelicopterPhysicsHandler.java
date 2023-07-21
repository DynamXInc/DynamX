package fr.dynamx.common.physics.entities;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.type.vehicle.HelicopterPhysicsInfo;
import fr.dynamx.common.entities.modules.HelicopterEngineModule;
import fr.dynamx.common.entities.vehicles.HelicopterEntity;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.Vector3fPool;

public class HelicopterPhysicsHandler<A extends HelicopterEntity<?>> extends BaseVehiclePhysicsHandler<A> implements IPackInfoReloadListener {
    private HelicopterPhysicsInfo physicsInfo;
    private final HelicopterEngineModule module;

    public HelicopterPhysicsHandler(A entity) {
        super(entity);
        module = entity.getModuleByType(HelicopterEngineModule.class);
    }

    @Override
    public void onPackInfosReloaded() {
        super.onPackInfosReloaded();
        physicsInfo = getPackInfo().getSubPropertyByType(HelicopterPhysicsInfo.class);
    }

    @Override
    public void update() {
        super.update();
        updateAngles();
        updateMovement();
        if (getHandledEntity().posY >= 1000) {
            getHandledEntity().setDead();
        }
    }

    //force de penchement de l'hélicoptère
    public void updateAngles() {
        if (module.isEngineStarted() && module.getPower() > 0) {
            float dx = module.getRollControls().get(0);
            if (dx != 0) {
                Vector3f force = Vector3fPool.get(0, -physicsInfo.getMouseYawForce() * dx, physicsInfo.getMouseRollForce() * dx);
                force = DynamXGeometry.rotateVectorByQuaternion(force, getRotation());
                applyTorque(force);
                module.getRollControls().set(0, 0);
            }
            float dy = module.getRollControls().get(1);
            if (dy != 0) {
                Vector3f force = Vector3fPool.get(-physicsInfo.getMousePitchForce() * dy, 0, 0);
                force = DynamXGeometry.rotateVectorByQuaternion(force, getRotation());
                applyTorque(force);
                module.getRollControls().set(1, 0);
            }
            if (module.isTurningLeft()) {
                roll(-1);
            } else if (module.isTurningRight()) {
                roll(1);
            } else if (module.isHandBraking()) {
                Quaternion targetRotation = DynamXGeometry.rotationYawToQuaternion(getHandledEntity().rotationYaw);
                setPhysicsRotation(DynamXMath.slerp(0.05f, getRotation(), targetRotation, getRotation()));
            }
        }

    }

    public void updateMovement() {
        Vector3f gravity = DynamXContext.getPhysicsWorld(getHandledEntity().world).getDynamicsWorld().getGravity(Vector3fPool.get());
        if (module.isEngineStarted()) {
            setForceActivation(true);

            // Gravity
            // Calculation of the inclination of the helicopter
            // We take a horizontal plane and rotate it
            Vector3f plane = Vector3fPool.get(1, 0, 1);
            plane = DynamXGeometry.rotateVectorByQuaternion(plane, getRotation());
            plane = plane.normalize();
            // we get the vertical component which is proportional to the inclination of the helicopter
            // and we apply a gravity force proportional
            // 0 if we are horizontal and power >= minPower
            // minPower + inclinedGravityFactor if we are at 90 ° to the vertical (helicopter stall, it falls)
            float requiredPower = Math.abs(plane.y) * physicsInfo.getInclinedGravityFactor() + physicsInfo.getMinPower();
            float gravFactor = requiredPower - module.getPower();
            if (gravFactor < 0)
                gravFactor = 0;
            gravFactor /= requiredPower;
            gravity.multLocal(gravFactor);
            getCollisionObject().setGravity(gravity);

            // Acceleration
            Vector3f force = Vector3fPool.get(0, physicsInfo.getThrustForce(), 0);
            force = DynamXGeometry.rotateVectorByQuaternion(force, getRotation());
            force.addLocal(0, -physicsInfo.getVerticalThrustCompensation(), 0);
            force.multLocal(module.getPower(), (module.getPower() >= 0.01 && module.isAccelerating()) ? module.getPower() : 0, module.getPower());
            applyForce(Vector3fPool.get(), force);

            //Brake
            if (module.isReversing()) {
                activate();
                force = Vector3fPool.get(0, -physicsInfo.getBrakeForce(), 0);
                force = DynamXGeometry.rotateVectorByQuaternion(force, getRotation());
                applyForce(Vector3fPool.get(), force);
            }
        } else {
            getCollisionObject().setGravity(gravity);
        }
    }

    protected void roll(float strength) {
        if (strength != 0) {
            Vector3f force = Vector3fPool.get(0, 0, physicsInfo.getRollForce() * strength);
            force = DynamXGeometry.rotateVectorByQuaternion(force, getRotation());
            applyTorque(force);
        }
    }
}
