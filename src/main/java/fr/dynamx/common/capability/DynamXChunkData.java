package fr.dynamx.common.capability;

import lombok.Getter;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds collisions data of DynamX blocks in each chunk <br>
 * Used for destroy and interaction raytracing
 */
public class DynamXChunkData {
    @Getter
    private final Map<BlockPos, AxisAlignedBB> blocksAABB = new HashMap<>();
}
