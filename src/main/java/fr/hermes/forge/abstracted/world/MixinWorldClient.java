package fr.hermes.forge.abstracted.world;

import fr.dynamx.core.utils.DynamXConstants;
import fr.hermes.api.mc.entities.HmPlayerEntity;
import fr.hermes.api.mc.world.HmClientWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

// TODO CHANGE REMAP TARGET
@Mixin(value = WorldClient.class, remap = DynamXConstants.REMAP)
public abstract class MixinWorldClient extends MixinWorld implements HmClientWorld {
    @Shadow
    @Final
    private Minecraft mc;

    @Override
    public HmPlayerEntity hm$getClientPlayer() {
        return (HmPlayerEntity) mc.player;
    }

    @Override
    public boolean hm$chunkExists(int x, int z) {
        return chunkProvider.isChunkGeneratedAt(x, z);
    }
}
