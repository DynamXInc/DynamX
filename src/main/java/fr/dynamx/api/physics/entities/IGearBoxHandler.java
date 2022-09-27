package fr.dynamx.api.physics.entities;

/**
 * Handles {@link fr.dynamx.common.physics.entities.parts.engine.GearBox} behavior (example : {@link fr.dynamx.common.physics.entities.parts.engine.AutomaticGearboxHandler})
 */
public interface IGearBoxHandler {
    /**
     * Called to update passed gear
     *
     * @param currentAcceleration The acceleration, between -1 and 1f
     */
    void update(float currentAcceleration);
}
