package fr.dynamx.common.physics.entities.parts.engine;

import com.jme3.math.Vector3f;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.contentpack.type.vehicle.BaseEngineInfo;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.maths.LinearSpline;
import lombok.Getter;
import lombok.Setter;

/**
 * A simple engine with a power, a power graph, revs, and engine braking.
 */
public class Engine {
    /**
     * The engine config type, using either a torque curve or a power curve
     */
    @Setter
    @Getter
    private BaseEngineInfo.EngineConfigType configType;
    /**
     * The maximum capacity of the engine. The nature of it depends on the configType. <br>
     * <ul>
     *     <li>For EngineConfigType.POWER, it's the maximum power of the engine.</li>
     *     <li>For EngineConfigType.TORQUE, it's the maximum torque of the engine.</li>
     * </ul>
     * T
     */
    @Getter
    @Setter
    private float maxCapacity;

    /**
     * Revolutions in a 0 - 1 range.
     */
    @Getter
    @Setter
    private float revs;
    /**
     * Max revs - e.g. 7000 - used as a VISUAL multiplier.
     */
    @Getter
    @Setter
    private float maxRevs;

    /**
     * the amount of engine braking when coasting.
     * this can be manipulated to simulate damage.
     */
    @Getter
    @Setter
    private float braking;

    @Getter
    @Setter
    private boolean started;

    /**
     * Can be either the torque graph, or the power graph of the engine, depending on the configType.
     */
    private final LinearSpline engineGraph;

    /**
     * Defines an engine
     *
     * @param engineInfo The engine info, loaded from the {@link ContentPackLoader}
     */
    public Engine(BaseEngineInfo engineInfo) {
        configType = engineInfo.getConfigType();
        maxCapacity = configType == BaseEngineInfo.EngineConfigType.POWER ? engineInfo.getMaxPower() : engineInfo.getMaxTorque();
        maxRevs = engineInfo.getMaxRevs();
        braking = engineInfo.getBraking();
        engineGraph = new LinearSpline(engineInfo.points);
    }

    /**
     * Gets the torque output at the current RPM.
     * This configType EngineConfigType.POWER, this is essentially the "power graph" of the engine.
     *
     * @return the torque of the engine at the current RPM.
     */
    public float getTorqueOutput(GearBox.GearData currentGear, float rpm) {
        if (engineGraph == null) {
            return 0;
        }
        float revs = rpm * getMaxRevs();
        revs = DynamXMath.clamp(revs, 0, getMaxRevs() - 0.01f);
        if (configType == BaseEngineInfo.EngineConfigType.POWER) {
            float power = evaluateSpline(engineGraph, revs);
            return power * getMaxCapacity() * 2;
        } else { // TORQUE
            float power = evaluateSpline(engineGraph, revs);
            return power * getMaxCapacity() * currentGear.getGearRatio();
        }
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

        float start = point.x;
        float end = powerGraph.getControlPoints().get(index + 1).x;

        float interp = map(range, start, end, 0, 1);

        return powerGraph.interpolate(interp, index, null).y;
    }

    private float map(float value, float oldMin, float oldMax, float newMin, float newMax) {
        return (((value - oldMin) * (newMax - newMin)) / (oldMax - oldMin)) + newMin;
    }
}
