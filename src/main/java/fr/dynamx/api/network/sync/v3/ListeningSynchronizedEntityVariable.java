package fr.dynamx.api.network.sync.v3;

import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.network.sync.SyncTarget;
import net.minecraftforge.fml.relauncher.Side;

import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

public class ListeningSynchronizedEntityVariable<T> extends SynchronizedEntityVariable<T> {
    private final Callable<T> valueUpdater;

    public ListeningSynchronizedEntityVariable(BiConsumer<SynchronizedEntityVariable<T>, T> receiveCallback, SynchronizationRules synchronizationRule, SynchronizedVariableSerializer<T> serializer, Callable<T> valueUpdater, String name) {
        super(receiveCallback, synchronizationRule, serializer, name);
        this.valueUpdater = valueUpdater;
    }

    @Override
    public SyncTarget getSyncTarget(SimulationHolder simulationHolder, Side side) {
        if(!getSynchronizationRule().listensSide(simulationHolder, side))
            return SyncTarget.NONE;
        try {
            set(getValueUpdater().call());
        } catch (Exception e) {
            throw new RuntimeException("Cannot get synchronized entity variable value !", e);
        }
        return super.getSyncTarget(simulationHolder, side);
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
