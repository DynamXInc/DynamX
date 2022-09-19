package fr.dynamx.common.physics.entities.parts.wheel;

import com.jme3.bullet.objects.PhysicsVehicle;
import com.jme3.bullet.objects.VehicleWheel;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.common.contentpack.type.vehicle.PartWheelInfo;
import fr.dynamx.common.physics.entities.parts.wheel.tyre.PajeckaTireModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class WheelPhysics {
    //TODO DOC
    @Getter
    private final PhysicsVehicle physicsVehicle;
    @Getter
    private final VehicleWheel physicsWheel;
    @Getter
    @Setter
    private byte wheelIndex;
    @Getter
    private final SuspensionPhysics suspension;
    @Getter
    @Setter
    private PajeckaTireModel tireModel;

    //PartWheel depending variables
    @Getter
    private PartWheelInfo wheelInfo;
    @Getter
    @Setter
    private boolean steering;
    @Getter
    @Setter
    private float maxSteerAngle;
    // determines whether this wheel provides acceleration in a 0..1 range.
    // this can be used for settings FWD, RWD, 60/40 distribution, etc...
    @Getter
    @Setter
    private float accelerationForce = 0;
    @Getter
    @Setter
    private boolean handBrakingWheel;

    //PartWheelInfo depending variables
    @Getter
    @Setter
    private float brakeStrength;
    @Getter
    @Setter
    private float handbrakeStrength;

    //Wheel state
    private boolean flattened;
    @Getter
    @Setter
    private float steeringAngle = 0;
    /**
     * simulates degradation. 1.0 = full grip the tyre allows, 0.0 = the tyre is dead.
     */
    @Getter
    @Setter
    private float grip = 1.0f;

    public WheelPhysics(PhysicsVehicle physicsVehicle, VehicleWheel vehicleWheel, byte wheelIndex, PartWheel partWheel) {
        this.physicsVehicle = physicsVehicle;
        this.physicsWheel = vehicleWheel;
        this.wheelIndex = wheelIndex;
        this.suspension = new SuspensionPhysics(vehicleWheel, partWheel);

        this.wheelInfo = partWheel.getDefaultWheelInfo();
        this.steering = partWheel.isWheelIsSteerable();
        this.maxSteerAngle = partWheel.getWheelMaxTurn();
        if (partWheel.isDrivingWheel())
            setAccelerationForce(1);
        this.handBrakingWheel = partWheel.isHandBrakingWheel();

        this.brakeStrength = wheelInfo.getWheelBrakeForce();
        this.handbrakeStrength = wheelInfo.getHandBrakeForce();
        setFriction(partWheel.getDefaultWheelInfo().getWheelFriction());
        setRollInfluence(partWheel.getDefaultWheelInfo().getWheelRollInInfluence());

        this.tireModel = new PajeckaTireModel(
                new TyreSettings(
                        1.54f,
                        18.86f,
                        0.27f,
                        2.0f,
                        0.000058f
                ),
                new TyreSettings(
                        1.52f,
                        30.0f,
                        -1.6f,
                        2.14f,
                        0.000055f
                ),
                new TyreSettings(
                        2.13f,
                        9.96f,
                        -2.0f,
                        2.65f,
                        0.000110f
                ));
    }

    public void setWheelInfo(PartWheelInfo wheelInfo) {
        this.wheelInfo = wheelInfo;
        setFlattened(false);
        getSuspension().setRestLength(wheelInfo.getSuspensionRestLength());
        getSuspension().setStiffness(wheelInfo.getSuspensionStiffness());
        getSuspension().setMaxForce(wheelInfo.getSuspensionMaxForce());
        getSuspension().setDampness(wheelInfo.getWheelsDampingRelaxation());
        getSuspension().setCompression(wheelInfo.getWheelsDampingCompression());
        setFriction(wheelInfo.getWheelFriction());
        setBrakeStrength(wheelInfo.getWheelBrakeForce());
        setHandbrakeStrength(wheelInfo.getHandBrakeForce());
        setRollInfluence(wheelInfo.getWheelRollInInfluence());
    }

    public void accelerate(float strength) {
        physicsVehicle.accelerate(getWheelIndex(), accelerationForce * strength);
    }

    /**
     * Causes the wheel to slow down.
     *
     * @param wheelBrake the strength of the braking force, being multiplied by this wheel's brake force.
     * @param handbrake  the strength of the braking force, being multiplied by this wheel's handbrake force.
     */
    public void brake(float wheelBrake, float handbrake) {
        physicsVehicle.brake(getWheelIndex(), brakeStrength * wheelBrake + handbrakeStrength * handbrake);
    }

    /**
     * Causes the wheel to slow down.
     *
     * @param wheelBrake      the strength of the braking force, being multiplied by this wheel's brake force.
     * @param handbrake       the strength of the braking force, being multiplied by this wheel's handbrake force.
     * @param additionalBrake some additional brake, for example engine braking
     */
    public void brake(float wheelBrake, float handbrake, float additionalBrake) {
        physicsVehicle.brake(getWheelIndex(), brakeStrength * wheelBrake + handbrakeStrength * handbrake + additionalBrake);
    }

    public void steer(float strength) {
        if (isSteering()) {
            steeringAngle = getMaxSteerAngle() * strength;
            this.physicsVehicle.steer(getWheelIndex(), steeringAngle);
        }
    }

    public float getRollInfluence() {
        return physicsWheel.getRollInfluence();
    }

    public void setRollInfluence(float rollInfluence) {
        physicsWheel.setRollInfluence(rollInfluence);
    }

    public float getFriction() {
        return physicsWheel.getFrictionSlip();
    }

    public void setFriction(float friction) {
        this.physicsWheel.setFrictionSlip(friction);
    }

    /**
     * Calculate to what extent the wheel is skidding (for skid sounds/smoke
     * etc.)
     *
     * @return the relative amount of traction (0&rarr;wheel is sliding,
     * 1&rarr;wheel has full traction)
     */
    public float getSkidInfo() {
        return physicsWheel.getSkidInfo();
    }

    public float getDeltaRotation() {
        return physicsWheel.getDeltaRotation();
    }

    public boolean isDrivingWheel() {
        return accelerationForce != 0;
    }

    public boolean isFlattened() {
        return flattened;
    }

    public void setFlattened(boolean flattened) {
        this.flattened = flattened;
        float radius = flattened ? wheelInfo.getRimRadius() : wheelInfo.getWheelRadius();
        if (radius != physicsWheel.getRadius())
            physicsWheel.setRadius(radius);
    }

    /**
     * Creates a Race Car Tire Model Object with the given curve coefficients.
     * Use these example coefficient values for basic tire to start with:<br>
     * C = 1.3<br>
     * B = 15.2<br>
     * E = -1.6<br>
     * KA = 2.0<br>
     * KB = 0.000055
     */
    @AllArgsConstructor
    public static class TyreSettings {

        @Getter
        @Setter
        private float slipAngleCoefficientC; // coefficient C for the normalised slip-angle curve.
        @Getter
        @Setter
        private float slipAngleCoefficientB; // coefficient B for the normalised slip-angle curve.
        @Getter
        @Setter
        private float slipAngleCoefficientE; // coefficient E for the normalised slip-angle curve.
        @Getter
        @Setter
        private float loadCoefficientKA; // coefficient KA for the load function.
        @Getter
        @Setter
        private float loadCoefficientKB; // coefficient KB for the load function.

    }
}
