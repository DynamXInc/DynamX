package fr.hermes.forge.platform;

import fr.hermes.api.mc.HmMinecraftServer;
import fr.hermes.api.mc.client.HmMinecraftClient;
import fr.hermes.api.platform.HermesPlatform;
import fr.hermes.api.platform.HermesPlatformLoader;
import fr.hermes.forge.wrappers.HmForgeBaseBlockEntity;
import fr.hermes.forge.wrappers.HmForgeBaseEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.concurrent.atomic.AtomicBoolean;

public class HermesForgePlatform extends HermesPlatform implements HermesPlatformLoader {
    private final AtomicBoolean loaded = new AtomicBoolean(false);

    @Override
    @SideOnly(Side.CLIENT)
    public HmMinecraftClient getClient() {
        return (HmMinecraftClient) Minecraft.getMinecraft();
    }

    @Override
    public HmMinecraftServer getServer() {
        return (HmMinecraftServer) FMLCommonHandler.instance().getMinecraftServerInstance();
    }

    @Override
    public HermesPlatformLoader getLoader() {
        return this;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void initializeClient(HermesPlatform platform, HmMinecraftClient client) {
        if (setLoading()) {
            return;
        }
        initializeCommon();

        // TODO WORK ON TE RENDERING?
        // how do we want to deal with TEs?
    }

    @Override
    public void initializeServer(HermesPlatform platform, HmMinecraftServer server) {
        if (setLoading()) {
            return;
        }
        initializeCommon();
    }

    private boolean setLoading() {
        if (loaded.get()) {
            return true;
        }
        loaded.set(true);
        return false;
    }

    private void initializeCommon() {
        GameRegistry.registerTileEntity(HmForgeBaseBlockEntity.class, new ResourceLocation(HermesPlatform.RESOURCES_DOMAIN, "common_block_entity"));
        // TODO tweak tracking properties
        EntityRegistry.registerModEntity(new ResourceLocation(HermesPlatform.RESOURCES_DOMAIN, "common_entity"), HmForgeBaseEntity.class, "common_entity", 102, this, 200, 4, false);

        // TODO REGISTER TE AND ENTITIES ETC
    }
}
