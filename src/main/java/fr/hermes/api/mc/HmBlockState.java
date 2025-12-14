package fr.hermes.api.mc;

import fr.dynamx.core.utils.optimization.MutableBoundingBox;
import fr.hermes.api.mc.HmWorld;
import fr.hermes.api.mc.HmServerWorld;
import org.joml.Vector3i;
import java.util.List;

public interface HmBlockState {
    boolean isLiquid();

    MutableBoundingBox getBoundingBox(HmServerWorld hmWorld, Vector3i blockPos);

    /**
     * True if this block state blocks movement (is considered solid for stacking checks)
     */
    boolean blocksMovement();

    void addCollisionBoxes(HmWorld world, Vector3i blockPos, MutableBoundingBox checkZone, List<MutableBoundingBox> out);
}
