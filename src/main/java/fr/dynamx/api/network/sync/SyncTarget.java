package fr.dynamx.api.network.sync;

import net.minecraftforge.fml.relauncher.Side;

/**
 * All possible targets for {@link SynchronizedVariable} <br>
 *     Determines the targets of sync packets
 */
public enum SyncTarget
{
    /** Send to no one, except players starting to track the entity */
    NONE,
    /** Send to server */
    SERVER,
    /** Send to all tracking client */
    ALL_CLIENTS,
    /** Send to all tracking clients except the driver (reduces network charge) */
    SPECTATORS,
    /** Send to the driver of this entity */
    DRIVER;

    /**
     * True if the given target (in parameter) includes this target (example : ALL_CLIENTS includes SPECTATORS)
     */
    public boolean isIncluded(SyncTarget target)
    {
        return target == this || (target == ALL_CLIENTS && (this == DRIVER || this == SPECTATORS)) || (target == SPECTATORS);
    }

    /**
     * @return if side is client : SPECTATORS, else SERVER
     */
    public static SyncTarget spectatorForSide(Side side)
    {
        return side.isServer() ? SERVER : SPECTATORS;
    }

    /**
     * Same as spectatorForSide
     * @return if side is client : SPECTATORS, else SERVER
     */
    public static SyncTarget nearSpectatorForSide(Side side)
    {
        return side.isServer() ? SERVER : SPECTATORS;
    }
}
