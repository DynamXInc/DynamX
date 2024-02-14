package fr.dynamx.common.items;

import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.utils.RegistryNameSetter;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class DynamXItemBlock extends ItemBlock implements IResourcesOwner, IDynamXItem<BlockObject<?>> {

    private final DynamXBlock<BlockObject<?>> dynamxMainBlock;

    public DynamXItemBlock(DynamXBlock<?> block) {
        super(block);
        this.dynamxMainBlock = (DynamXBlock<BlockObject<?>>) block;
        RegistryNameSetter.setRegistryName(this, block.getRegistryName().toString());

        if (block.textureNum > 1) {
            setHasSubtypes(true);
            setMaxDamage(0);
        }
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (this.isInCreativeTab(tab)) {
            for (byte m = 0; m < dynamxMainBlock.textureNum; m++) {
                items.add(new ItemStack(this, 1, m));
            }
        }
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return (stack.getMetadata() != 0 && dynamxMainBlock.textureNum > 1) ? super.getTranslationKey(stack) + "_" + dynamxMainBlock.getInfo().getMainObjectVariantName((byte) stack.getMetadata())
                : super.getTranslationKey(stack);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        IBlockState iblockstate = worldIn.getBlockState(pos);
        Block block = iblockstate.getBlock();

        if (!block.isReplaceable(worldIn, pos)) {
            pos = pos.offset(facing);
        }

        ItemStack itemstack = player.getHeldItem(hand);

        if (!itemstack.isEmpty() && player.canPlayerEdit(pos, facing, itemstack) && worldIn.mayPlace(this.block, pos, false, facing, null)) {
            int i = itemstack.getMetadata();
            IBlockState iblockstate1 = this.block.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, i, player, hand);
            int orientation = MathHelper.floor((player.rotationYaw * 16.0F / 360.0F) + 0.5D) & 0xF;

            if (placeBlockAt(itemstack, player, worldIn, pos, orientation, iblockstate1)) {
                iblockstate1 = worldIn.getBlockState(pos);
                SoundType soundtype = iblockstate1.getBlock().getSoundType(iblockstate1, worldIn, pos, player);
                worldIn.playSound(player, pos, soundtype.getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
                itemstack.shrink(1);
            }

            return EnumActionResult.SUCCESS;
        } else {
            return EnumActionResult.FAIL;
        }
    }

    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, int rotation, IBlockState newState) {
        if (!world.setBlockState(pos, newState, 11)) return false;

        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == this.block) {
            setTileEntityNBT(world, player, pos, stack);

            if (dynamxMainBlock.isDxModel()) {
                TileEntity tileentity = world.getTileEntity(pos);

                if (tileentity instanceof TEDynamXBlock) {
                    TEDynamXBlock teDynamXBlock = (TEDynamXBlock) tileentity;
                    teDynamXBlock.setRotation(rotation);
                }
            } else {
                world.setBlockState(pos, newState.withProperty(DynamXBlock.METADATA, rotation));
            }
            this.block.onBlockPlacedBy(world, pos, state, player, stack);

            if (player instanceof EntityPlayerMP)
                CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, pos, stack);
        }

        return true;
    }

    @Override
    public String getJsonName(int meta) {
        return ((IResourcesOwner) block).getJsonName(meta);
    }

    @Override
    public IModelPackObject getDxModel() {
        return ((IResourcesOwner) block).getDxModel();
    }

    @Override
    public int getMaxMeta() {
        return dynamxMainBlock.textureNum;
    }

    @Override
    public boolean createJson() {
        return dynamxMainBlock.createJson();
    }

    @Override
    public boolean createTranslation() {
        return dynamxMainBlock.createTranslation();
    }

    @Override
    public BlockObject<?> getInfo() {
        return dynamxMainBlock.getInfo();
    }

    @Override
    public void setInfo(BlockObject<?> info) {
        dynamxMainBlock.setInfo(info);
    }

    @Override
    public String toString() {
        return "DynamXItemBlock{" +
                "dynamxMainBlock=" + dynamxMainBlock +
                '}';
    }
}
