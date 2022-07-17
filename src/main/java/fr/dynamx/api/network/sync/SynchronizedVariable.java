package fr.dynamx.api.network.sync;

import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.sync.MessagePhysicsEntitySync;
import fr.dynamx.common.network.sync.vars.VehicleSynchronizedVariables;
import fr.dynamx.utils.debug.Profiler;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;

/**
 * A variable to sync a {@link PhysicsEntity} from {@link SimulationHolder} to other clients <br>
 * This system is different from DataManager of vanilla mc because it adds options of sync, interpolation, and uses UDP <br>
 *
 * @see SynchronizedVariablesRegistry To register your {@link SynchronizedVariable}
 * @see PhysicsEntityNetHandler To how it's used
 * @see VehicleSynchronizedVariables To see implementations of this
 */
public interface SynchronizedVariable<T extends PhysicsEntity<?>> {
    /**
     * Called on simulation side (generally the server but it can be a client if a player is driving the entity), should set the values of the variable (that will be sent to clients) from the values of the entity
     *
     * @param side     The target of the sync
     * @param syncTick The sync tick (increments on each sync), useful to reduce network charge (you can send some date only one tick out of two for example)
     * @return A {@link SyncTarget} describing to whom the var changes must be sent
     */
    SyncTarget getValueFrom(T entity, PhysicsEntityNetHandler<T> network, Side side, int syncTick);

    /**
     * Sets value of the variable on the entity from the value stored in this variable <br>
     * Used for local update without packets in single player, and on server side (where there isn"t interpolation)
     *
     * @param msg  The msg that sent this variable, containing MessageBulletEntitySync.simulationTimeClient. Can be null
     * @param side The side where we call the function
     */
    void setValueTo(T entity, PhysicsEntityNetHandler<T> network, @Nullable MessagePhysicsEntitySync msg, Side side);

    /**
     * Called on client side to update the entity, with interpolation (see DynamXUtils for interpolation)
     *
     * @param msg  The msg that sent this variable, containing MessageBulletEntitySync.simulationTimeClient. Can be null
     * @param step The interpolation step
     * @deprecated Use and implement the other interpolate function
     */
    @Deprecated
    default void interpolate(T entity, PhysicsEntityNetHandler<T> network, @Nullable MessagePhysicsEntitySync msg, int step) {
        setValueTo(entity, network, msg, Side.CLIENT);
    }

    /**
     * Called on client side to update the entity, with interpolation (see DynamXUtils for interpolation)
     *
     * @param msg      The msg that sent this variable, containing MessageBulletEntitySync.simulationTimeClient. Can be null
     * @param profiler The profiler for the current thread
     * @param step     The interpolation step
     */
    default void interpolate(T entity, PhysicsEntityNetHandler<T> network, Profiler profiler, @Nullable MessagePhysicsEntitySync msg, int step) {
        interpolate(entity, network, msg, step);
    }

    default void validate(Object entity, int step) {
    }

    /**
     * Write the variable data here, sent over the network. <br>
     * You should write the values cached from the getValueFrom method.
     *
     * @param compress If true, you should send the minimum amount of data to preserve player bandwidth
     */
    void write(ByteBuf buf, boolean compress);

    /**
     * Write the variable data here, but the values of the entity and not the cached values, sent over the network. <br>
     * <strong>Do NOT use or modify the cached values here. See usages in {@link VehicleSynchronizedVariables}.</strong> <br>
     * <br>
     * Used for direct entity sync when a players starts to track it. See {@link fr.dynamx.common.handlers.TaskScheduler.ResyncItem}. <br>
     * <br>
     * The target side is <strong>always</strong> the server.
     */
    void writeEntityValues(T entity, ByteBuf buf);

    /**
     * Read the variable data here, received from the network
     */
    void read(ByteBuf buf);
}
