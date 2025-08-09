package fr.dynamx.api.physics.terrain;

import fr.dynamx.core.common.physics.terrain.PhysicsEntityTerrainLoader;
import fr.dynamx.core.common.slopes.SlopesPreviewTerrainLoader;
import fr.dynamx.core.utils.debug.Profiler;

/**
 * An object managing the loading of physic terrain for itself <br>
 * You should register it in the {@link DynamXTerrainApi}
 *
 * @see PhysicsEntityTerrainLoader
 * @see SlopesPreviewTerrainLoader
 */
public interface IPhysicsTerrainLoader {
    /**
     * Updates this terrain loader
     *
     * @param terrain The current terrain manager, that will load your chunks
     * @param profiler The current profiler
     */
    void update(ITerrainManager terrain, Profiler profiler);

    /**
     * Releases the chunks loaded by this terrain loader
     * @param terrain The current terrain manager, that loaded your chunks
     */
    void onRemoved(ITerrainManager terrain);
}
