package fr.hermes.forge.abstracted.blocks;

import fr.dynamx.core.utils.DynamXConstants;
import fr.hermes.api.mc.blocks.HmBlockEntity;
import fr.hermes.api.mc.world.HmWorld;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

// TODO CHANGE REMAP TARGET
@Mixin(value = TileEntity.class, remap = DynamXConstants.REMAP)
public abstract class MixinTileEntity implements HmBlockEntity {
    @Shadow
    public abstract World getWorld();

    @Shadow
    public abstract BlockPos getPos();

    @Shadow
    public abstract boolean isInvalid();

    @Override
    public HmWorld hm$getWorld() {
        return (HmWorld) getWorld();
    }

    @Override
    public BlockPos hm$getPos() {
        return getPos();
    }

    @Override
    public void hm$markBlockForRenderUpdate() {
        getWorld().markBlockRangeForRenderUpdate(getPos(), getPos());
    }

    @Override
    public boolean hm$isInvalid() {
        return isInvalid();
    }
}
