package fr.dynamx.common.physics.entities.parts.wheel;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.common.physics.entities.modules.WheelsPhysicsHandler;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;

import static fr.dynamx.utils.maths.DynamXMath.clamp;

public class PacejkaMagicFormula {
    private final WheelsPhysicsHandler wheelsPhysics;
    public final float[] lateral = new float[10];
    public final float[] longitudinal = new float[10];

    public PacejkaMagicFormula(WheelsPhysicsHandler wheelsPhysics) {
        this.wheelsPhysics = wheelsPhysics;
    }

    public void update() {
        for (int i = 0; i < wheelsPhysics.getNumWheels(); i++) {
            WheelPhysicsHandler wheelPhysicsHandler = wheelsPhysics.getWheel(i);

            // the angle between the dir of the wheel and the dir the vehicle is travelling.
            float lateralSlip = calculateLateralSlipAngle(wheelPhysicsHandler);
            float load = 10000;
            wheelPhysicsHandler.getTireModel().setLoad(load);

            // returns the amount of force in N on the tyre.
            // this model allows max 10,000 (this is determined by the tyre).
            lateral[i] = wheelPhysicsHandler.getTireModel().calcLateralTireForce(lateralSlip);

            // the slip angle for this is how much force is being applied to the tyre (acceleration force).
            float longSlip = calculateLongitudinalSlipAngle(wheelPhysicsHandler);
            longitudinal[i] = wheelPhysicsHandler.getTireModel().calcLongtitudeTireForce(longSlip);

            float friction = 1.0f - ((lateral[i] / 10000) - (longitudinal[i] / 10000));
            friction *= 2.0;
            friction *= wheelPhysicsHandler.getGrip();
            wheelPhysicsHandler.setFriction(friction);
        }
    }

    // Pacejka
    // LATERAL
    // the slip angle is the angle between the direction in which a wheel is pointing
    // and the direction in which the vehicle is traveling.
    protected float calculateLateralSlipAngle(WheelPhysicsHandler wheelPhysics) {
        Quaternion wheelRot = wheelPhysics.getPhysicsVehicle().getPhysicsRotation(QuaternionPool.get()).mult(
                QuaternionPool.get().fromAngleNormalAxis(wheelPhysics.getSteeringAngle(), Vector3fPool.get(0, 1, 0)), QuaternionPool.get());

        Vector3f wheelDir = DynamXGeometry.getRotationColumn(wheelRot, 2, Vector3fPool.get());

        Vector3f vehicleTravel;

        if (wheelPhysics.getPhysicsVehicle().getCurrentVehicleSpeedKmHour() < 5) {
            vehicleTravel = DynamXGeometry.getRotationColumn(wheelPhysics.getPhysicsVehicle().getPhysicsRotation(null), 2);
        } else {
            vehicleTravel = wheelPhysics.getPhysicsVehicle().getLinearVelocity(Vector3fPool.get());
            DynamXGeometry.normalizeVector(vehicleTravel);
            vehicleTravel.y = 0;
        }

        float minAngle = 0.1f;

        float angle = minAngle + DynamXGeometry.angle(wheelDir, vehicleTravel);

        angle = clamp(angle, 0, (float) (0.25f * Math.PI));

        return angle;

    }

    // the slip angle for this is how much force is being applied to the tyre (acceleration force).
    // how much rotation has been applied as a result of acceleration.
    protected float calculateLongitudinalSlipAngle(WheelPhysicsHandler wheelPhysics) {
        // the rotation of the wheel as if it were just following a moving vehicle.
        // that is to say a wheel that is rolling without slip.
        float normalRot = wheelPhysics.getPhysicsWheel().getDeltaRotation();// * 0.5f;

        // the rotation applied via wheelspin
        float wheelSpinRot = wheelPhysics.getDeltaRotation();// * 1.5f;

        // combined rotation of normal roll + wheelspin
        float rot = wheelSpinRot + normalRot;

        float vel = wheelPhysics.getPhysicsVehicle().getLinearVelocity(Vector3fPool.get()).length();

        float angle = rot / vel;
        angle *= 10;

        angle = clamp(angle, 0, (float) (Math.PI * 2));
        return angle;
    }
}
