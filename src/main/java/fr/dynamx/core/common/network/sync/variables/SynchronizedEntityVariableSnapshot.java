package fr.dynamx.core.common.network.sync.variables;

import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.EntityVariableSerializer;
import fr.dynamx.core.utils.optimization.PooledHashMap;
import io.netty.buffer.ByteBuf;

/**
 * The received value of a synchronized entity variable, before it is actually applied to the variable. <br>
 * This class includes the method to read the value from the network.
 *
 * @param <T> The type of the variable
 */
public class SynchronizedEntityVariableSnapshot<T> {
    private final EntityVariableSerializer<T> serializer;
    private T value;
    private boolean updated;

    /**
     * Create a new variable snapshot with the given initial value
     *
     * @param serializer The serializer for the variable
     * @param initialValue The initial value of the variable
     */
    public SynchronizedEntityVariableSnapshot(EntityVariableSerializer<T> serializer, T initialValue) {
        this.value = initialValue;
        this.serializer = serializer;
    }

    /**
     * @return The current value of the variable
     */
    public T get() {
        return value;
    }

    /**
     * Update the variable with the current value of this snapshot
     *
     * @param variable The variable to update
     */
    public void updateVariable(EntityVariable<T> variable) {
        if (!updated) {
            return;
        }
        variable.receiveValue(value);
        updated = false;
        if(value instanceof PooledHashMap) {
            ((PooledHashMap<?, ?>) value).release();
        }
    }

    /**
     * Read the value from the network
     *
     * @param buf The buffer to read from
     */
    public void read(ByteBuf buf) {
        value = serializer.readObject(buf);
        updated = true;
    }

    @Override
    public String toString() {
        return "SynchronizedEntityVariableSnapshot{" +
                "serializer=" + serializer +
                ", value=" + value +
                ", updated=" + updated +
                '}';
    }
}
