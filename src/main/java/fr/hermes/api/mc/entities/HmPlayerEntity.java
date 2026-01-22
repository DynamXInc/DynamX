package fr.hermes.api.mc.entities;

import com.mojang.authlib.GameProfile;
import fr.hermes.api.mc.items.HmItemStack;
import net.minecraft.util.EnumHand;
import org.spongepowered.asm.mixin.MixinEnvironment;

/**
 * A Minecraft {@link net.minecraft.entity.player.PlayerEntity}
 */
public interface HmPlayerEntity extends HmLivingEntity {
    MixinEnvironment.Side hm$getSide();

    GameProfile hm$getGameProfile();

    void hm$sendMessage(String message);

    boolean hm$isLocalPlayer();

    HmItemStack hm$getHeldItemMainhand();

    boolean hm$isCreativeMode();

    boolean hm$isSneaking();

    HmItemStack hm$getHeldItem(EnumHand hand);
}
