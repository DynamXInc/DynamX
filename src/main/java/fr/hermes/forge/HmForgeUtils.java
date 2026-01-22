package fr.hermes.forge;

import fr.dynamx.core.common.blocks.TEDynamXBlock;
import fr.hermes.api.mod.HermesUtils;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.net.URL;

import static fr.dynamx.core.utils.DynamXConstants.ID;

public class HmForgeUtils implements HermesUtils {
    @Override
    public void addPathToClasspath(URL path) {
        ((LaunchClassLoader) Thread.currentThread().getContextClassLoader()).addURL(path);
    }

    @Override
    public void registerMcObjects() {
        GameRegistry.registerTileEntity(TEDynamXBlock.class, new ResourceLocation(ID + ":dynamxblock"));
    }
}
