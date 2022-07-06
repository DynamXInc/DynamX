package fr.dynamx.common.core;

import fr.dynamx.common.DynamXContext;
import fr.dynamx.utils.DynamXReflection;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.List;

import static fr.dynamx.utils.DynamXReflection.*;

/**
 * Patches vanilla movement to collide with vehicles and DynamX blocks
 */
public class AABBCollisionHandler
{
    public static void vanillaMove(Entity entity, MoverType type, double x, double y, double z)
    {
        if (entity.noClip)
        {
            entity.setEntityBoundingBox(entity.getEntityBoundingBox().offset(x, y, z));
            entity.resetPositionToBB();
        }
        else
        {
            if (type == MoverType.PISTON)
            {
                long i = entity.world.getTotalWorldTime();

                if (i != entity.pistonDeltasGameTime)
                {
                    Arrays.fill(entity.pistonDeltas, 0.0D);
                    entity.pistonDeltasGameTime = i;
                }

                if (x != 0.0D)
                {
                    int j = EnumFacing.Axis.X.ordinal();
                    double d0 = MathHelper.clamp(x + entity.pistonDeltas[j], -0.51D, 0.51D);
                    x = d0 - entity.pistonDeltas[j];
                    entity.pistonDeltas[j] = d0;

                    if (Math.abs(x) <= 9.999999747378752E-6D)
                    {
                        return;
                    }
                }
                else if (y != 0.0D)
                {
                    int l4 = EnumFacing.Axis.Y.ordinal();
                    double d12 = MathHelper.clamp(y + entity.pistonDeltas[l4], -0.51D, 0.51D);
                    y = d12 - entity.pistonDeltas[l4];
                    entity.pistonDeltas[l4] = d12;

                    if (Math.abs(y) <= 9.999999747378752E-6D)
                    {
                        return;
                    }
                }
                else
                {
                    if (z == 0.0D)
                    {
                        return;
                    }

                    int i5 = EnumFacing.Axis.Z.ordinal();
                    double d13 = MathHelper.clamp(z + entity.pistonDeltas[i5], -0.51D, 0.51D);
                    z = d13 - entity.pistonDeltas[i5];
                    entity.pistonDeltas[i5] = d13;

                    if (Math.abs(z) <= 9.999999747378752E-6D)
                    {
                        return;
                    }
                }
            }

            double preX = x, preY = y, preZ = z;
            entity.world.profiler.startSection("move");
            double d10 = entity.posX;
            double d11 = entity.posY;
            double d1 = entity.posZ;

            if (entity.isInWeb)
            {
                entity.isInWeb = false;
                x *= 0.25D;
                y *= 0.05000000074505806D;
                z *= 0.25D;
                entity.motionX = 0.0D;
                entity.motionY = 0.0D;
                entity.motionZ = 0.0D;
            }

            double d2 = x;
            double d3 = y;
            double d4 = z;

            //SNEAKING
            if ((type == MoverType.SELF || type == MoverType.PLAYER) && entity.onGround && entity.isSneaking() && entity instanceof EntityPlayer)
            {
                for (double d5 = 0.05D; x != 0.0D && entity.world.getCollisionBoxes(entity, entity.getEntityBoundingBox().offset(x, (double)(-entity.stepHeight), 0.0D)).isEmpty(); d2 = x)
                {
                    if (x < 0.05D && x >= -0.05D)
                    {
                        x = 0.0D;
                    }
                    else if (x > 0.0D)
                    {
                        x -= 0.05D;
                    }
                    else
                    {
                        x += 0.05D;
                    }
                }

                for (; z != 0.0D && entity.world.getCollisionBoxes(entity, entity.getEntityBoundingBox().offset(0.0D, (double)(-entity.stepHeight), z)).isEmpty(); d4 = z)
                {
                    if (z < 0.05D && z >= -0.05D)
                    {
                        z = 0.0D;
                    }
                    else if (z > 0.0D)
                    {
                        z -= 0.05D;
                    }
                    else
                    {
                        z += 0.05D;
                    }
                }

                for (; x != 0.0D && z != 0.0D && entity.world.getCollisionBoxes(entity, entity.getEntityBoundingBox().offset(x, (double)(-entity.stepHeight), z)).isEmpty(); d4 = z)
                {
                    if (x < 0.05D && x >= -0.05D)
                    {
                        x = 0.0D;
                    }
                    else if (x > 0.0D)
                    {
                        x -= 0.05D;
                    }
                    else
                    {
                        x += 0.05D;
                    }

                    d2 = x;

                    if (z < 0.05D && z >= -0.05D)
                    {
                        z = 0.0D;
                    }
                    else if (z > 0.0D)
                    {
                        z -= 0.05D;
                    }
                    else
                    {
                        z += 0.05D;
                    }
                }
            }

            AxisAlignedBB axisalignedbb = entity.getEntityBoundingBox();
            Vector3fPool.openPool();
            Profiler.get().start(Profiler.Profiles.ENTITY_COLLISION);
            double[] data = DynamXContext.getCollisionHandler().handleCollisionWithBulletEntities(entity, x, y, z);
            Profiler.get().end(Profiler.Profiles.ENTITY_COLLISION);
            Vector3fPool.closePool();
            x = data[0];
            y = data[1];
            z = data[2];
            //if(entity instanceof EntityPlayer && (Math.abs(x) > 0.01f || Math.abs(z) > 0.01f || Math.abs(preY-y) > 0.01f))
              //  System.out.println(RotatedCollisionHandler.motionChanged+" Calculated motion "+(preX-x)+" "+(preY-y)+" "+(preZ-z));
            entity.setEntityBoundingBox(axisalignedbb.offset(x, y, z));

            boolean flag = entity.onGround || d3 != y && d3 < 0.0D;

            if(!DynamXContext.getCollisionHandler().motionHasChanged()) {
                if (entity.stepHeight > 0.0F && flag && (d2 != x || d4 != z)) {
                    double d14 = x;
                    double d6 = y;
                    double d7 = z;
                    AxisAlignedBB axisalignedbb1 = entity.getEntityBoundingBox();
                    entity.setEntityBoundingBox(axisalignedbb);
                    y = entity.stepHeight;

                    List<AxisAlignedBB> list = entity.world.getCollisionBoxes(entity, entity.getEntityBoundingBox().expand(d2, y, d4));
                    AxisAlignedBB axisalignedbb2 = entity.getEntityBoundingBox();
                    AxisAlignedBB axisalignedbb3 = axisalignedbb2.expand(d2, 0.0D, d4);
                    double d8 = y;
                    int j1 = 0;

                    for (int k1 = list.size(); j1 < k1; ++j1) {
                        d8 = list.get(j1).calculateYOffset(axisalignedbb3, d8);
                    }

                    axisalignedbb2 = axisalignedbb2.offset(0.0D, d8, 0.0D);
                    double d18 = d2;
                    int l1 = 0;

                    for (int i2 = list.size(); l1 < i2; ++l1) {
                        d18 = list.get(l1).calculateXOffset(axisalignedbb2, d18);
                    }

                    axisalignedbb2 = axisalignedbb2.offset(d18, 0.0D, 0.0D);
                    double d19 = d4;
                    int j2 = 0;

                    for (int k2 = list.size(); j2 < k2; ++j2) {
                        d19 = list.get(j2).calculateZOffset(axisalignedbb2, d19);
                    }

                    axisalignedbb2 = axisalignedbb2.offset(0.0D, 0.0D, d19);
                    AxisAlignedBB axisalignedbb4 = entity.getEntityBoundingBox();
                    double d20 = y;
                    int l2 = 0;

                    for (int i3 = list.size(); l2 < i3; ++l2) {
                        d20 = list.get(l2).calculateYOffset(axisalignedbb4, d20);
                    }

                    axisalignedbb4 = axisalignedbb4.offset(0.0D, d20, 0.0D);
                    double d21 = d2;
                    int j3 = 0;

                    for (int k3 = list.size(); j3 < k3; ++j3) {
                        d21 = list.get(j3).calculateXOffset(axisalignedbb4, d21);
                    }

                    axisalignedbb4 = axisalignedbb4.offset(d21, 0.0D, 0.0D);
                    double d22 = d4;
                    int l3 = 0;

                    for (int i4 = list.size(); l3 < i4; ++l3) {
                        d22 = list.get(l3).calculateZOffset(axisalignedbb4, d22);
                    }

                    axisalignedbb4 = axisalignedbb4.offset(0.0D, 0.0D, d22);
                    double d23 = d18 * d18 + d19 * d19;
                    double d9 = d21 * d21 + d22 * d22;

                    if (d23 > d9) {
                        x = d18;
                        z = d19;
                        y = -d8;
                        entity.setEntityBoundingBox(axisalignedbb2);
                    } else {
                        x = d21;
                        z = d22;
                        y = -d20;
                        entity.setEntityBoundingBox(axisalignedbb4);
                    }

                    int j4 = 0;

                    for (int k4 = list.size(); j4 < k4; ++j4) {
                        y = list.get(j4).calculateYOffset(entity.getEntityBoundingBox(), y);
                    }

                    entity.setEntityBoundingBox(entity.getEntityBoundingBox().offset(0.0D, y, 0.0D));

                    if (d14 * d14 + d7 * d7 >= x * x + z * z) {
                        x = d14;
                        y = d6;
                        z = d7;
                        entity.setEntityBoundingBox(axisalignedbb1);
                    }
                }
            }

            entity.world.profiler.endSection();
            entity.world.profiler.startSection("rest");
            entity.resetPositionToBB();
            entity.collidedHorizontally = d2 != x || d4 != z;
            entity.collidedVertically = d3 != y;
            entity.onGround = entity.collidedVertically && d3 < 0.0D;
            entity.collided = entity.collidedHorizontally || entity.collidedVertically;
            //System.out.println("Output real "+x+" "+y+" "+z+" "+entity.onGround+" "+entity.collidedVertically);
            int j6 = MathHelper.floor(entity.posX);
            int i1 = MathHelper.floor(entity.posY - 0.20000000298023224D);
            int k6 = MathHelper.floor(entity.posZ);
            BlockPos blockpos = new BlockPos(j6, i1, k6);
            IBlockState iblockstate = entity.world.getBlockState(blockpos);

            if (iblockstate.getMaterial() == Material.AIR)
            {
                BlockPos blockpos1 = blockpos.down();
                IBlockState iblockstate1 = entity.world.getBlockState(blockpos1);
                Block block1 = iblockstate1.getBlock();

                if (block1 instanceof BlockFence || block1 instanceof BlockWall || block1 instanceof BlockFenceGate)
                {
                    iblockstate = iblockstate1;
                    blockpos = blockpos1;
                }
            }

            invokeMethod(DynamXReflection.updateFallState, entity,  y, entity.onGround, iblockstate, blockpos);

            if (d2 != x)
            {
                entity.motionX = 0.0D;
            }

            if (d4 != z)
            {
                entity.motionZ = 0.0D;
            }

            Block block = iblockstate.getBlock();

            if (d3 != y)
            {
                block.onLanded(entity.world, entity);
            }

            if ((boolean)invokeMethod(canTriggerWalking, entity) && (!entity.onGround || !entity.isSneaking() || !(entity instanceof EntityPlayer)) && !entity.isRiding())
            {
                double d15 = entity.posX - d10;
                double d16 = entity.posY - d11;
                double d17 = entity.posZ - d1;

                if (block != Blocks.LADDER)
                {
                    d16 = 0.0D;
                }

                if (block != null && entity.onGround)
                {
                    block.onEntityWalk(entity.world, blockpos, entity);
                }

                entity.distanceWalkedModified = (float)((double)entity.distanceWalkedModified + (double)MathHelper.sqrt(d15 * d15 + d17 * d17) * 0.6D);
                entity.distanceWalkedOnStepModified = (float)((double)entity.distanceWalkedOnStepModified + (double)MathHelper.sqrt(d15 * d15 + d16 * d16 + d17 * d17) * 0.6D);

                if (entity.distanceWalkedOnStepModified > (float)entity.nextStepDistance && iblockstate.getMaterial() != Material.AIR)
                {
                    entity.nextStepDistance = (int)entity.distanceWalkedOnStepModified + 1;

                    if (entity.isInWater())
                    {
                        Entity passenger = entity.isBeingRidden() && entity.getControllingPassenger() != null ? entity.getControllingPassenger() : entity;
                        float f = passenger == entity ? 0.35F : 0.4F;
                        float f1 = MathHelper.sqrt(passenger.motionX * passenger.motionX * 0.20000000298023224D + passenger.motionY * passenger.motionY + passenger.motionZ * passenger.motionZ * 0.20000000298023224D) * f;

                        if (f1 > 1.0F)
                        {
                            f1 = 1.0F;
                        }

                        entity.playSound(SoundEvents.ENTITY_GENERIC_SWIM, f1, 1.0F + (entity.rand.nextFloat() - entity.rand.nextFloat()) * 0.4F);
                    }
                    else
                    {
                        invokeMethod(playStepSound, entity, blockpos, block);
                    }
                }
                else if (entity.distanceWalkedOnStepModified > entity.nextFlap && (boolean)invokeMethod(makeFlySound, entity) && iblockstate.getMaterial() == Material.AIR)
                {
                    entity.nextFlap = (float) invokeMethod(playFlySound,entity,entity.distanceWalkedOnStepModified);
                }
            }

            try
            {
                invokeMethod(doBlockCollisions, entity);
            }
            catch (Throwable throwable)
            {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Checking entity block collision");
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being checked for collision");
                entity.addEntityCrashInfo(crashreportcategory);
                throw new ReportedException(crashreport);
            }

            boolean flag1 = entity.isWet();

            if (entity.world.isFlammableWithin(entity.getEntityBoundingBox().shrink(0.001D)))
            {
                invokeMethod(DynamXReflection.dealFireDamage, entity, 1);

                if (!flag1)
                {
                    ++entity.fire;

                    if (entity.fire == 0)
                    {
                        entity.setFire(8);
                    }
                }
            }
            else if (entity.fire <= 0)
            {
                entity.fire = -1;
            }

            if (flag1 && entity.isBurning())
            {
                entity.playSound(SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.7F, 1.6F + (entity.rand.nextFloat() - entity.rand.nextFloat()) * 0.4F);
                entity.fire = -1;
            }

            entity.world.profiler.endSection();
        }
    }
}