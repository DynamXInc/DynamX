package fr.dynamx.common.network.sync;

import com.google.common.collect.Queues;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.client.network.ClientPhysicsEntitySynchronizer;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.sync.variables.NetworkActivityTracker;
import fr.dynamx.common.network.sync.variables.SynchronizedEntityVariableSnapshot;
import lombok.Getter;

import java.util.Queue;
import java.util.stream.Collectors;

public abstract class MPPhysicsEntitySynchronizer<T extends PhysicsEntity<?>> extends PhysicsEntitySynchronizer<T> {
    /**
     * Ordered sync waiting packet queue, we need this because when network lags, we receive all packets at the same tick
     */
    @Getter
    private final Queue<MessagePhysicsEntitySync<T>> receivedPackets = Queues.newArrayDeque();

    public MPPhysicsEntitySynchronizer(T entity) {
        super(entity);
    }

    public void receiveEntitySyncPacket(MessagePhysicsEntitySync<T> msg) {
        receivedPackets.offer(msg);
    }

    public void readReceivedPackets() {
        if(!receivedPackets.isEmpty()) {
            while (!receivedPackets.isEmpty()) {
                MessagePhysicsEntitySync<T> msg = receivedPackets.remove();
                getReceivedVariables().putAll(msg.getVarsToRead());
                setSimulationTimeClient(msg.getSimulationTimeClient());
                onDataReceived(msg);
            }
            getReceivedVariables().forEach((key, value) -> ((SynchronizedEntityVariableSnapshot<Object>) value).updateVariable(tryGetVariable(key)));
        }
    }

    protected void onDataReceived(MessagePhysicsEntitySync<T> msg) {}

    public abstract void setSimulationTimeClient(int simulationTimeClient);
}
