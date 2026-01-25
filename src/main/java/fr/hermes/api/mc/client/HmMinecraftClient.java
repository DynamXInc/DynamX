package fr.hermes.api.mc.client;

import fr.dynamx.core.utils.debug.renderer.BoatDebugRenderer;
import fr.hermes.api.mc.entities.HmClientPlayerEntity;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.utils.HmGameSettings;
import fr.hermes.api.mc.utils.HmRayTraceResult;
import fr.hermes.api.mc.world.HmClientWorld;
import fr.hermes.api.mod.HmRenderApi;

public interface HmMinecraftClient {
    HmGameSettings hm$getGameSettings();

    HmClientPlayerEntity hm$getPlayer();

    boolean hm$isSingleplayer();

    HmClientWorld hm$getWorld();

    boolean hm$isGamePaused();

    void hm$addScheduledTask(Runnable task);

    HmEntity hm$getRenderViewEntity();

    boolean hm$textureManagerIsLoaded();

    HmRayTraceResult hm$getObjectMouseOver();

    void hm$displayGuiScreen(HmScreen screen);

    void hm$disconnectPlayer(String reason);

    HmRenderApi getHmRenderApi();
}
