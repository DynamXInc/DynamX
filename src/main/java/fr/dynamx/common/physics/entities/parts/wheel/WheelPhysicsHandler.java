package fr.dynamx.common.physics.entities.parts.wheel;

import com.jme3.bullet.objects.PhysicsVehicle;
import com.jme3.bullet.objects.VehicleWheel;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.common.contentpack.type.PartWheelInfo;
import fr.dynamx.common.physics.entities.parts.wheel.tyre.PajeckaTireModel;
import fr.dynamx.common.physics.entities.parts.wheel.tyre.TyreSettings;

public class WheelPhysicsHandler {
    //TODO DOC
    private final PhysicsVehicle physicsVehicle;
    private final VehicleWheel physicsWheel;
    private byte wheelIndex;
    private final SuspensionPhysicsHandler suspension;
    private PajeckaTireModel tireModel;

    //PartWheel depending variables
    private PartWheelInfo wheelInfo;
    private boolean steering;
    private float maxSteerAngle;
    // determines whether this wheel provides acceleration in a 0..1 range.
    // this can be used for settings FWD, RWD, 60/40 distribution, etc...
    private float accelerationForce = 0;
    private boolean handBrakingWheel;

    //PartWheelInfo depending variables
    private float brakeStrength;
    private float handbrakeStrength;

    //Wheel state
    private boolean flattened;
    private float steeringAngle = 0;
    /** simulates degradation. 1.0 = full grip the tyre allows, 0.0 = the tyre is dead. */
    private float grip = 1.0f;

    public WheelPhysicsHandler(PhysicsVehicle physicsVehicle, VehicleWheel vehicleWheel, byte wheelIndex, PartWheel partWheel) {
        this.physicsVehicle = physicsVehicle;
        this.physicsWheel = vehicleWheel;
        this.wheelIndex = wheelIndex;
        this.suspension = new SuspensionPhysicsHandler(vehicleWheel, partWheel);

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

        this.tireModel = new PajeckaTireModel("Default Tyre",
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
                ),
                10000);
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

    public void setRollInfluence(float rollInfluence){
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
        if(radius != physicsWheel.getRadius())
            physicsWheel.setRadius(radius);
    }

    public PhysicsVehicle getPhysicsVehicle() {
        return physicsVehicle;
    }

    public VehicleWheel getPhysicsWheel() {
        return physicsWheel;
    }

    public byte getWheelIndex() {
        return wheelIndex;
    }

    public void setWheelIndex(byte wheelIndex) {
        this.wheelIndex = wheelIndex;
    }

    public SuspensionPhysicsHandler getSuspension() {
        return suspension;
    }

    public PajeckaTireModel getTireModel() {
        return tireModel;
    }

    public void setTireModel(PajeckaTireModel tireModel) {
        this.tireModel = tireModel;
    }

    public PartWheelInfo getWheelInfo() {
        return wheelInfo;
    }

    public boolean isSteering() {
        return steering;
    }

    public void setSteering(boolean steering) {
        this.steering = steering;
    }

    public float getMaxSteerAngle() {
        return maxSteerAngle;
    }

    public void setMaxSteerAngle(float maxSteerAngle) {
        this.maxSteerAngle = maxSteerAngle;
    }

    public float getAccelerationForce() {
        return accelerationForce;
    }

    /**
     * The amount of force applied to this wheel when the vehicle is accelerating
     * Value is in the 0 to 1 range. 0 = no force, 1 = full force.
     * This acts as a multiplier for the engine power to this wheel.
     *
     * @param accelerationForce the amount of force to apply to this wheel when accelerating.
     */
    public void setAccelerationForce(float accelerationForce) {
        this.accelerationForce = accelerationForce;
    }

    public boolean isHandBrakingWheel() {
        return handBrakingWheel;
    }

    public void setHandBrakingWheel(boolean handBrakingWheel) {
        this.handBrakingWheel = handBrakingWheel;
    }

    public float getBrakeStrength() {
        return brakeStrength;
    }

    public void setBrakeStrength(float brakeStrength) {
        this.brakeStrength = brakeStrength;
    }

    public float getHandbrakeStrength() {
        return handbrakeStrength;
    }

    public void setHandbrakeStrength(float handbrakeStrength) {
        this.handbrakeStrength = handbrakeStrength;
    }

    public float getSteeringAngle() {
        return steeringAngle;
    }

    public void setSteeringAngle(float steeringAngle) {
        this.steeringAngle = steeringAngle;
    }

    public float getGrip() {
        return grip;
    }

    public void setGrip(float grip) {
        this.grip = grip;
    }
}
