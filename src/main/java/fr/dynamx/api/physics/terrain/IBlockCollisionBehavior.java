package fr.dynamx.api.physics.terrain;

import fr.dynamx.common.physics.terrain.computing.BlockCollisionBehaviors;
import fr.dynamx.common.physics.terrain.computing.TerrainBoxBuilder;
import fr.dynamx.common.physics.terrain.computing.TerrainBoxConstructor;
import fr.dynamx.common.physics.terrain.computing.TerrainCollisionsCalculator;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Custom physic collisions for certain blocks <br>
 * The principle is to merge (stack) all similar blocks, so there is less little boxes, improving the simulation performance
 *
 * @see ITerrainManager
 * @see BlockCollisionBehaviors
 */
public interface IBlockCollisionBehavior {
    /**
     * Fired when beginning a stack box, with the first block boxStart
     *
     * @param terrainBoxConstructor The terrain box constructor
     * @param world                 The world
     * @param mutable               The starting pos
     * @param boxStart              The starting block (accepted by the applies method)
     * @param ox                    The current local translation on x axis
     * @param oy                    The current local translation on y axis
     * @param oz                    The current local translation on z axis
     * @return The {@link TerrainBoxBuilder} to stack similar blocks on boxStart
     */
    default TerrainBoxBuilder initBoxBuilder(TerrainBoxConstructor terrainBoxConstructor, World world, BlockPos mutable, IBlockState boxStart, double ox, double oy, double oz) {
        AxisAlignedBB box = boxStart.getBoundingBox(world, mutable);
        return new TerrainBoxBuilder.MutableTerrainBoxBuilder(ox + box.minX, oy + box.minY, oz + box.minZ, box.maxX - box.minX, box.maxY - box.minY, box.maxZ - box.minZ);
    }

    /**
     * @return True if this {@link IBlockCollisionBehavior} can handle the collisions of this block <br>
     * It should be as restrictive as possible, to let different blocks handled by other collision behaviors <br> <br>
     * <strong>NOTE : For one block state, the result should ALWAYS be the same</strong>
     */
    boolean applies(IBlockAccess world, BlockPos pos, IBlockState toBlock);

    /**
     * Checks if this block can be the base of a stack box
     *
     * @param world      The world
     * @param pos        The pos
     * @param blockState The block being tested
     * @return True to being a stack box of this block, false to add this block separately to the world
     */
    default boolean isStackableBlock(IBlockAccess world, BlockPos pos, IBlockState blockState) {
        return true;
    }

    /**
     * Checks if the collisions of the onBlock can be merged with the collisions of the stackingBlock, on the given axis. <br>
     * It permits to have optimized collisions, because the less boxes there is, the happier the physics engine is. <br>
     * If you want greatly optimized collisions, this can be really complex, so check {@link BlockCollisionBehaviors} for examples.
     *
     * @param world         The world
     * @param pos           The pos
     * @param axis          The axis of stacking (the stacking direction is always the positive direction)
     * @param onBlock       The base of the stack (accepted by the applies method)
     * @param stackingBlock The stacking block (note : at this step, all the blocks between onBlock and stackingBlock are already stacked) (accepted by the applies method)
     * @param lastStacked   The block previously stacked, on the previous axis, to avoid adding a block and a slab on the same plane. Null if not pertinent.
     * @return True if onBlock can be stacked on stackingBlock
     */
    boolean stacks(IBlockAccess world, BlockPos pos, EnumFacing.Axis axis, IBlockState onBlock, IBlockState stackingBlock, @Nullable IBlockState lastStacked);

    /**
     * Stacks the block of the current 'boxBuilder', of if 'ofBlock', adds its collisions boxes to the 'terrainBoxConstructor' <br>
     *
     * @param terrainBoxConstructor The terrain box constructor
     * @param boxBuilder            The current box builder, returned by the initBoxBuilder function of a block collision behavior (not always the same) <br>
     *                              <strong>Null if this block is not stackable</strong>
     * @param cursor
     * @param world                 The world
     * @param at                    The pos
     * @param ofBlock               The block being added to the boxBuilder or terrainBoxConstructor
     * @param axis                  The direction of stacking (always positive) <br>
     */
    void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, @Nullable TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, World world, BlockPos at, IBlockState ofBlock, @Nullable EnumFacing.Axis axis);

    /**
     * Fired when the construction of 'boxBuilder', and no more blocks will be added <br>
     * You can execute some callbacks operations based on the last added blocks (see {@link BlockCollisionBehaviors.Panes} for an example)
     *
     * @param terrainBoxConstructor The terrain box constructor
     * @param boxBuilder            The box just build
     */
    default void onBoxBuildEnd(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder) {
    }
}