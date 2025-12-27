package fr.hermes.api.events;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A simple event system inspired by Fabric's event API.
 * <p>
 * Example usage:
 * <pre>
 * // Define a callback interface
 * {@literal @}FunctionalInterface
 * public interface WorldLoadCallback {
 *     void onWorldLoad(HmWorld world);
 * }
 *
 * // Create a simple void event
 * public static final HmEvent&lt;WorldLoadCallback&gt; WORLD_LOAD = HmEvent.create(event ->
 *     world -> event.forEach(l -> l.onWorldLoad(world))
 * );
 *
 * // Create a cancellable event
 * public static final HmEvent&lt;VehicleSpawnCallback&gt; VEHICLE_SPAWN = HmEvent.create(event ->
 *     (world, entity) -> event.process(l -> l.onVehicleSpawn(world, entity))
 * );
 *
 * // Register a listener
 * WORLD_LOAD.register(world -> System.out.println("World loaded: " + world));
 *
 * // Trigger the event
 * WORLD_LOAD.invoker().onWorldLoad(someWorld);
 *
 * // Trigger a cancellable event
 * HmEventResult&lt;Void&gt; result = VEHICLE_SPAWN.invoker().onVehicleSpawn(world, entity);
 * if (result.isCancelled()) { ... }
 * </pre>
 *
 * @param <T> The callback/listener type (should be a functional interface)
 */
public class HmEvent<T> {
    private final List<T> listeners = new ArrayList<>();
    private final Function<HmEvent<T>, T> invokerFactory;
    private T invoker;

    private HmEvent(Function<HmEvent<T>, T> invokerFactory) {
        this.invokerFactory = invokerFactory;
        // Initialize with empty listeners
        this.invoker = invokerFactory.apply(this);
    }

    /**
     * Creates a new event with the given invoker factory.
     * <p>
     * The invoker factory receives this event instance and should return
     * a callback that uses {@link #forEach} or {@link #process} to invoke listeners.
     *
     * @param invokerFactory A function that creates the invoker using the event's helper methods
     * @param <T>            The callback type
     * @return A new HmEvent instance
     */
    public static <T> HmEvent<T> create(Function<HmEvent<T>, T> invokerFactory) {
        return new HmEvent<>(invokerFactory);
    }

    /**
     * Registers a listener for this event.
     *
     * @param listener The listener to register
     */
    public void register(T listener) {
        listeners.add(listener);
        // Invalidate the cached invoker so it gets rebuilt on next access
        invoker = null;
    }

    /**
     * Unregisters a listener from this event.
     *
     * @param listener The listener to remove
     * @return true if the listener was found and removed
     */
    public boolean unregister(T listener) {
        boolean removed = listeners.remove(listener);
        if (removed) {
            invoker = null;
        }
        return removed;
    }

    /**
     * Returns the invoker for this event.
     * <p>
     * The invoker is a single instance of T that, when its method is called,
     * will call all registered listeners in order.
     *
     * @return The combined invoker
     */
    public T invoker() {
        if (invoker == null) {
            invoker = invokerFactory.apply(this);
        }
        return invoker;
    }

    // ==================== Helper methods for invoker factories ====================

    /**
     * Invokes all registered listeners using the provided invoker function.
     * <p>
     * Use this inside your invoker factory for simple void callbacks.
     *
     * @param listenerInvoker A function that calls the appropriate method on each listener
     */
    public void forEach(Consumer<T> listenerInvoker) {
        for (T listener : listeners) {
            listenerInvoker.accept(listener);
        }
    }

    /**
     * Invokes listeners until one returns a non-PASS result.
     * <p>
     * Use this inside your invoker factory for cancellable/result callbacks.
     * <p>
     * Each listener is called via the listenerInvoker function, which should return:
     * <ul>
     *   <li>{@code null} or {@code HmEventResult.pass()} - continue to next listener</li>
     *   <li>{@code HmEventResult.success()} - stop processing, event handled successfully</li>
     *   <li>{@code HmEventResult.cancel()} - stop processing, event cancelled</li>
     * </ul>
     *
     * @param listenerInvoker A function that calls the appropriate method on each listener
     * @param <R>             The result type
     * @return The first non-PASS result, or PASS if all listeners passed
     */
    public <R> HmEventResult<R> process(Function<T, HmEventResult<R>> listenerInvoker) {
        for (T listener : listeners) {
            HmEventResult<R> result = listenerInvoker.apply(listener);
            // null is treated as PASS
            if (result != null && result.shouldStopPropagation()) {
                return result;
            }
        }
        return HmEventResult.pass();
    }
}
