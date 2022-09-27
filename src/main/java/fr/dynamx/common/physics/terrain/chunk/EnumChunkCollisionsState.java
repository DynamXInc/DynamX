package fr.dynamx.common.physics.terrain.chunk;

import fr.dynamx.api.physics.terrain.ITerrainManager;

public enum EnumChunkCollisionsState {
    /**
     * Not initialized chunk, or old chunk with invalid data
     */
    INVALID,
    /**
     * Initialized chunk, with its correct position and world, but with no computed collisions
     */
    INITIALIZED,
    /**
     * The collisions of the chunk are being computed, may take place in the server thread or in the {@link ITerrainManager}
     */
    COMPUTING,
    /**
     * Valid chunk with up-to-date collision shape, not added into the physics world
     */
    COMPUTED,
    /**
     * Valid chunk with up-to-date collision shape, with computed elements added into the physics world
     */
    ADDED_COMPUTED,
    /**
     * Valid chunk with up-to-date collision shape, with persistent elements added into the physics world
     */
    ADDED_PERSISTENT,
    /**
     * Valid chunk with up-to-date collision shape, with all elements added into the physics world
     */
    ADDED_ALL;

    public boolean arePersistentElementsAdded() {
        return this == ADDED_ALL || this == ADDED_PERSISTENT;
    }

    public boolean areComputedElementsAdded() {
        return this == ADDED_ALL || this == ADDED_COMPUTED;
    }
}
//BRAVO - Yanis - 19/03/20 00:41