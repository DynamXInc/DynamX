package fr.hermes.api.mc.blocks;

import fr.dynamx.core.utils.optimization.MutableBoundingBox;
import fr.hermes.api.mc.world.HmWorld;
import fr.hermes.api.mc.world.HmServerWorld;
import org.joml.Vector3i;
import java.util.List;

public interface HmBlockState {
    HmBlock getBlock();

    boolean isLiquid();

    MutableBoundingBox getBoundingBox(HmWorld hmWorld, Vector3i blockPos);

    /**
     * True if this block state blocks movement (is considered solid for stacking checks)
     */
    boolean blocksMovement();

    void addCollisionBoxes(HmWorld world, Vector3i blockPos, MutableBoundingBox checkZone, List<MutableBoundingBox> out);

    boolean isFullCube();
}
