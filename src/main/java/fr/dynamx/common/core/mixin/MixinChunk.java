package fr.dynamx.common.core.mixin;

import fr.dynamx.common.handlers.CommonEventHandler;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

/**
 * Patches the world raytrace to raytrace on dynamx blocks
 */
@Mixin(value = Chunk.class, priority = 500, remap = DynamXConstants.REMAP)
public abstract class MixinChunk {
    @Final
    @Shadow
    private ExtendedBlockStorage[] storageArrays;
    @Final
    @Shadow
    private int[] precipitationHeightMap;
    @Final
    @Shadow
    private World world;
    @Final
    @Shadow
    private int[] heightMap;
    @Shadow
    private boolean dirty;

    @Shadow
    private void relightBlock(int x, int y, int z) {
        throw new IllegalStateException("MixinChunk failed to shadow relightBlock !");
    }

    @Shadow
    private void propagateSkylightOcclusion(int x, int z) {
        throw new IllegalStateException("MixinChunk failed to shadow propagateSkylightOcclusion !");
    }

    /**
     * @author Aym'
     * @reason DynamX
     */
    @Nullable
    @Overwrite
    public IBlockState setBlockState(BlockPos pos, IBlockState state) {
        Chunk chunk = (Chunk) (Object) this;
        int i = pos.getX() & 15;
        int j = pos.getY();
        int k = pos.getZ() & 15;
        int l = k << 4 | i;

        if (j >= precipitationHeightMap[l] - 1) {
            precipitationHeightMap[l] = -999;
        }

        int i1 = heightMap[l];
        IBlockState iblockstate = chunk.getBlockState(pos);

        if (iblockstate == state) {
            return null;
        } else {
            Block block = state.getBlock();
            Block block1 = iblockstate.getBlock();
            int k1 = iblockstate.getLightOpacity(world, pos); // Relocate old light value lookup here, so that it is called before TE is removed.
            ExtendedBlockStorage extendedblockstorage = storageArrays[j >> 4];
            boolean flag = false;

            if (extendedblockstorage == Chunk.NULL_BLOCK_STORAGE) {
                if (block == Blocks.AIR) {
                    return null;
                }

                extendedblockstorage = new ExtendedBlockStorage(j >> 4 << 4, world.provider.hasSkyLight());
                storageArrays[j >> 4] = extendedblockstorage;
                flag = j >= i1;
            }

            extendedblockstorage.set(i, j & 15, k, state);

            //if (block1 != block)
            {
                if (!world.isRemote) {
                    if (block1 != block) //Only fire block breaks when the block changes.
                        block1.breakBlock(world, pos, iblockstate);
                    TileEntity te = chunk.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
                    if (te != null && te.shouldRefresh(world, pos, iblockstate, state)) world.removeTileEntity(pos);
                } else if (block1.hasTileEntity(iblockstate)) {
                    TileEntity te = chunk.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
                    if (te != null && te.shouldRefresh(world, pos, iblockstate, state))
                        world.removeTileEntity(pos);
                }
            }

            if (extendedblockstorage.get(i, j & 15, k).getBlock() != block) {
                return null;
            } else {
                if (flag) {
                    chunk.generateSkylightMap();
                } else {
                    int j1 = state.getLightOpacity(world, pos);

                    if (j1 > 0) {
                        if (j >= i1) {
                            relightBlock(i, j + 1, k);
                        }
                    } else if (j == i1 - 1) {
                        relightBlock(i, j, k);
                    }

                    if (j1 != k1 && (j1 < k1 || chunk.getLightFor(EnumSkyBlock.SKY, pos) > 0 || chunk.getLightFor(EnumSkyBlock.BLOCK, pos) > 0)) {
                        propagateSkylightOcclusion(i, k);
                    }
                }

                // If capturing blocks, only run block physics for TE's. Non-TE's are handled in ForgeHooks.onPlaceItemIntoWorld
                if (!world.isRemote && block1 != block && (!world.captureBlockSnapshots || block.hasTileEntity(state))) {
                    block.onBlockAdded(world, pos, state);
                }

                if (block.hasTileEntity(state)) {
                    TileEntity tileentity1 = chunk.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);

                    if (tileentity1 == null) {
                        tileentity1 = block.createTileEntity(world, state);
                        world.setTileEntity(pos, tileentity1);
                    }

                    if (tileentity1 != null) {
                        tileentity1.updateContainingBlockInfo();
                    }
                }
                CommonEventHandler.onBlockChange(world, pos, iblockstate, state);
                dirty = true;
                return iblockstate;
            }
        }
    }
}
