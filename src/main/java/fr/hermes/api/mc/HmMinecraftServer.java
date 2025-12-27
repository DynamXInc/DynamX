package fr.hermes.api.mc;

import fr.hermes.api.mc.world.HmServerWorld;

public interface HmMinecraftServer {
    boolean hm$isDedicatedServer();

    String hm$getHostname(); // mc.getServerHostname()

    void hm$sendGlobalChatMessage(String message);

    void hm$addScheduledTask(Runnable task);

    HmServerWorld hm$getWorld();

    int hm$getTickCounter();
}
