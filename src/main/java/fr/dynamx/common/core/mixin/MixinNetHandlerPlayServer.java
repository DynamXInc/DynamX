package fr.dynamx.common.core.mixin;

import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.block.Block;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * Patches the NetHandlerPlayServer to disable console spam when players walk on moving vehicles
 */
@Mixin(value = NetHandlerPlayServer.class, remap = DynamXConstants.REMAP)
public class MixinNetHandlerPlayServer {

    @Shadow
    public EntityPlayerMP player;

    @Redirect(method = "processPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayerMP;isInvulnerableDimensionChange()Z", ordinal = 0))
    private boolean test(EntityPlayerMP instance) {
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
    private boolean test2(EntityPlayerMP instance) {
        if (DynamXContext.getWalkingPlayers().containsKey(player)) {
            //TODO IMPROVE SECURITY
            //LOGGER.warn("Ignoring moving wrongly from " + player.getName() + " : walking on vehicle !");
            return false;
        } else {
            return true;
        }
    }

    @Unique
    private int dynamX$overrideReachDistance;

    @Inject(method = "processPlayerDigging(Lnet/minecraft/network/play/client/CPacketPlayerDigging;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/attributes/IAttributeInstance;getAttributeValue()D"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void captureLocals(CPacketPlayerDigging packet, CallbackInfo ci) {
        if (packet.getAction() == CPacketPlayerDigging.Action.START_DESTROY_BLOCK || packet.getAction() == CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK || packet.getAction() == CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK){
            WorldServer world = this.player.getServerWorld();
            BlockPos pos = packet.getPosition();
            Block block = world.getBlockState(pos).getBlock();
            this.dynamX$overrideReachDistance = (block instanceof DynamXBlock) ? 20 : -1;
        }
    }

    @Redirect(method = "processPlayerDigging(Lnet/minecraft/network/play/client/CPacketPlayerDigging;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/attributes/IAttributeInstance;getAttributeValue()D"))
    private double overrideReachDistance(IAttributeInstance instance) {
        return this.dynamX$overrideReachDistance == -1 ? instance.getAttributeValue() : this.dynamX$overrideReachDistance;
    }
}
