package fr.dynamx.common.physics.entities.parts.wheel;

import com.jme3.bullet.objects.PhysicsVehicle;
import com.jme3.bullet.objects.VehicleWheel;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.common.contentpack.type.PartWheelInfo;
import fr.dynamx.common.physics.entities.parts.wheel.tyre.PajeckaTireModel;
import fr.dynamx.common.physics.entities.parts.wheel.tyre.TyreSettings;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.Vector3fPool;

import static fr.dynamx.utils.maths.DynamXMath.*;

public class WheelPhysicsHandler
{
    private final PhysicsVehicle raycastVehicle;
    public byte wheelIndex;
    private final VehicleWheel physicsWheel;

    private boolean steering;
    // determines whether this wheel provides acceleration in a 0..1 range.
    // this can be used for settings FWD, RWD, 60/40 distribution, etc...
    private float accelerationForce = 0;
    private float maxSteerAngle;

    private final SuspensionPhysicsHandler suspensionPhysicsHandler;
    private float brakeStrength;
    private float handbrakeStrength;

    // simulates degradation. 1.0 = full grip the tyre allows, 0.0 = the tyre is dead.
    private float grip = 1.0f;
    private boolean drivingWheel;
    private boolean handBrakingWheel;
    private boolean flattened;

    private PajeckaTireModel tireModel;

    public WheelPhysicsHandler(PartWheel partWheel, PhysicsVehicle raycastVehicle, byte wheelIndex, boolean isSteering, SuspensionPhysicsHandler suspensionPhysicsHandler, float brakeStrength, float handbrakeStrength)
    {
        this.raycastVehicle = raycastVehicle;

        this.wheelIndex = wheelIndex;
        this.physicsWheel = raycastVehicle.getWheel(wheelIndex);

        this.steering = isSteering;
        //this.steeringFlipped = steeringFlipped;

        this.suspensionPhysicsHandler = suspensionPhysicsHandler;
        this.brakeStrength = brakeStrength;
        this.handbrakeStrength = handbrakeStrength;

        setMaxSteerAngle(partWheel.getWheelMaxTurn());
        setFriction(partWheel.getDefaultWheelInfo().getWheelFriction());
        setRollInfluence(partWheel.getDefaultWheelInfo().getWheelRollInInfluence());
        setTireModel(new PajeckaTireModel("Default Tyre",
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
                10000));
    }

    public void setWheelInfo(PartWheelInfo wheelInfo) {
        physicsWheel.setRadius(wheelInfo.getWheelRadius());

        setFriction(wheelInfo.getWheelFriction());
        brakeStrength = wheelInfo.getWheelBrakeForce();
        handbrakeStrength = wheelInfo.getHandBrakeForce();
        setRollInfluence(wheelInfo.getWheelRollInInfluence());
        getSuspension().setRestLength(wheelInfo.getSuspensionRestLength());
        getSuspension().setStiffness(wheelInfo.getSuspensionStiffness());
        getSuspension().setMaxForce(wheelInfo.getSuspensionMaxForce());
        getSuspension().setDampness(wheelInfo.getWheelsDampingRelaxation());
        getSuspension().setCompression(wheelInfo.getWheelsDampingCompression());

        setFlattened(false);
    }

    public PajeckaTireModel getTireModel() {
        return tireModel;
    }

    public void setTireModel(PajeckaTireModel tireModel) {
        this.tireModel = tireModel;
    }

    public float getGrip() {
        return grip;
    }

    public void setGrip(float grip) {
        this.grip = grip;
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

    public boolean isSteering() {
        return steering;
    }

    public void setSteering(boolean steering) {
        this.steering = steering;
    }

    public float getAccelerationForce() { return accelerationForce; }

    /**
     * The amount of force applied to this wheel when the vehicle is accelerating
     * Value is in the 0 to 1 range. 0 = no force, 1 = full force.
     * This acts as a multiplier for the engine power to this wheel.
     * @param accelerationForce the amount of force to apply to this wheel when accelerating.
     */
    public void setAccelerationForce(float accelerationForce) { this.accelerationForce = accelerationForce; }

    // public float getBrakeForce() { return
    // eForce; }
    // public void setBrakeForce(float brakeForce) { this.brakeForce = brakeForce; }

    public void accelerate(float strength) {
        raycastVehicle.accelerate(wheelIndex,accelerationForce * strength);
    }

    /**
     * Causes the wheel to slow down.
     * @param wheelBrake the strength of the braking force, being multiplied by this wheel's brake force.
     * @param handbrake the strength of the braking force, being multiplied by this wheel's handbrake force.
     */
    public void brake(float wheelBrake, float handbrake) {
        raycastVehicle.brake(wheelIndex, brakeStrength * wheelBrake + handbrakeStrength * handbrake);
    }

    /**
     * Causes the wheel to slow down.
     * @param wheelBrake the strength of the braking force, being multiplied by this wheel's brake force.
     * @param handbrake the strength of the braking force, being multiplied by this wheel's handbrake force.
     * @param additionalBrake some additional brake, for example engine braking
     */
    public void brake(float wheelBrake, float handbrake, float additionalBrake) {
        raycastVehicle.brake(wheelIndex, brakeStrength * wheelBrake + handbrakeStrength * handbrake + additionalBrake);
    }

    public float getBrakeStrength() {
        return brakeStrength;
    }

    public float getHandbrakeStrength() {
        return handbrakeStrength;
    }

    private float steeringAngle = 0;

    public void steer(float strength) {
        if (isSteering()) {
            steeringAngle = getMaxSteerAngle() * strength;
            this.raycastVehicle.steer(wheelIndex,steeringAngle);
        }
    }

    public float getSteeringAngle() {
        return this.steeringAngle;
    }

    public float getMaxSteerAngle() {
        return maxSteerAngle;// * speed;
    }

    public void setMaxSteerAngle(float maxSteerAngle) {
        this.maxSteerAngle = maxSteerAngle;
    }

    public SuspensionPhysicsHandler getSuspension() { return suspensionPhysicsHandler; }

    //TODO DOC

    public VehicleWheel getPhysicsWheel() {
        return this.physicsWheel;
    }

    /**
     * Calculate to what extent the wheel is skidding (for skid sounds/smoke
     * etc.)
     *
     * @return the relative amount of traction (0&rarr;wheel is sliding,
     * 1&rarr;wheel has full traction)
     */
    public float getSkidInfo(){
        return physicsWheel.getSkidInfo();
    }

    public void setDrivingWheel(){
        drivingWheel = true;
    }

    public boolean isDrivingWheel(){
        return drivingWheel;
    }

    public void setHandBrakingWheel(boolean handBrakingWheel) {
        this.handBrakingWheel = handBrakingWheel;
    }

    public boolean isHandBrakingWheel() {
        return handBrakingWheel;
    }

    public boolean isFlattened() {
        return flattened;
    }

    public void setFlattened(boolean flattened) {
        this.flattened = flattened;
    }

    //Optimization
    private Quaternion store1 = new Quaternion(), store2 = new Quaternion(), store3 = new Quaternion();

    // Pacejka
    // LATERAL
    // the slip angle is the angle between the direction in which a wheel is pointing
    // and the direction in which the vehicle is traveling.
    public float calculateLateralSlipAngle() {
        Quaternion wheelRot = raycastVehicle.getPhysicsRotation(store1).mult(
                store3.fromAngleNormalAxis(getSteeringAngle(), Vector3fPool.get(0,1,0)), store2);

        Vector3f wheelDir = DynamXGeometry.getRotationColumn(wheelRot,2, Vector3fPool.get());

        Vector3f vehicleTravel;

        if (raycastVehicle.getCurrentVehicleSpeedKmHour() < 5) {
            vehicleTravel = DynamXGeometry.getRotationColumn(raycastVehicle.getPhysicsRotation(null),2);
        }
        else {
            vehicleTravel = raycastVehicle.getLinearVelocity(Vector3fPool.get());
            DynamXGeometry.normalizeVector(vehicleTravel);
            vehicleTravel.y = 0;
        }

        float minAngle = 0.1f;

        float angle = minAngle + DynamXGeometry.angle(wheelDir,vehicleTravel);

        angle = clamp(angle, 0, (float) (0.25f * Math.PI));

        return angle;

    }

    // the slip angle for this is how much force is being applied to the tyre (acceleration force).
    // how much rotation has been applied as a result of acceleration.
    public float calculateLongitudinalSlipAngle() {

        // the rotation of the wheel as if it were just following a moving vehicle.
        // that is to say a wheel that is rolling without slip.
        float normalRot = physicsWheel.getDeltaRotation();// * 0.5f;

        // the rotation applied via wheelspin
        float wheelSpinRot = getRotationDelta();// * 1.5f;

        // combined rotation of normal roll + wheelspin
        float rot = wheelSpinRot + normalRot;

        float vel = raycastVehicle.getLinearVelocity(Vector3fPool.get()).length();

        float angle = rot / vel;
        angle *= 10;

        angle = clamp(angle, 0, (float) (Math.PI * 2));
        return angle;
    }

    public float getRotationDelta() {
        return physicsWheel.getDeltaRotation();
    }
}
