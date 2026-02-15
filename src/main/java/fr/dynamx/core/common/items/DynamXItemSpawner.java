package fr.dynamx.core.common.items;

import com.jme3.math.Vector3f;
import fr.dynamx.core.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.core.utils.DynamXUtils;
import fr.dynamx.core.utils.optimization.Vector3fPool;
import fr.hermes.api.mc.entities.HmEntityFactory;
import fr.hermes.api.mc.entities.HmPlayerEntity;
import fr.hermes.api.mc.items.HmItemStack;
import fr.hermes.api.mc.utils.HmResourceLocation;
import fr.hermes.api.mc.world.HmWorld;
import fr.hermes.forge.JmeVector3fPool;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class DynamXItemSpawner<T extends AbstractItemObject<T, ?>> extends DynamXItem<T> {
    public DynamXItemSpawner(T itemInfo) {
        super(itemInfo);
    }

    public DynamXItemSpawner(String modid, String itemName, HmResourceLocation model) {
        super(modid, itemName, model);
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

    public boolean spawnEntity(HmItemStack itemStackIn, HmWorld worldIn, HmPlayerEntity playerIn, Vec3d blockPos) {
        if (!worldIn.hm$isClient()) {
            Vector3f pos = JmeVector3fPool.get((float) blockPos.x, (float) blockPos.y + 1F, (float) blockPos.z);
            HmEntityFactory entity = getSpawnEntity(playerIn, pos,
                    playerIn.hm$getRotationYaw() % 360.0F, itemStackIn.hm$getMetadata());
            // TODO EVENT if (!MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.Spawn(worldIn, entity, playerIn, this, blockPos)))
            worldIn.spawnHmEntity(entity, Vector3fPool.get(pos));
        }
        return true;
    }

    public abstract HmEntityFactory getSpawnEntity(@Nullable HmPlayerEntity playerIn, Vector3f pos, float spawnRotation, int metadata);
}
