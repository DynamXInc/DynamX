package fr.dynamx.api.network.sync;

import fr.dynamx.utils.DynamXConfig;
import net.minecraftforge.fml.relauncher.Side;

/**
 * All possible holder of the simulation of an entity, they will be responsible to sync the entity data to the other clients (and to the server if required)
 */
public enum SimulationHolder {
    /**
     * The driver is holding all entity's physics, sending it to the server (with verifications)
     */
    DRIVER(true),
    /**
     * Same as DRIVER, but in singleplayer. The client holds all the physics and the controls.
     */
    DRIVER_SP(true),
    /**
     * The server is holding all entity's physics
     */
    SERVER(false),
    /**
     * Another client is holding all entity's physics
     */
    OTHER_CLIENT(false),
    /**
     * Only in singleplayer. The integrated single player server is holding entity's controls. The physics are on the client side.
     */
    SERVER_SP(true);

    private final boolean hasClientPhysics;

    SimulationHolder(boolean hasClientPhysics) {
        this.hasClientPhysics = hasClientPhysics;
    }

    /**
     * Tests if this simulation holder owns the physics on the current side
     *
     * @return True if this physics simulation holder is me
     */
    public boolean ownsPhysics(Side side) {
        if (DynamXConfig.clientOwnsPhysicsInSolo) {
            return side.isClient() ? this.hasClientPhysics : this == SERVER;
        }
        return side.isClient() ? (this == DRIVER || this == DRIVER_SP) : (this == SERVER || this == SERVER_SP);
    }

    /**
     * Tests if this simulation holder owns the controls (engine on, lights states read from nbt...) on the current side
     *
     * @return True if this controls simulation holder is me
     */
    public boolean ownsControls(Side side) {
        return side.isClient() ? (this == DRIVER || this == DRIVER_SP) : (this == SERVER || this == SERVER_SP);
    }

    /**
     * Tests if the current side is playing the main physics simulation <br>
     * The main simulation is the simulation having an authority on the others, e.g. the server in multiplayer <br>
     * In solo mode, this is the client side
     *
     * @param currentSide The current side
     * @return True if the current side is the physics authority <br>
     */
    public boolean isPhysicsAuthority(Side currentSide) {
        return (!this.isSinglePlayer() && currentSide.isServer()) || ownsPhysics(currentSide);
    }

    public boolean hasClientPhysics() {
        return hasClientPhysics;
    }

    public boolean isSinglePlayer() {
        return this == SERVER_SP || this == DRIVER_SP;
    }

    /**
     * The simulation holder update context, changes the affected entities (linked entities, entities in props containers...)
     */
    public enum UpdateContext {
        /**
         * Normal context, all linked entities will be affected
         */
        NORMAL,
        /**
         * Attached entity, other attached entities won't be affect
         */
        ATTACHED_ENTITIES,
        /**
         * Entity in a props container, other entities in props containers won't be affect
         */
        PROPS_CONTAINER_UPDATE
    }
}
