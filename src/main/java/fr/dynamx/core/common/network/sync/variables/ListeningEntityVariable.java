package fr.dynamx.core.common.network.sync.variables;

import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.network.sync.SyncTarget;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SynchronizationRules;

import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

public class ListeningEntityVariable<T> extends EntityVariable<T> {
    private final Callable<T> valueUpdater;

    public ListeningEntityVariable(BiConsumer<EntityVariable<T>, T> receiveCallback, SynchronizationRules synchronizationRule, Callable<T> valueUpdater) {
        super(receiveCallback, synchronizationRule);
        this.valueUpdater = valueUpdater;
    }

    @Override
    public SyncTarget getSyncTarget(SimulationHolder simulationHolder, boolean isClient) {
        if(!getSynchronizationRule().listensSide(simulationHolder, isClient))
            return SyncTarget.NONE;
        try {
            set(getValueUpdater().call());
        } catch (Exception e) {
            throw new RuntimeException("Cannot get synchronized entity variable value !", e);
        }
        return super.getSyncTarget(simulationHolder, isClient);
    }

    public Callable<T> getValueUpdater() {
        return valueUpdater;
    }

    @Override
    public String toString() {
        return "ListeningSynchronizedEntityVariable{" +
                "synchronizationRule=" + getSynchronizationRule() +
                ", serializer=" + getSerializer() +
                ", value=" + get()  +
                '}';
    }
}
