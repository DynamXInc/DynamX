package fr.dynamx.common.items.tools;

import com.jme3.math.Vector3f;
import fr.dynamx.common.entities.RagdollEntity;
import fr.dynamx.common.items.DynamXItemRegistry;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.RegistryNameSetter;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class ItemRagdoll extends Item {
    public ItemRagdoll() {
        super();
        RegistryNameSetter.setRegistryName(this, DynamXConstants.ID, "ragdoll");
        setTranslationKey(DynamXConstants.ID + "." + "ragdoll");
        setCreativeTab(DynamXItemRegistry.objectTab);
        DynamXItemRegistry.add(this);
        //ContentPackUtils.addMissingJSONs(itemInfo, DynamXMain.resDir, itemInfo.getPackName());
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand hand) {
        ItemStack itemstack = playerIn.getHeldItem(hand);
        RayTraceResult raytraceresult = DynamXUtils.rayTraceEntitySpawn(worldIn, playerIn, hand);
        if (raytraceresult == null || raytraceresult.typeOfHit != RayTraceResult.Type.BLOCK) {
            return new ActionResult<>(EnumActionResult.PASS, itemstack);
        }
        BlockPos blockpos = raytraceresult.getBlockPos();

        if (worldIn.getBlockState(blockpos).getBlock() == net.minecraft.init.Blocks.SNOW_LAYER) {
            blockpos = blockpos.down();
        }

        if (!spawnEntity(itemstack, worldIn, playerIn, blockpos)) {
            return new ActionResult(EnumActionResult.FAIL, itemstack);
        }

        if (!playerIn.capabilities.isCreativeMode) {
            itemstack.grow(-1);
        }

        playerIn.addStat(StatList.getObjectUseStats(this));
        return new ActionResult<>(EnumActionResult.SUCCESS, itemstack);
    }

    public boolean spawnEntity(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn, BlockPos blockpos) {
        if (!worldIn.isRemote) {
            if (playerIn.isSneaking()) {
                for (int i = -10; i < 10; i += 4) {
                    for (int j = -10; j < 10; j += 4) {
                        //for (int k = 0; k < 3; k++) {
                        Vector3f pos = new Vector3f(blockpos.getX() + i, blockpos.getY(), blockpos.getZ() + j);
                        RagdollEntity entity = getSpawnEntity(worldIn, playerIn, Vector3fPool.get(pos.x, pos.y + 2.3F, pos.z), playerIn.rotationYaw % 360.0F, itemStackIn.getMetadata());
                        worldIn.spawnEntity(entity);

                        // }
                    }

                }
            } else {
                RagdollEntity entity = getSpawnEntity(worldIn, playerIn, Vector3fPool.get(blockpos.getX(), blockpos.getY() + 2.19F, blockpos.getZ())
                        .add(new Vector3f(0.5f, 0, 0.5f)), playerIn.rotationYaw % 360.0F, itemStackIn.getMetadata());
                worldIn.spawnEntity(entity);
            }
            //if(!MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.PhysicsEntitySpawnedEvent(worldIn,entity,playerIn,this, blockpos)))
        }
        return true;
    }

    public RagdollEntity getSpawnEntity(World worldIn, EntityPlayer playerIn, Vector3f pos, float spawnRotation, int metadata) {
        return new RagdollEntity(worldIn, pos, spawnRotation, playerIn.getName());
    }
}
