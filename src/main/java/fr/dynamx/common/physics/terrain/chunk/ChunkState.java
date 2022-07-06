package fr.dynamx.common.physics.terrain.chunk;

import fr.dynamx.common.physics.terrain.chunk.ChunkLoadingTicket;

/**
 * The state of a {@link ChunkLoadingTicket}
 */
public enum ChunkState
{
    /** Not loaded */
    NONE,
    /** Loading */
    LOADING,
    /** Loaded into the {@link fr.dynamx.api.physics.terrain.ITerrainManager} */
    LOADED;
}
