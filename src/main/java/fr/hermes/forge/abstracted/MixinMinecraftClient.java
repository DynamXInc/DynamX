package fr.hermes.forge.abstracted;

import com.google.common.util.concurrent.ListenableFuture;
import fr.dynamx.core.utils.DynamXConstants;
import fr.hermes.api.mc.client.HmMinecraftClient;
import fr.hermes.api.mc.client.HmScreen;
import fr.hermes.api.mc.entities.HmClientPlayerEntity;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.utils.HmGameSettings;
import fr.hermes.api.mc.utils.HmRayTraceResult;
import fr.hermes.api.mc.world.HmClientWorld;
import fr.hermes.client.api.HmRenderApi;
import fr.hermes.client.forge.HmForgeRenderApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

// TODO CHANGE REMAP TARGET
@Mixin(value = Minecraft.class, remap = DynamXConstants.REMAP)
public abstract class MixinMinecraftClient implements HmMinecraftClient {
    private final HmRenderApi renderApi = new HmForgeRenderApi((Minecraft) (Object) this);

    @Shadow
    public GameSettings gameSettings;

    @Shadow
    public EntityPlayerSP player;

    @Shadow
    public abstract boolean isSingleplayer();

    @Shadow
    public WorldClient world;

    @Shadow
    public abstract boolean isGamePaused();

    @Shadow
    public abstract ListenableFuture<Object> addScheduledTask(Runnable runnableToSchedule);

    @Shadow
    @Nullable
    private Entity renderViewEntity;

    @Shadow
    public abstract TextureManager getTextureManager();

    @Shadow
    public RayTraceResult objectMouseOver;

    @Shadow
    public abstract void displayGuiScreen(@Nullable GuiScreen guiScreenIn);

    @Override
    public HmGameSettings hm$getGameSettings() {
        return (HmGameSettings) gameSettings;
    }

    @Override
    public HmClientPlayerEntity hm$getPlayer() {
        return (HmClientPlayerEntity) player;
    }

    @Override
    public boolean hm$isSingleplayer() {
        return isSingleplayer();
    }

    @Override
    public HmClientWorld hm$getWorld() {
        return (HmClientWorld) world;
    }

    @Override
    public boolean hm$isGamePaused() {
        return isGamePaused();
    }

    @Override
    public void hm$addScheduledTask(Runnable task) {
        addScheduledTask(task);
    }

    @Override
    public HmEntity hm$getRenderViewEntity() {
        return (HmEntity) renderViewEntity;
    }

    @Override
    public boolean hm$textureManagerIsLoaded() {
        return getTextureManager() != null;
    }

    @Override
    public HmRayTraceResult hm$getObjectMouseOver() {
        return (HmRayTraceResult) objectMouseOver;
    }

    @Override
    public void hm$displayGuiScreen(HmScreen screen) {
        displayGuiScreen((GuiScreen) screen);
    }

    @Override
    public void hm$disconnectPlayer(String reason) {
        if (player == null) {
            return;
        }
        player.connection.getNetworkManager().closeChannel(new TextComponentString(reason));
    }

    @Override
    public HmRenderApi getHmRenderApi() {
        return renderApi;
    }
}
