package fr.dynamx.common.core.mixin;

import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.capability.DynamXChunkData;
import fr.dynamx.common.capability.DynamXChunkDataProvider;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;
import net.minecraft.world.IWorldEventListener;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Patches the world raytrace to raytrace on dynamx blocks
 */
@Mixin(World.class)
public abstract class MixinWorld {
    @Shadow
    public abstract void removeEventListener(IWorldEventListener listener);

    @Shadow
    public abstract World init();

    /**
     * Updates the terrain when the world is modified
     *
     * @author Aym'
     */
    /*@Overwrite
    public boolean setBlockState(BlockPos pos, IBlockState newState, int flags)
    {
        World world = (World) (Object) this;
        if (world.isOutsideBuildHeight(pos))
        {
            return false;
        }
        else if (!world.isRemote && world.getWorldInfo().getTerrainType() == WorldType.DEBUG_ALL_BLOCK_STATES)
        {
            return false;
        }
        else
        {
            Chunk chunk = this.getChunk(pos);

            pos = pos.toImmutable(); // Forge - prevent mutable BlockPos leaks
            net.minecraftforge.common.util.BlockSnapshot blockSnapshot = null;
            if (world.captureBlockSnapshots && !world.isRemote)
            {
                blockSnapshot = net.minecraftforge.common.util.BlockSnapshot.getBlockSnapshot(world, pos, flags);
                world.capturedBlockSnapshots.add(blockSnapshot);
            }
            IBlockState oldState = getBlockState(pos);
            int oldLight = oldState.getLightValue(world, pos);
            int oldOpacity = oldState.getLightOpacity(world, pos);

            IBlockState iblockstate = chunk.setBlockState(pos, newState);

            if (iblockstate == null)
            {
                if (blockSnapshot != null) world.capturedBlockSnapshots.remove(blockSnapshot);
                return false;
            }
            else
            {
                if (newState.getLightOpacity(world, pos) != oldOpacity || newState.getLightValue(world, pos) != oldLight)
                {
                    world.profiler.startSection("checkLight");
                    world.checkLight(pos);
                    world.profiler.endSection();
                }

                if (blockSnapshot == null) // Don't notify clients or update physics while capturing blockstates
                {
                    world.markAndNotifyBlock(pos, chunk, iblockstate, newState, flags);
                }


                return true;
            }
        }
    }
   /* @Inject(at = @At("RETURN"), target = @Desc(value = "setBlockState", ret = boolean.class, args = {BlockPos.class, IBlockState.class, int.class}))
    public void setBlockState(BlockPos pos, IBlockState newState, int flags, CallbackInfoReturnable<Boolean> info) {
        if(flags != 4) {
            System.out.println(info.getReturnValue() + " tut " + info.getReturnValueZ());
            if (info.getReturnValueZ()) {
                System.out.println("SET BLOCK CALLBACK " + pos + " " + flags + " " + (newState.getBlock().getTickRandomly()));
                if (flags != 4 && DynamXContext.usesPhysicsWorld((World) (Object) this)) { // 4 is random leaves update
                    System.out.println("Processing "+newState);
                    CommonEventHandler.onBlockChange((World) (Object) this, pos);
                }
            } else {
                DynamXMain.log.error("MixinWorld failed to inject at the right return !");
                //System.out.println("Got but returning false :c");
            }
        }
    }*/

    /**
     * @author Yanis
     */
    @Nullable
    @Overwrite
    public RayTraceResult rayTraceBlocks(Vec3d vec31, Vec3d vec32, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock) {
        if (!Double.isNaN(vec31.x) && !Double.isNaN(vec31.y) && !Double.isNaN(vec31.z)) {
            if (!Double.isNaN(vec32.x) && !Double.isNaN(vec32.y) && !Double.isNaN(vec32.z)) {
                int x = MathHelper.floor(vec32.x);
                int y = MathHelper.floor(vec32.y);
                int z = MathHelper.floor(vec32.z);
                int x1 = MathHelper.floor(vec31.x);
                int y1 = MathHelper.floor(vec31.y);
                int z1 = MathHelper.floor(vec31.z);
                AtomicReference<BlockPos> blockpos = new AtomicReference<>(new BlockPos(x1, y1, z1));
                //System.out.println("First " +blockpos);
                IBlockState iblockstate = this.getBlockState(blockpos.get());
                Block block = iblockstate.getBlock();
                //System.out.println("First " +block);

                if ((!ignoreBlockWithoutBoundingBox || iblockstate.getCollisionBoundingBox((World) (Object) this, blockpos.get()) != Block.NULL_AABB) && block.canCollideCheck(iblockstate, stopOnLiquid)) {
                    RayTraceResult raytraceresult = iblockstate.collisionRayTrace((World) (Object) this, blockpos.get(), vec31, vec32);
                    if (raytraceresult != null) {
                        return raytraceresult;
                    }
                }

                RayTraceResult raytraceresult2 = null;
                AtomicReference<RayTraceResult> rayResult = new AtomicReference<>(null);
                int k1 = 200;

                while (k1-- >= 0) {
                    if (Double.isNaN(vec31.x) || Double.isNaN(vec31.y) || Double.isNaN(vec31.z)) {
                        return null;
                    }

                    if (x1 == x && y1 == y && z1 == z) {
                        return returnLastUncollidableBlock ? raytraceresult2 : null;
                    }

                    boolean flag2 = true;
                    boolean flag = true;
                    boolean flag1 = true;
                    double d0 = 999.0D;
                    double d1 = 999.0D;
                    double d2 = 999.0D;

                    if (x > x1) {
                        d0 = (double) x1 + 1.0D;
                    } else if (x < x1) {
                        d0 = (double) x1 + 0.0D;
                    } else {
                        flag2 = false;
                    }

                    if (y > y1) {
                        d1 = (double) y1 + 1.0D;
                    } else if (y < y1) {
                        d1 = (double) y1 + 0.0D;
                    } else {
                        flag = false;
                    }

                    if (z > z1) {
                        d2 = (double) z1 + 1.0D;
                    } else if (z < z1) {
                        d2 = (double) z1 + 0.0D;
                    } else {
                        flag1 = false;
                    }

                    double d3 = 999.0D;
                    double d4 = 999.0D;
                    double d5 = 999.0D;
                    double d6 = vec32.x - vec31.x;
                    double d7 = vec32.y - vec31.y;
                    double d8 = vec32.z - vec31.z;

                    if (flag2) {
                        d3 = (d0 - vec31.x) / d6;
                    }

                    if (flag) {
                        d4 = (d1 - vec31.y) / d7;
                    }

                    if (flag1) {
                        d5 = (d2 - vec31.z) / d8;
                    }

                    if (d3 == -0.0D) {
                        d3 = -1.0E-4D;
                    }

                    if (d4 == -0.0D) {
                        d4 = -1.0E-4D;
                    }

                    if (d5 == -0.0D) {
                        d5 = -1.0E-4D;
                    }

                    EnumFacing enumfacing;

                    if (d3 < d4 && d3 < d5) {
                        enumfacing = x > x1 ? EnumFacing.WEST : EnumFacing.EAST;
                        vec31 = new Vec3d(d0, vec31.y + d7 * d3, vec31.z + d8 * d3);
                    } else if (d4 < d5) {
                        enumfacing = y > y1 ? EnumFacing.DOWN : EnumFacing.UP;
                        vec31 = new Vec3d(vec31.x + d6 * d4, d1, vec31.z + d8 * d4);
                    } else {
                        enumfacing = z > z1 ? EnumFacing.NORTH : EnumFacing.SOUTH;
                        vec31 = new Vec3d(vec31.x + d6 * d5, vec31.y + d7 * d5, d2);
                    }

                    x1 = MathHelper.floor(vec31.x) - (enumfacing == EnumFacing.EAST ? 1 : 0);
                    y1 = MathHelper.floor(vec31.y) - (enumfacing == EnumFacing.UP ? 1 : 0);
                    z1 = MathHelper.floor(vec31.z) - (enumfacing == EnumFacing.SOUTH ? 1 : 0);
                    Vec3d vec3d = new Vec3d(x1, y1, z1);
                    blockpos.set(new BlockPos(vec3d));

                    Chunk chunk = getChunk(blockpos.get());
                    DynamXChunkData capability = chunk.getCapability(DynamXChunkDataProvider.DYNAM_X_CHUNK_DATA_CAPABILITY, null);

                    rayResult.set(null); //Ray-trace DynamX blocks
                    for (Map.Entry<BlockPos, AxisAlignedBB> e : capability.getBlocksAABB().entrySet()) {
                        RayTraceResult res = e.getValue().calculateIntercept(vec31, vec32);
                        if (res != null) {
                            rayResult.set(res);
                            blockpos.set(e.getKey());
                            break;
                        }
                    }

                    IBlockState iblockstate1 = this.getBlockState(blockpos.get());
                    Block block1 = iblockstate1.getBlock();
                    if (rayResult.get() != null && !(block1 instanceof DynamXBlock)) {
                        capability.getBlocksAABB().remove(blockpos.get());
                    }
                    if (!ignoreBlockWithoutBoundingBox || iblockstate1.getMaterial() == Material.PORTAL || iblockstate1.getCollisionBoundingBox((World) (Object) this, blockpos.get()) != Block.NULL_AABB) {
                        if (block1.canCollideCheck(iblockstate1, stopOnLiquid)) {
                            if (rayResult.get() == null) { //Don't re-raytrace DynamX blocks
                                rayResult.set(iblockstate1.collisionRayTrace((World) (Object) this, blockpos.get(), vec31, vec32));
                                if (rayResult.get() != null) {
                                    return rayResult.get();
                                }
                            } else {
                                return new RayTraceResult(rayResult.get().hitVec.add(blockpos.get().getX(), blockpos.get().getY(), blockpos.get().getZ()), rayResult.get().sideHit, blockpos.get());
                            }
                        } else {
                            raytraceresult2 = new RayTraceResult(RayTraceResult.Type.MISS, vec31, enumfacing, blockpos.get());
                        }
                    }
                }

                return returnLastUncollidableBlock ? raytraceresult2 : null;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Shadow
    public IBlockState getBlockState(BlockPos pos) {
        throw new IllegalStateException("Mixin failed to shadow getBlockState()");
    }

    @Shadow
    public Chunk getChunk(BlockPos pos) {
        throw new IllegalStateException("Mixin failed to shadow getChunk()");
    }
}
