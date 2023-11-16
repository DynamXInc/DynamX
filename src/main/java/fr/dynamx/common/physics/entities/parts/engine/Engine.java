package fr.dynamx.common.physics.entities.parts.engine;

import com.jme3.math.Vector3f;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.contentpack.type.vehicle.BaseEngineInfo;
import fr.dynamx.common.contentpack.type.vehicle.CarEngineInfo;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.maths.LinearSpline;
import lombok.Getter;
import lombok.Setter;

/**
 * A simple engine with a power, a power graph, revs, and engine braking.
 */
public class Engine {

    /**
     * The total power of the engine. This will be distributed to the propellant(s).
     */
    @Getter
    @Setter
    private float power;

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

    private final LinearSpline powerGraph;

    /**
     * Defines an engine
     *
     * @param engineInfo The engine info, loaded from the {@link ContentPackLoader}
     */
    public Engine(BaseEngineInfo engineInfo) {
        power = engineInfo.getPower();
        maxRevs = engineInfo.getMaxRevs();
        braking = engineInfo.getBraking();

        powerGraph = new LinearSpline(engineInfo.points);
    }

    /**
     * Gets the power output at the current RPM.
     * This is essentially the "power graph" of the engine.
     *
     * @return the power of the engine at the current RPM.
     */
    public float getPowerOutputAtRevs() {
        if (powerGraph == null) {
            return 0;
        }
        float revs = getRevs() * getMaxRevs();
       // System.out.println("Revs: " +revs + " MaxRevs: " + getMaxRevs()+" "+evaluateSpline(powerGraph, getRevs() * getMaxRevs()));
        revs = DynamXMath.clamp(revs, 0, getMaxRevs() - 0.01f);
        float power = evaluateSpline(powerGraph, revs);
        return power * getPower();

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
