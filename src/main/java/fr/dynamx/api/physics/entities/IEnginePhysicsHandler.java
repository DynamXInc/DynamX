package fr.dynamx.api.physics.entities;

import fr.dynamx.common.physics.entities.parts.engine.Engine;

/**
 * Physics handler of {@link fr.dynamx.api.entities.modules.IEngineModule} <br>
 * Controls the behavior of an engine, and for example its gearbox <br>
 * Also "gives orders" to the propulsion
 */
public interface IEnginePhysicsHandler {
    /**
     * @return The controlled physical engine
     */
    Engine getEngine();

    /**
     * @return True if the engine is connected to the propulsion (if a gear is engaged, for example)
     */
    boolean isEngaged();

    void syncActiveGear(int activeGearNum);
}
