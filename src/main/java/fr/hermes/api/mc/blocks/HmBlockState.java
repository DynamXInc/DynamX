package fr.hermes.api.mc.blocks;

import fr.dynamx.core.utils.optimization.MutableBoundingBox;
import fr.hermes.api.mc.world.HmWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public interface HmBlockState {
    HmBlock hm$getBlock();

    boolean hm$isLiquid();

    MutableBoundingBox hm$getBoundingBox(HmWorld hmWorld, BlockPos blockPos);

    /**
     * True if this block state blocks movement (is considered solid for stacking checks)
     */
    boolean hm$blocksMovement();

    void hm$addCollisionBoxes(HmWorld world, BlockPos blockPos, MutableBoundingBox checkZone, List<MutableBoundingBox> out);

    boolean hm$isFullCube();

    boolean hm$isOpaqueCube();
}
