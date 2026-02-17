package fr.hermes.forge.abstracted.utils;

import fr.dynamx.core.utils.DynamXConstants;
import fr.hermes.api.mc.utils.HmBlockPos;
import fr.hermes.api.mc.utils.HmVec3i;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

// TODO CHANGE REMAP TARGET
@Mixin(value = Vec3i.class, remap = DynamXConstants.REMAP)
public abstract class MixinVec3i implements HmVec3i {
    @Shadow
    public abstract int getX();

    @Shadow
    public abstract int getY();

    @Shadow
    public abstract int getZ();

    @Override
    public int hm$getX() {
        return getX();
    }

    @Override
    public int hm$getY() {
        return getY();
    }

    @Override
    public int hm$getZ() {
        return getZ();
    }
}
