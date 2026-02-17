package fr.hermes.forge.abstracted.utils;

import fr.dynamx.core.utils.DynamXConstants;
import fr.hermes.api.mc.utils.HmBlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

// TODO CHANGE REMAP TARGET
@Mixin(value = BlockPos.class, remap = DynamXConstants.REMAP)
public abstract class MixinBlockPos implements HmBlockPos {
    @Shadow
    public abstract BlockPos add(int x, int y, int z);

    @Shadow
    public abstract BlockPos offset(EnumFacing facing, int n);

    @Override
    public HmBlockPos hm$add(int x, int y, int z) {
        return (HmBlockPos) add(x, y, z);
    }

    @Override
    public HmBlockPos hm$offset(EnumFacing facing, int n) {
        return (HmBlockPos) offset(facing, n);
    }
}
