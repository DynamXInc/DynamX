package fr.dynamx.core.common.physics.terrain.chunk;

/**
 * The state of a {@link ChunkLoadingTicket}
 */
public enum ChunkState {
    /**
     * Not loaded
     */
    NONE,
    /**
     * Loading
     */
    LOADING,
    /**
     * Loaded into the {@link fr.dynamx.api.physics.terrain.ITerrainManager}
     */
    LOADED
}
