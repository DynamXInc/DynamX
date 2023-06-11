package fr.dynamx.common.core.mixin;

import fr.dynamx.common.DynamXContext;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Patches the NetHandlerPlayServer to disable console spam when players walk on moving vehicles
 */
@Mixin(value = NetHandlerPlayServer.class, remap = DynamXConstants.REMAP)
public class MixinNetHandlerPlayServer {

    @Shadow public EntityPlayerMP player;

    @Redirect(method = "processPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayerMP;isInvulnerableDimensionChange()Z", ordinal = 0))
    private boolean test(EntityPlayerMP instance){
        if (DynamXContext.getWalkingPlayers().containsKey(player)) {
            //TODO IMPROVE SECURITY
            //LOGGER.warn("Ignoring moving too quickly from " + player.getName() + " : walking on vehicle !");
            return false;
        } else {
            return true;
        }
    }

    @Redirect(method = "processPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayerMP;isInvulnerableDimensionChange()Z", ordinal = 1))
    private boolean test2(EntityPlayerMP instance){
        if (DynamXContext.getWalkingPlayers().containsKey(player)) {
            //TODO IMPROVE SECURITY
            //LOGGER.warn("Ignoring moving wrongly from " + player.getName() + " : walking on vehicle !");
            return false;
        } else {
            return true;
        }
    }
}
