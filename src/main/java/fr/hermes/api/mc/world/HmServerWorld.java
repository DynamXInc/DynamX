package fr.hermes.api.mc.world;

import fr.hermes.api.mc.HmServer;

public interface HmServerWorld extends HmWorld {
    default boolean isClient() {
        return false;
    }

    HmServer getServer();
}
