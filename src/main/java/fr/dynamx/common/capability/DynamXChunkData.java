package fr.dynamx.common.capability;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds collisions data of DynamX blocks in each chunk <br>
 * Used for destroy and interaction raytracing
 */
public class DynamXChunkData
{
    private final Map<BlockPos, AxisAlignedBB> blocksAABB = new HashMap<>();

    public Map<BlockPos, AxisAlignedBB> getBlocksAABB() {
        return blocksAABB;
    }
}
