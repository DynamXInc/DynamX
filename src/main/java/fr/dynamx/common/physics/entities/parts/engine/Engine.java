package fr.dynamx.common.physics.entities.parts.engine;

import com.jme3.math.Vector3f;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.contentpack.type.vehicle.EngineInfo;
import fr.dynamx.common.physics.entities.PackEntityPhysicsHandler;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.maths.LinearSpline;

public class Engine {
    private final String name;
    /**
     * The total power of the engine. This will be distributed to the propellant(s).
     */
    private float power;

    /**
     * Revolutions in a 0 - 1 range.
     */
    private float revs;
    /**
     * Max revs - e.g. 7000 - used as a VISUAL multiplier.
     */
    private float maxRevs;

    /**
     * the amount of engine braking when coasting.
     * this can be manipulated to simulate damage.
     */
    private float braking;

    private boolean started;

    private LinearSpline powerGraph;

    /**
     * Defines an engine
     *
     * @param engineInfo The engine info, loaded from the {@link ContentPackLoader}
     */
    public Engine(PackEntityPhysicsHandler<ModularVehicleInfo<?>, ?> handler, EngineInfo engineInfo) {
        this.name = engineInfo.getName();
        this.power = engineInfo.getPower();
        this.maxRevs = engineInfo.getMaxRevs();
        this.braking = engineInfo.getBraking();

        initSpline(engineInfo);
    }

    public String getName() {
        return this.name;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public float getPower() {
        return power;
    }

    public void setPower(float power) {
        this.power = power;
    }

    public float getRevs() {
        return revs;
    }

    public void setRevs(float revs) {
        this.revs = revs;
    }

    public float getMaxRevs() {
        return maxRevs;
    }

    public void setMaxRevs(float maxRevs) {
        this.maxRevs = maxRevs;
    }

    /**
     * Gets the power output at the current RPM.
     * This is essentially the "power graph" of the engine.
     *
     * @return the power of the engine at the current RPM.
     */
    public float getPowerOutputAtRevs() {
        if (powerGraph != null) {
            float revs = getRevs() * getMaxRevs();
            revs = DynamXMath.clamp(revs, 0, getMaxRevs() - 0.01f);
            float power = evaluateSpline(powerGraph, revs);
            return power * getPower();
        } else {
            return 0;
        }
    }


    /**
     * Get the amount of torque the engine produces at speed.
     * This is a kind of speed limiter that slows down acceleration as the vehicle increases in speed.
     *
     * @return the amount of torque applied at the current speed.
     */
    /*public float getTorqueAtSpeed(EngineModule vehicle) {
        // the maximum this vehicle can go is 135mph or 216kmh.

        // float engineMaxSpeed = 192.0f;
        float engineMaxSpeed = vehicle.getPhysicsHandler().getGearBox() == null ? 192 : vehicle.getPhysicsHandler().getGearBox().getMaxSpeed(ModulableVehiclePhysicsHandler.SpeedUnit.KMH);
        float speedUnit = vehicle.getSpeed(ModulableVehiclePhysicsHandler.SpeedUnit.KMH) / engineMaxSpeed;
        return 1.0f - DynamXMath.interpolateLinear(speedUnit, 0, speedUnit);
    }*/
    public float getBraking() {
        return braking;
    }

    public void setBraking(float braking) {
        this.braking = braking;
    }

    private void initSpline(EngineInfo engine) {
        powerGraph = new LinearSpline(engine.points);
    }

    /**
     * Evaluate the power graph
     *
     * @param range a value from 0-maxRevs
     * @return the power at this rev-range, from 0 to getPower().
     */
    public float evaluateSpline(LinearSpline powerGraph, float range) {

        int index = powerGraph.getControlPoints().size() - 1;

        Vector3f point = powerGraph.getControlPoints().get(index);

        while (point.x >= range && index > 0) {
            index -= 1;
            point = powerGraph.getControlPoints().get(index);
        }

        //System.out.println("index: " + index + " - range: " + range);

        float start = point.x;
        float end = powerGraph.getControlPoints().get(index + 1).x;

        float interp = map(range, start, end, 0, 1);

        return powerGraph.interpolate(interp, index, null).y;
    }

    private float map(float value, float oldMin, float oldMax, float newMin, float newMax) {
        return (((value - oldMin) * (newMax - newMin)) / (oldMax - oldMin)) + newMin;
    }
}
