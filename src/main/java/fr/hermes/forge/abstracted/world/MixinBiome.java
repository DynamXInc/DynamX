package fr.hermes.forge.abstracted.world;

import fr.dynamx.core.utils.DynamXConstants;
import fr.hermes.api.mc.world.HmBiome;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Biome.class, remap = DynamXConstants.REMAP)
public abstract class MixinBiome implements HmBiome {
    @Shadow
    public abstract String getBiomeName();

    @Shadow
    public abstract boolean canRain();

    @Override
    public String hm$getName() {
        return getBiomeName();
    }

    @Override
    public boolean hm$canRain() {
        return canRain();
    }
}
