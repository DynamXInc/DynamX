package fr.dynamx.common.core.mixin;

import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.capability.DynamXChunkData;
import fr.dynamx.common.capability.DynamXChunkDataProvider;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Patches the world raytrace to raytrace on dynamx blocks
 */
@Mixin(value = World.class, remap = DynamXConstants.REMAP)
public abstract class MixinWorld {
    @Shadow
    public abstract World init();

    /**
     * @author Yanis
     * @reason
     */
    @Nullable
    @Overwrite
    public RayTraceResult rayTraceBlocks(Vec3d vec31, Vec3d vec32, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock) {
        if (!Double.isNaN(vec31.x) && !Double.isNaN(vec31.y) && !Double.isNaN(vec31.z)) {
            if (!Double.isNaN(vec32.x) && !Double.isNaN(vec32.y) && !Double.isNaN(vec32.z)) {
                Vec3d rayTraceStart = new Vec3d(vec31.x, vec31.y, vec31.z);
                int x = MathHelper.floor(vec32.x);
                int y = MathHelper.floor(vec32.y);
                int z = MathHelper.floor(vec32.z);
                int x1 = MathHelper.floor(vec31.x);
                int y1 = MathHelper.floor(vec31.y);
                int z1 = MathHelper.floor(vec31.z);
                BlockPos blockpos = new BlockPos(x1, y1, z1);
                IBlockState iblockstate = this.getBlockState(blockpos);
                Block block = iblockstate.getBlock();

                if ((!ignoreBlockWithoutBoundingBox || iblockstate.getCollisionBoundingBox((World) (Object) this, blockpos) != Block.NULL_AABB) && block.canCollideCheck(iblockstate, stopOnLiquid)) {
                    RayTraceResult raytraceresult = iblockstate.collisionRayTrace((World) (Object) this, blockpos, vec31, vec32);
                    if (raytraceresult != null) {
                        return raytraceresult;
                    }
                }

                RayTraceResult raytraceresult2 = null;
                int k1 = 200;

                RayTraceResult dynamXHit = null;
                BlockPos dynamXHitPos = null;
                while (k1-- >= 0) {
                    if (Double.isNaN(vec31.x) || Double.isNaN(vec31.y) || Double.isNaN(vec31.z)) {
                        return null;
                    }

                    if (x1 == x && y1 == y && z1 == z) {
                        if(returnLastUncollidableBlock && raytraceresult2 != null)
                            return getCloserHit(rayTraceStart, raytraceresult2, dynamXHit, dynamXHitPos);
                        return dynamXHit != null ? new RayTraceResult(dynamXHit.hitVec.add(dynamXHitPos.getX(), dynamXHitPos.getY(), dynamXHitPos.getZ()), dynamXHit.sideHit, dynamXHitPos) : null;
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
                    blockpos = new BlockPos(x1, y1, z1);

                    //Ray-trace DynamX blocks
                    Chunk chunk = getChunk(blockpos);
                    DynamXChunkData capability = chunk.getCapability(DynamXChunkDataProvider.DYNAM_X_CHUNK_DATA_CAPABILITY, null);
                    if(dynamXHit == null) {
                        for (Map.Entry<BlockPos, AxisAlignedBB> e : capability.getBlocksAABB().entrySet()) {
                            RayTraceResult res = e.getValue().calculateIntercept(vec31, vec32);
                            if (res != null) {
                                dynamXHit = res;
                                dynamXHitPos = e.getKey();
                                break;
                            }
                        }
                        //Remove outdated collision boxes
                        if(dynamXHitPos != null) {
                            IBlockState iblockstate1 = this.getBlockState(dynamXHitPos);
                            Block block1 = iblockstate1.getBlock();
                            if (!(block1 instanceof DynamXBlock)) {
                                capability.getBlocksAABB().remove(dynamXHitPos);
                                dynamXHit = null;
                            }
                        }
                    }

                    IBlockState iblockstate1 = this.getBlockState(blockpos);
                    Block block1 = iblockstate1.getBlock();
                    if (!ignoreBlockWithoutBoundingBox || iblockstate1.getMaterial() == Material.PORTAL || iblockstate1.getCollisionBoundingBox((World) (Object) this, blockpos) != Block.NULL_AABB) {
                        if (block1.canCollideCheck(iblockstate1, stopOnLiquid)) {
                            RayTraceResult rayResult = iblockstate1.collisionRayTrace((World) (Object) this, blockpos, vec31, vec32);
                            if(rayResult != null)
                                return getCloserHit(rayTraceStart, rayResult, dynamXHit, dynamXHitPos);
                        } else {
                            raytraceresult2 = new RayTraceResult(RayTraceResult.Type.MISS, vec31, enumfacing, blockpos);
                        }
                    }
                }
                if(returnLastUncollidableBlock && raytraceresult2 != null)
                    return getCloserHit(rayTraceStart, raytraceresult2, dynamXHit, dynamXHitPos);
                return dynamXHit != null ? new RayTraceResult(dynamXHit.hitVec.add(dynamXHitPos.getX(), dynamXHitPos.getY(), dynamXHitPos.getZ()), dynamXHit.sideHit, dynamXHitPos) : null;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private RayTraceResult getCloserHit(Vec3d rayTraceStart, RayTraceResult rayTraceResult, RayTraceResult dynamXHit, BlockPos dynamXHitPos) {
        // If there is a DynamXhit, but Minecraft hit is closer
        if(dynamXHit == null || rayTraceResult.hitVec.subtract(rayTraceStart).lengthSquared() < dynamXHit.hitVec.subtract(rayTraceStart).lengthSquared())
            return rayTraceResult;
        else //Mc's hit is farther, so DynamX hit is closer :)
            return new RayTraceResult(dynamXHit.hitVec.add(dynamXHitPos.getX(), dynamXHitPos.getY(), dynamXHitPos.getZ()), dynamXHit.sideHit, dynamXHitPos);
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
