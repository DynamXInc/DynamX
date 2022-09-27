package fr.dynamx.api.physics.terrain;

import fr.dynamx.utils.debug.Profiler;

/**
 * An object managing the loading of physic terrain for itself <br>
 * You should register it in the {@link DynamXTerrainApi}
 *
 * @see fr.dynamx.common.physics.terrain.PhysicsEntityTerrainLoader
 * @see fr.dynamx.common.slopes.SlopesPreviewTerrainLoader
 */
public interface IPhysicsTerrainLoader {
    /**
     * Updates this terrain loader
     *
     * @param terrain  The current terrain manager, that will load your chunks
     * @param profiler
     */
    void update(ITerrainManager terrain, Profiler profiler);

    //TODO DOC
    void onRemoved(ITerrainManager terrain);
}
