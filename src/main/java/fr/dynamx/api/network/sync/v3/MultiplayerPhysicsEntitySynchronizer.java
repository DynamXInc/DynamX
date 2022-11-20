package fr.dynamx.api.network.sync.v3;

import com.google.common.collect.Queues;
import fr.dynamx.client.network.ClientPhysicsEntitySynchronizer;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.sync.MessagePhysicsEntitySync;
import lombok.Getter;

import java.util.Queue;
import java.util.stream.Collectors;

public abstract class MultiplayerPhysicsEntitySynchronizer<T extends PhysicsEntity<?>> extends PhysicsEntitySynchronizer<T> {
    /**
     * Ordered sync waiting packet queue, we need this because when network lags, we receive all packets at the same tick
     */
    @Getter
    private final Queue<MessagePhysicsEntitySync<T>> receivedPackets = Queues.newArrayDeque();

    public MultiplayerPhysicsEntitySynchronizer(T entity) {
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
                if(this instanceof ClientPhysicsEntitySynchronizer) //todo clean
                    NetworkActivityTracker.addReceivedVars(entity, msg.getVarsToRead().keySet().stream().map(v -> getSynchronizedVariables().get(v)).collect(Collectors.toList()));
            }
            getReceivedVariables().forEach((key, value) -> ((SynchronizedEntityVariableSnapshot<Object>) value).updateVariable((SynchronizedEntityVariable<Object>) getSynchronizedVariables().get(key)));
        }
    }

    public abstract void setSimulationTimeClient(int simulationTimeClient);
}
