package fr.dynamx.api.physics.terrain;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Defines the physics terrain update when a block is changed in the world <br>
 * This allows to ignore certain block updates, for example regular changes of animated blocks from Decocraft mod, and therefore improve the performance of the physics engine
 */
public interface ITerrainUpdateBehavior {
    /**
     * Fired when a block changes in the world, except if another {@link ITerrainUpdateBehavior} has already handled it <br>
     * Return {@link Result#DO_UPDATE} to force the update of the terrain (should not be done too often for performance reasons) <br>
     * Return {@link Result#IGNORE} to ignore this block update (the physics terrain won't change) <br>
     * Return {@link Result#CONTINUE} to use the default behavior (the physics terrain will change one of the block states blocks movement or if the full cube state change) <br> <br>
     * This method should be as restrictive as possible, to let different blocks handled by other collision behaviors.
     *
     * @param world    The world
     * @param pos      The block pos
     * @param oldState The old block state
     * @param newState The new block state
     * @return The result of this update behavior.
     */
    Result getResult(IBlockAccess world, BlockPos pos, IBlockState oldState, IBlockState newState);

    /**
     * The result of an {@link ITerrainUpdateBehavior}
     */
    enum Result {
        CONTINUE, IGNORE, DO_UPDATE
    }

    /**
     * The default update behavior, that updates the terrain if the block states blocks movement or if the full cube state change
     */
    class DefaultUpdateBehavior implements ITerrainUpdateBehavior {
        @Override
        public Result getResult(IBlockAccess world, BlockPos pos, IBlockState oldState, IBlockState newState) {
            if (!(oldState.isFullCube() && newState.isFullCube()) && (oldState.getMaterial().blocksMovement() || newState.getMaterial().blocksMovement()))
                return Result.DO_UPDATE;
            return Result.IGNORE;
        }
    }
}
