package fr.hermes.api.mc.world;

import fr.hermes.api.mc.entities.HmPlayerEntity;
import net.minecraft.client.Minecraft;

public interface HmClientWorld extends HmWorld {
    default boolean hm$isClient() {
        return true;
    }

    HmPlayerEntity hm$getClientPlayer();

    // TODO USE HmMinecraftClient
    Minecraft hm$getMinecraftClient();
}
