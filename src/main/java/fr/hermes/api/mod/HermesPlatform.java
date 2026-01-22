package fr.hermes.api.mod;

import fr.hermes.api.mc.HmMinecraftClient;
import fr.hermes.api.mc.HmMinecraftServer;
import lombok.Getter;

// TODO Platform loader
public abstract class HermesPlatform {
    @Getter
    private static HermesPlatform instance;

    public abstract HmMinecraftClient getClient();

    public abstract HmMinecraftServer getServer();
}
