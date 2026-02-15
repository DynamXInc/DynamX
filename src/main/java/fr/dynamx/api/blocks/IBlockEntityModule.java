package fr.dynamx.api.blocks;

import fr.dynamx.api.entities.modules.IBaseModule;
import fr.dynamx.core.common.blocks.TEDynamXBlock;
import fr.hermes.api.mc.blocks.HmBlockState;
import fr.hermes.api.mc.items.HmItemStack;
import fr.hermes.api.mc.world.HmWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Base implementation of a {@link TEDynamXBlock} module
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
    default void getBlockDrops(List<HmItemStack> drops, HmWorld world, BlockPos pos, HmBlockState state, int fortune) {
    }

    /**
     * Implement this on you module to listen tile entity updates
     */
    interface IBlockEntityUpdateListener {
        /**
         * @return True to listen this update on this side (default is true on all sides)
         */
        default boolean listenBlockEntityUpdates(boolean isClient) {
            return true;
        }

        /**
         * Called when updating the tile entity
         */
        default void updateBlockEntity() {
        }
    }
}
