package fr.dynamx.common.items;

import com.jme3.math.Vector3f;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nonnull;

public abstract class DynamXItemSpawner<T extends AbstractItemObject<T, ?>> extends DynamXItem<T> {
    public DynamXItemSpawner(T itemInfo) {
        super(itemInfo);
    }

    @Override
    @Nonnull
    public ActionResult<ItemStack> onItemRightClick(@Nonnull World worldIn, EntityPlayer playerIn, @Nonnull EnumHand hand) {
        ItemStack itemstack = playerIn.getHeldItem(hand);
        if (hand == EnumHand.MAIN_HAND) {
            RayTraceResult raytraceresult = DynamXUtils.rayTraceEntitySpawn(worldIn, playerIn, hand);
            if (raytraceresult == null) {
                return new ActionResult<>(EnumActionResult.PASS, itemstack);
            }
            if (raytraceresult.typeOfHit == RayTraceResult.Type.BLOCK || raytraceresult.typeOfHit == RayTraceResult.Type.ENTITY) {
                BlockPos blockpos = raytraceresult.getBlockPos();
                if (raytraceresult.typeOfHit == RayTraceResult.Type.ENTITY) {
                    blockpos = raytraceresult.entityHit.getPosition();
                }

                if (worldIn.getBlockState(blockpos).getBlock() == Blocks.SNOW_LAYER) {
                    blockpos = blockpos.down();
                }

                if (!spawnEntity(itemstack, worldIn, playerIn, raytraceresult.hitVec)) {
                    return new ActionResult<>(EnumActionResult.FAIL, itemstack);
                }

                if (!playerIn.capabilities.isCreativeMode) {
                    itemstack.grow(-1);
                }

                playerIn.addStat(StatList.getObjectUseStats(this));
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, itemstack);
        }
        return new ActionResult<>(EnumActionResult.FAIL, itemstack);
    }

    public boolean spawnEntity(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn, Vec3d blockPos) {
        if (!worldIn.isRemote) {
            PackPhysicsEntity<?, ?> entity = getSpawnEntity(worldIn, playerIn, Vector3fPool.get((float) blockPos.x, (float) blockPos.y + 1F, (float) blockPos.z), playerIn.rotationYaw % 360.0F, itemStackIn.getMetadata());
            if (!MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.PhysicsEntitySpawnedEvent(worldIn, entity, playerIn, this, blockPos)))
                worldIn.spawnEntity(entity);
        }
        return true;
    }

    public abstract PackPhysicsEntity<?, ?> getSpawnEntity(World worldIn, EntityPlayer playerIn, Vector3f pos, float spawnRotation, int metadata);
}
