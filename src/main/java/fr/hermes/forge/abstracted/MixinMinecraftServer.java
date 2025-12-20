package fr.hermes.forge.abstracted;

import fr.dynamx.core.utils.DynamXConstants;
import fr.hermes.api.mc.HmServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.TextComponentString;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MinecraftServer.class, remap = DynamXConstants.REMAP)
public abstract class MixinMinecraftServer implements HmServer {
    @Shadow
    public abstract boolean isDedicatedServer();

    @Shadow
    public abstract String getServerHostname();

    @Shadow
    public abstract PlayerList getPlayerList();

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
}
