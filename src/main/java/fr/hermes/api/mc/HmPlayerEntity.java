package fr.hermes.api.mc;

import com.mojang.authlib.GameProfile;
import org.spongepowered.asm.mixin.MixinEnvironment;

/**
 * A Minecraft {@link net.minecraft.entity.player.PlayerEntity}
 */
public interface HmPlayerEntity extends HmEntity {
    MixinEnvironment.Side getSide();

    GameProfile getGameProfile();

    void sendMessage(String message);
}
