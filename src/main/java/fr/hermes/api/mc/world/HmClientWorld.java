package fr.hermes.api.mc.world;

import fr.hermes.api.mc.HmMinecraftClient;
import fr.hermes.api.mc.entities.HmPlayerEntity;
import net.minecraft.util.math.BlockPos;

public interface HmClientWorld extends HmWorld {
    default boolean hm$isClient() {
        return true;
    }

    HmPlayerEntity hm$getClientPlayer();

    // TODO USE HmMinecraftClient
    HmMinecraftClient hm$getMinecraftClient();

    int hm$getLightAt(BlockPos pos); // getCombinedLight(pos, 0) with forge 1.12.2
}
