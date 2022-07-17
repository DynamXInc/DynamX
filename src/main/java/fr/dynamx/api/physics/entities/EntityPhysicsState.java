package fr.dynamx.api.physics.entities;

/**
 * The physics state of the entity
 */
public enum EntityPhysicsState {
    /**
     * Enable physics
     */
    ENABLE,
    /**
     * Object can be frozen if too far from players
     */
    FREEZE,
    /**
     * Object will be frozen next tick <br>
     * Adds a 1-tick delay in case of little de-syncs from the physics thread
     */
    WILL_FREEZE,
    /**
     * Frozen object, when too far from players
     */
    FROZEN,
    /**
     * Object should be enabled again
     */
    UNFREEZE
}
