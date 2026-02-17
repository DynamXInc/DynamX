package fr.hermes.api.platform;

import fr.hermes.api.mc.McObjectBinder;
import fr.hermes.api.mc.client.HmMinecraftClient;
import fr.hermes.api.mc.HmMinecraftServer;
import fr.hermes.api.mc.utils.HmResourceLocation;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// TODO Platform loader
public abstract class HermesPlatform {
    public static final Logger log = LogManager.getLogger("HermesPlatform");

    public static final String RESOURCES_DOMAIN = "hermes";

    @Getter
    private static HermesPlatform instance;

    public abstract HmMinecraftClient getClient();

    public abstract HmMinecraftServer getServer();

    public abstract HermesPlatformLoader getLoader();
}
