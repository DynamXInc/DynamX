package fr.dynamx.api.entities.callbacks;

import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.common.entities.ModularPhysicsEntity;

import java.util.List;

/**
 * Init callback for {@link ModularPhysicsEntity} <br>
 * The entity is initialized during the first tick of its existence
 */
public interface ModularEntityInitCallback {
    /**
     * Fired when the entity modules has been initialized
     *
     * @param modularEntity The modular entity
     * @param modules       The entity modules list, modifiable
     */
    void onEntityInit(ModularPhysicsEntity<?> modularEntity, List<IPhysicsModule<?>> modules);
}
