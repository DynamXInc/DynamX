package fr.hermes.api.platform;

import fr.hermes.api.mc.HmMinecraftServer;
import fr.hermes.api.mc.client.HmMinecraftClient;

public interface HermesPlatformLoader {
    void initializeClient(HermesPlatform platform, HmMinecraftClient client);

    void initializeServer(HermesPlatform platform, HmMinecraftServer server);
}
