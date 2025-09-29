package fr.hermes.api.mc;

import fr.dynamx.core.utils.optimization.MutableBoundingBox;
import org.joml.Vector3i;

public interface HmBlockState {
    boolean isLiquid();

    MutableBoundingBox getBoundingBox(HmServerWorld hmWorld, Vector3i blockPos);
}
