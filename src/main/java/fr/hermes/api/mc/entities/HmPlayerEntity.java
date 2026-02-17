package fr.hermes.api.mc.entities;

import com.mojang.authlib.GameProfile;
import fr.hermes.api.mc.items.HmItemStack;
import net.minecraft.util.EnumHand;
import org.spongepowered.asm.mixin.MixinEnvironment;

public interface HmPlayerEntity extends HmLivingEntity {
    MixinEnvironment.Side hm$getSide();

    GameProfile hm$getGameProfile();

    boolean hm$isLocalPlayer();

    boolean hm$isCreativeMode();

    boolean hm$isSpectator();
}
