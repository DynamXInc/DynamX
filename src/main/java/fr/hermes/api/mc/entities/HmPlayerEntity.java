package fr.hermes.api.mc.entities;

import com.mojang.authlib.GameProfile;
import fr.hermes.api.mc.items.HmItemStack;
import org.spongepowered.asm.mixin.MixinEnvironment;

/**
 * A Minecraft {@link net.minecraft.entity.player.PlayerEntity}
 */
public interface HmPlayerEntity extends HmLivingEntity {
    MixinEnvironment.Side getSide();

    GameProfile getGameProfile();

    void sendMessage(String message);

    boolean isLocalPlayer();

    HmItemStack getHeldItemMainhand();

    boolean isCreativeMode();

    boolean isSneaking();
}
