package fr.dynamx.common.core;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.common.contentpack.parts.BasePartSeat;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * Teleports the player to a valid position near the door when dismounting from a vehicle
 */
public class DismountHelper {
    public static void preDismount(EntityLivingBase dismounter, Entity entityIn) {
        if (entityIn instanceof IModuleContainer.ISeatsContainer) {
            IModuleContainer.ISeatsContainer vehicleEntity = (IModuleContainer.ISeatsContainer) entityIn;
            BasePartSeat seat = vehicleEntity.getSeats().getLastRiddenSeat();
            if (seat != null) {
                Vector3fPool.openPool();
                Vector3f dismountPosition = //PhysicsHelper.getRotatedPoint(seat.position.add(new Vector3f(seat.position.x > 0 ? 1 : -1, 0, 0)),
                        //vehicleEntity.rotationPitch, vehicleEntity.rotationYaw, vehicleEntity.rotationRoll)
                        DynamXGeometry.rotateVectorByQuaternion(seat.getPosition().add(Vector3fPool.get(seat.getPosition().x > 0 ? 1 : -1, 0, 0)), vehicleEntity.cast().physicsRotation)
                                .addLocal(vehicleEntity.cast().physicsPosition);

                AxisAlignedBB collisionDetectionBox = new AxisAlignedBB(dismountPosition.x, dismountPosition.y + 1, dismountPosition.z, dismountPosition.x + 1, dismountPosition.y + 2, dismountPosition.z + 1);
                if (!dismounter.world.collidesWithAnyBlock(collisionDetectionBox)) {
                    dismounter.setPositionAndUpdate(dismountPosition.x, collisionDetectionBox.minY, dismountPosition.z);
                } else {
                    dismountPosition = //PhysicsHelper.getRotatedPoint(seat.position.add(new Vector3f(seat.position.x > 0 ? -2 : 2, 0, 0))
                            //, vehicleEntity.rotationPitch, vehicleEntity.rotationYaw, vehicleEntity.rotationRoll)
                            DynamXGeometry.rotateVectorByQuaternion(seat.getPosition().add(Vector3fPool.get(seat.getPosition().x > 0 ? -2 : 2, 0, 0)), vehicleEntity.cast().physicsRotation)
                                    .addLocal(vehicleEntity.cast().physicsPosition);
                    collisionDetectionBox = new AxisAlignedBB(dismountPosition.x, dismountPosition.y + 1, dismountPosition.z, dismountPosition.x + 1, dismountPosition.y + 2, dismountPosition.z + 1);
                    dismounter.setPositionAndUpdate(dismountPosition.x, collisionDetectionBox.minY, dismountPosition.z);
                }
                Vector3fPool.closePool();
            }
        } else if (!(entityIn instanceof EntityBoat) && !(entityIn instanceof AbstractHorse)) {
            double d1 = entityIn.posX;
            double d13 = entityIn.getEntityBoundingBox().minY + (double) entityIn.height;
            double d14 = entityIn.posZ;
            EnumFacing enumfacing1 = entityIn.getAdjustedHorizontalFacing();

            if (enumfacing1 != null) {
                EnumFacing enumfacing = enumfacing1.rotateY();
                int[][] aint1 = new int[][]{{0, 1}, {0, -1}, {-1, 1}, {-1, -1}, {1, 1}, {1, -1}, {-1, 0}, {1, 0}, {0, 1}};
                double d5 = Math.floor(dismounter.posX) + 0.5D;
                double d6 = Math.floor(dismounter.posZ) + 0.5D;
                double d7 = dismounter.getEntityBoundingBox().maxX - dismounter.getEntityBoundingBox().minX;
                double d8 = dismounter.getEntityBoundingBox().maxZ - dismounter.getEntityBoundingBox().minZ;
                AxisAlignedBB axisalignedbb = new AxisAlignedBB(d5 - d7 / 2.0D, entityIn.getEntityBoundingBox().minY, d6 - d8 / 2.0D, d5 + d7 / 2.0D, Math.floor(entityIn.getEntityBoundingBox().minY) + (double) dismounter.height, d6 + d8 / 2.0D);

                for (int[] aint : aint1) {
                    double d9 = enumfacing1.getXOffset() * aint[0] + enumfacing.getXOffset() * aint[1];
                    double d10 = enumfacing1.getZOffset() * aint[0] + enumfacing.getZOffset() * aint[1];
                    double d11 = d5 + d9;
                    double d12 = d6 + d10;
                    AxisAlignedBB axisalignedbb1 = axisalignedbb.offset(d9, 0.0D, d10);

                    if (!dismounter.world.collidesWithAnyBlock(axisalignedbb1)) {
                        if (dismounter.world.getBlockState(new BlockPos(d11, dismounter.posY, d12)).isSideSolid(dismounter.world, new BlockPos(d11, dismounter.posY, d12), EnumFacing.UP)) {
                            dismounter.setPositionAndUpdate(d11, dismounter.posY + 1.0D, d12);
                            return;
                        }

                        BlockPos blockpos = new BlockPos(d11, dismounter.posY - 1.0D, d12);

                        if (dismounter.world.getBlockState(blockpos).isSideSolid(dismounter.world, blockpos, EnumFacing.UP) || dismounter.world.getBlockState(blockpos).getMaterial() == Material.WATER) {
                            d1 = d11;
                            d13 = dismounter.posY + 1.0D;
                            d14 = d12;
                        }
                    } else if (!dismounter.world.collidesWithAnyBlock(axisalignedbb1.offset(0.0D, 1.0D, 0.0D)) && dismounter.world.getBlockState(new BlockPos(d11, dismounter.posY + 1.0D, d12)).isSideSolid(dismounter.world, new BlockPos(d11, dismounter.posY + 1.0D, d12), EnumFacing.UP)) {
                        d1 = d11;
                        d13 = dismounter.posY + 2.0D;
                        d14 = d12;
                    }
                }
            }

            dismounter.setPositionAndUpdate(d1, d13, d14);
        } else {
            double d0 = (double) (dismounter.width / 2.0F + entityIn.width / 2.0F) + 0.4D;
            float f;

            if (entityIn instanceof EntityBoat) {
                f = 0.0F;
            } else {
                f = ((float) Math.PI / 2F) * (float) (dismounter.getPrimaryHand() == EnumHandSide.RIGHT ? -1 : 1);
            }

            float f1 = -MathHelper.sin(-dismounter.rotationYaw * 0.017453292F - (float) Math.PI + f);
            float f2 = -MathHelper.cos(-dismounter.rotationYaw * 0.017453292F - (float) Math.PI + f);
            double d2 = Math.abs(f1) > Math.abs(f2) ? d0 / (double) Math.abs(f1) : d0 / (double) Math.abs(f2);
            double d3 = dismounter.posX + (double) f1 * d2;
            double d4 = dismounter.posZ + (double) f2 * d2;
            dismounter.setPosition(d3, entityIn.posY + (double) entityIn.height + 0.001D, d4);

            if (dismounter.world.collidesWithAnyBlock(dismounter.getEntityBoundingBox())) {
                dismounter.setPosition(d3, entityIn.posY + (double) entityIn.height + 1.001D, d4);

                if (dismounter.world.collidesWithAnyBlock(dismounter.getEntityBoundingBox())) {
                    dismounter.setPosition(entityIn.posX, entityIn.posY + (double) dismounter.height + 0.001D, entityIn.posZ);
                }
            }
        }
    }
}
