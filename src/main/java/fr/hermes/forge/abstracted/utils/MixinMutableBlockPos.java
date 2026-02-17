package fr.hermes.forge.abstracted.utils;

import fr.dynamx.core.utils.DynamXConstants;
import fr.hermes.api.mc.utils.HmBlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

// TODO CHANGE REMAP TARGET
@Mixin(value = BlockPos.MutableBlockPos.class, remap = DynamXConstants.REMAP)
public abstract class MixinMutableBlockPos implements HmBlockPos.HmMutableBlockPos {
    @Shadow
    public abstract BlockPos.MutableBlockPos setPos(int xIn, int yIn, int zIn);

    @Shadow
    public abstract BlockPos.MutableBlockPos setPos(double xIn, double yIn, double zIn);

    @Override
    public HmMutableBlockPos hm$setPos(int xIn, int yIn, int zIn) {
        return (HmMutableBlockPos) setPos(xIn, yIn, zIn);
    }

    @Override
    public HmMutableBlockPos hm$setPos(double xIn, double yIn, double zIn) {
        return (HmMutableBlockPos) setPos(xIn, yIn, zIn);
    }
}
