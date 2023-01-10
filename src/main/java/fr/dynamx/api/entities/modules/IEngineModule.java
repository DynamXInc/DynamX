package fr.dynamx.api.entities.modules;

import fr.dynamx.api.physics.entities.IEnginePhysicsHandler;
import fr.dynamx.client.handlers.hud.CarController;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;

/**
 * Base interface for engine modules, responsible to handle player controls, and give acceleration/brake to the {@link IPropulsionModule} <br>
 * Requires a propulsion module in order to work <br>
 * Needs an {@link IEnginePhysicsHandler}
 */
public interface IEngineModule<P extends AbstractEntityPhysicsHandler<?, ?>> extends IPhysicsModule<P> {
    //TODO CLEAN THIS

    /**
     * @return engine properties such as speed or rpm <br>
     * {@link fr.dynamx.common.entities.modules.EngineModule} implementation uses {@link fr.dynamx.api.entities.VehicleEntityProperties.EnumEngineProperties}
     */
    float[] getEngineProperties();

    /**
     * @return True if the engine is started, used for sounds
     */
    boolean isEngineStarted();

    /**
     * Sets the engine started state
     */
    void setEngineStarted(boolean started);

    /**
     * @return The physics of this engine
     */
    IEnginePhysicsHandler getPhysicsHandler();

    /**
     * Used for sync and {@link CarController}, currently hard coded
     */
    void setEngineProperties(float[] engineProperties);
}
