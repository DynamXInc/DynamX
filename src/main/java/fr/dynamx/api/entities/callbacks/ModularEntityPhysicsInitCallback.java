package fr.dynamx.api.entities.callbacks;

import fr.dynamx.common.entities.ModularPhysicsEntity;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;

import javax.annotation.Nullable;

/**
 * Init callback for {@link ModularEntityPhysicsInitCallback} <br>
 * The physics handler is initialized in the first tick after the entity init
 */
public interface ModularEntityPhysicsInitCallback {

    /**
     * Fired when the entity physics handler has been initialized
     *
     * @param modularEntity The entity
     * @param physicsHandler The created physics handler, or null if physics are not simulated on this side
     */
    void onPhysicsInit(ModularPhysicsEntity<?> modularEntity, @Nullable AbstractEntityPhysicsHandler<?, ?> physicsHandler);
}
