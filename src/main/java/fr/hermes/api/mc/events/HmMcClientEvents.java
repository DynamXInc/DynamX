package fr.hermes.api.mc.events;

import fr.hermes.api.events.HmEvent;
import fr.hermes.api.events.HmEventPhase;
import fr.hermes.api.events.ListenableHmEvent;
import fr.hermes.api.mc.entities.HmPlayerEntity;
import fr.hermes.api.mc.world.HmWorld;

public final class HmMcClientEvents {
    /**
     * Callback for client tick events.
     */
    @FunctionalInterface
    public interface ClientTickCallback {
        void onClientTick(HmEventPhase phase);
    }

    /**
     * Called every client tick.
     */
    public static final HmEvent<ClientTickCallback> TICK = ListenableHmEvent.create(event ->
            (phase) -> event.forEach(l -> l.onClientTick(phase))
    );
    // TODO INVOKE
}
