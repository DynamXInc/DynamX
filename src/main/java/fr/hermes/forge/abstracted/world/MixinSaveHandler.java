package fr.hermes.forge.abstracted.world;

import fr.dynamx.core.utils.DynamXConstants;
import fr.hermes.api.mc.world.HmWorldSaveHandler;
import net.minecraft.world.storage.SaveHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;

@Mixin(value = SaveHandler.class, remap = DynamXConstants.REMAP)
public abstract class MixinSaveHandler implements HmWorldSaveHandler {
    @Shadow
    public abstract File getWorldDirectory();

    @Override
    public File hm$getWorldDirectory() {
        return getWorldDirectory();
    }
}
