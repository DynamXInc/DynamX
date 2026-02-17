package fr.hermes.forge.abstracted.entities;

import com.mojang.authlib.GameProfile;
import fr.dynamx.core.utils.DynamXConstants;
import fr.hermes.api.mc.entities.HmPlayerEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

// TODO CHANGE REMAP TARGET
@Mixin(value = EntityPlayer.class, remap = DynamXConstants.REMAP)
public abstract class MixinEntityPlayer implements HmPlayerEntity {
    @Shadow
    public abstract GameProfile getGameProfile();

    @Shadow
    public PlayerCapabilities capabilities;

    @Shadow
    public abstract boolean isSpectator();

    @Override
    public GameProfile hm$getGameProfile() {
        return getGameProfile();
    }

    @Override
    public boolean hm$isCreativeMode() {
        return capabilities.isCreativeMode;
    }

    @Override
    public boolean hm$isSpectator() {
        return isSpectator();
    }
}
