package fr.hermes.api.mc;

import fr.hermes.api.mc.entities.HmClientPlayerEntity;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.utils.HmGameSettings;
import fr.hermes.api.mc.world.HmClientWorld;

public interface HmMinecraftClient {
    HmGameSettings hm$getGameSettings();

    HmClientPlayerEntity hm$getPlayer();

    boolean hm$isSingleplayer();

    HmClientWorld hm$getWorld();

    boolean hm$isGamePaused();

    void hm$addScheduledTask(Runnable task);

    HmEntity hm$getRenderViewEntity();

    boolean hm$textureManagerIsLoaded();
}
