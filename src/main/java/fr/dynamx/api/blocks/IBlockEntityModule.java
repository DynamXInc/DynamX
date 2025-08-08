package fr.dynamx.api.blocks;

import fr.dynamx.api.entities.modules.IBaseModule;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Base implementation of a {@link fr.dynamx.common.blocks.TEDynamXBlock} module
 */
public interface IBlockEntityModule extends IBaseModule {
    /**
     * Called when the tile entity was just loaded
     */
    default void initBlockEntityProperties() {
    }

    /**
     * Called when the block is break
     */
    default void onBlockBreak() {
    }

    /**
     * Fills the drops list with the block drops when the block is broken
     */
    default void getBlockDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
    }

    /**
     * Implement this on you module to listen tile entity updates
     */
    interface IBlockEntityUpdateListener {
        /**
         * @return True to listen this update on this side (default is true on all sides)
         */
        default boolean listenBlockEntityUpdates(Side side) {
            return true;
        }

        /**
         * Called when updating the tile entity
         */
        default void updateBlockEntity() {
        }
    }
}
