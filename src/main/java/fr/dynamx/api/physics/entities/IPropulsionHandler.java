package fr.dynamx.api.physics.entities;

import fr.dynamx.api.entities.modules.IEngineModule;
import fr.dynamx.common.contentpack.parts.PartRotor;

/**
 * Physics handler of {@link fr.dynamx.api.entities.modules.IPropulsionModule} <br>
 * Controls the behavior of physics wheels for example
 */
public interface IPropulsionHandler {
    /**
     * Called by the engine to accelerate
     *
     * @param module     The engine
     * @param strength   The strength, between -1 and 1f
     * @param speedLimit The speed limit, in km/h (especially for cars)
     */
    void accelerate(IEngineModule module, float strength, float speedLimit);

    /**
     * Sets acceleration to 0 <br>
     * FR : Débraye (met l'accélération des roues à 0), quand on freine
     */
    void disengageEngine();

    /**
     * Brakes with normal brakes
     *
     * @param strength The strength, between 0 and 1f
     */
    void brake(float strength);

    /**
     * Brakes with hand brake
     *
     * @param strength The strength
     */
    void handbrake(float strength);

    /**
     * Steers
     *
     * @param strength The strength, between -1 and 1f (-1 for the left, 1 for the right)
     */
    void steer(float strength);

    /**
     * Applies the "natural" brake of the engine
     */
    void applyEngineBraking(IEngineModule engine);
}
