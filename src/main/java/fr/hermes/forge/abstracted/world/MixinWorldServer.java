package fr.hermes.forge.abstracted.world;

import fr.dynamx.core.utils.DynamXConstants;
import fr.hermes.api.mc.HmServer;
import fr.hermes.api.mc.world.HmServerWorld;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

// TODO CHANGE REMAP TARGET
@Mixin(value = WorldServer.class, remap = DynamXConstants.REMAP)
public abstract class MixinWorldServer extends MixinWorld implements HmServerWorld {
    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    public abstract ChunkProviderServer getChunkProvider();

    @Override
    public HmServer hm$getServer() {
        return (HmServer) server;
    }

    @Override
    public boolean hm$chunkExists(int x, int z) {
        return getChunkProvider().chunkExists(x, z);
    }
}
