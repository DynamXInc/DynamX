package fr.hermes.api.events;

import lombok.Getter;

import javax.annotation.Nullable;

/**
 * Represents the result of an event callback that can control event propagation
 * and optionally carry a result value.
 *
 * @param <T> The type of the result value (use Void if no result is needed)
 */
public class HmEventResult<T> {
    /**
     * -- GETTER --
     *
     * @return The result type
     */
    @Getter
    private final Type type;
    @Nullable
    private final T result;

    private HmEventResult(Type type, @Nullable T result) {
        this.type = type;
        this.result = result;
    }

    /**
     * The event should continue to other listeners.
     */
    public static <T> HmEventResult<T> pass() {
        return new HmEventResult<>(Type.PASS, null);
    }

    /**
     * The event was handled successfully, stop processing further listeners.
     */
    public static <T> HmEventResult<T> success() {
        return new HmEventResult<>(Type.SUCCESS, null);
    }

    /**
     * The event was handled successfully with a result value.
     *
     * @param result The result value
     */
    public static <T> HmEventResult<T> success(T result) {
        return new HmEventResult<>(Type.SUCCESS, result);
    }

    /**
     * The event should be cancelled, stop processing further listeners.
     */
    public static <T> HmEventResult<T> cancel() {
        return new HmEventResult<>(Type.CANCEL, null);
    }

    /**
     * The event should be cancelled with a result value.
     *
     * @param result The result value
     */
    public static <T> HmEventResult<T> cancel(T result) {
        return new HmEventResult<>(Type.CANCEL, result);
    }

    /**
     * @return The result value, or null if none was provided
     */
    @Nullable
    public T getResult() {
        return result;
    }

    /**
     * @return true if a result value is present
     */
    public boolean hasResult() {
        return result != null;
    }

    /**
     * @return true if the event should stop propagating to other listeners
     */
    public boolean shouldStopPropagation() {
        return type != Type.PASS;
    }

    /**
     * @return true if the event was cancelled
     */
    public boolean isCancelled() {
        return type == Type.CANCEL;
    }

    /**
     * @return true if the event completed successfully
     */
    public boolean isSuccess() {
        return type == Type.SUCCESS;
    }

    /**
     * @return true if the event should pass to the next listener
     */
    public boolean isPass() {
        return type == Type.PASS;
    }

    /**
     * The type of event result.
     */
    public enum Type {
        /**
         * The event should continue to other listeners.
         */
        PASS,
        /**
         * The event was handled successfully, stop processing.
         */
        SUCCESS,
        /**
         * The event should be cancelled.
         */
        CANCEL
    }
}
