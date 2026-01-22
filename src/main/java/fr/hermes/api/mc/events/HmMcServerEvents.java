package fr.hermes.api.mc.events;

import fr.hermes.api.events.HmEvent;
import fr.hermes.api.events.HmEventPhase;
import fr.hermes.api.events.ListenableHmEvent;

public final class HmMcServerEvents {
    /**
     * Callback for server tick events.
     */
    @FunctionalInterface
    public interface ServerTickCallback {
        void onServerTick(HmEventPhase phase);
    }

    /**
     * Called every server tick.
     */
    public static final HmEvent<ServerTickCallback> TICK = ListenableHmEvent.create(event ->
            (phase) -> event.forEach(l -> l.onServerTick(phase))
    );
    // TODO INVOKE
}
