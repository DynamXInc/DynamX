package fr.hermes.api.mc.events;

import fr.hermes.api.events.HmEvent;
import fr.hermes.api.events.ListenableHmEvent;
import fr.hermes.api.mc.entities.HmPlayerEntity;
import fr.hermes.api.mc.entities.HmServerPlayerEntity;

/**
 * Registry of Minecraft common player events
 */
public final class HmPlayerEvents {
    /**
     * Callback for player join events.
     */
    @FunctionalInterface
    public interface PlayerJoinCallback {
        void onPlayerJoin(HmServerPlayerEntity player);
    }

    /**
     * Callback for player leave events.
     */
    @FunctionalInterface
    public interface PlayerLeaveCallback {
        void onPlayerLeave(HmServerPlayerEntity player);
    }

    /**
     * Called when a player joins the server.
     */
    public static final HmEvent<PlayerJoinCallback> JOIN = ListenableHmEvent.create(event ->
            (player) -> event.forEach(l -> l.onPlayerJoin(player))
    );

    /**
     * Called when a player leaves the server.
     */
    public static final HmEvent<PlayerLeaveCallback> LEAVE = ListenableHmEvent.create(event ->
            (player) -> event.forEach(l -> l.onPlayerLeave(player))
    );
}
