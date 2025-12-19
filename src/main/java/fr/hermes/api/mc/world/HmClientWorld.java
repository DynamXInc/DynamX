package fr.hermes.api.mc.world;

import fr.hermes.api.mc.entities.HmPlayerEntity;

public interface HmClientWorld extends HmWorld {
    default boolean isClient() {
        return true;
    }

    HmPlayerEntity getClientPlayer();
}
