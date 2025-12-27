package fr.hermes.forge.abstracted;

import com.google.common.util.concurrent.ListenableFuture;
import fr.dynamx.core.utils.DynamXConstants;
import fr.hermes.api.mc.HmMinecraftServer;
import fr.hermes.api.mc.world.HmServerWorld;
import fr.hermes.api.mc.world.HmWorld;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MinecraftServer.class, remap = DynamXConstants.REMAP)
public abstract class MixinMinecraftServer implements HmMinecraftServer {
    @Shadow
    public abstract boolean isDedicatedServer();

    @Shadow
    public abstract String getServerHostname();

    @Shadow
    public abstract PlayerList getPlayerList();

    @Shadow
    public abstract World getEntityWorld();

    @Shadow
    public abstract int getTickCounter();

    @Shadow
    public abstract ListenableFuture<Object> addScheduledTask(Runnable runnableToSchedule);

    @Shadow
    public WorldServer[] worlds;

    @Override
    public boolean hm$isDedicatedServer() {
        return isDedicatedServer();
    }

    @Override
    public String hm$getHostname() {
        return getServerHostname();
    }

    @Override
    public void hm$sendGlobalChatMessage(String message) {
        getPlayerList().sendMessage(new TextComponentString(message));
    }

    @Override
    public void hm$addScheduledTask(Runnable task) {
        addScheduledTask(task);
    }

    @Override
    public HmServerWorld hm$getWorld() {
        return (HmServerWorld) getEntityWorld();
    }

    @Override
    public int hm$getTickCounter() {
        return getTickCounter();
    }

    @Override
    public HmServerWorld[] hm$getWorlds() {
        return (HmServerWorld[]) worlds;
    }
}
