package fr.dynamx.common.items.tools;

import com.jme3.math.Vector3f;
import fr.dynamx.common.items.DynamXItemRegistry;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class ItemSlopes extends Item
{
    public ItemSlopes() {
        super();
        setRegistryName(DynamXConstants.ID, "slopes");
        setTranslationKey(DynamXConstants.ID + "." + "slopes");
        setCreativeTab(DynamXItemRegistry.vehicleTab);
        DynamXItemRegistry.add(this);
        //ContentPackUtils.addMissingJSONs(itemInfo, DynamXMain.resDir, itemInfo.getPackName());
    }

    public void clearMemory(World worldIn, EntityPlayer playerIn, ItemStack stack) {
        if(!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        NBTTagCompound nbt = stack.getTagCompound();
        if(nbt.getInteger("mode")==1) {//Create
            nbt.removeTag("plist");
            if (worldIn.isRemote)
                playerIn.sendMessage(new TextComponentString(TextFormatting.GREEN+"[CREATE] La pente de sélection a été réinitialisée"));
        }
        else if(nbt.getInteger("mode")==0) { //Delete
            nbt.removeTag("p1");
            nbt.removeTag("p2");
            if (worldIn.isRemote)
                playerIn.sendMessage(new TextComponentString(TextFormatting.LIGHT_PURPLE+"[DELETE] La région de sélection a été réinitialisée"));
        }
        else if(nbt.getInteger("mode")==2) { //Auto
            nbt.removeTag("pt1");
            nbt.removeTag("pt2");
            nbt.removeTag("ptface");
            nbt.removeTag("ptround");
            if (worldIn.isRemote)
                playerIn.sendMessage(new TextComponentString(TextFormatting.GOLD+"[AUTO] La région de sélection a été réinitialisée"));
        }
    }

    public static Vector3f fixPos(World worldIn, Vec3d post) {
        Vector3f pos = Vector3fPool.get(DynamXMath.preciseRound(post.x), DynamXMath.preciseRound(post.y), DynamXMath.preciseRound(post.z));
        BlockPos bpos = new BlockPos(post.x, post.y, post.z);
        IBlockState state = worldIn.getBlockState(bpos);
        AxisAlignedBB box = state.getCollisionBoundingBox(worldIn, bpos);
        pos.y = (float) (bpos.getY() + (box == null ? 0 : box.maxY));

        return pos;
    }

    public void clickedWith(World worldIn, EntityPlayer playerIn, EnumHand handIn, Vector3f pos) {
        if(handIn!=EnumHand.MAIN_HAND)
            return;

        ItemStack s = playerIn.getHeldItem(handIn);
        if(!s.hasTagCompound())s.setTagCompound(new NBTTagCompound());
        NBTTagCompound nbt = s.getTagCompound();
        if(nbt.getInteger("mode")==1){//Create
            if(!nbt.hasKey("plist"))
                nbt.setTag("plist",new NBTTagList());
            NBTTagList list = nbt.getTagList("plist",10);
            boolean set = false;
            for (int i = 0; i < list.tagCount(); i++) {
                Vector3f other = getPosFromTag(list.getCompoundTagAt(i));
                if(other.equals(pos))
                {
                    set = true;
                    list.removeTag(i);
                    break;
                }
            }
            if(!set) {
                //if (list.tagCount() < 4)
                    list.appendTag(createPosTag(pos));
                //else list.set(3, createPosTag(pos));
            }
            //if(worldIn.isRemote)
                //playerIn.sendMessage(new TextComponentString("[CREATE] Vous avez ajouté le point "+pos+" à la liste. La liste contient \2473"+list.tagCount()+" \247fpoints"));
        }else if(nbt.getInteger("mode")==0){ //Delete
            if(!nbt.hasKey("p1")){
                nbt.removeTag("p2");
                nbt.setTag("p1",createPosTag(pos));
                //if(worldIn.isRemote)
                  //  playerIn.sendMessage(new TextComponentString("[DELETE] Point 1 défini à "+pos));
            }else{
                if(pos.equals(getPosFromTag(nbt.getCompoundTag("p1")))) {
                    if(!nbt.hasKey("p2"))
                        nbt.removeTag("p1");
                    else {
                        nbt.setTag("p1", nbt.getCompoundTag("p2"));
                        nbt.removeTag("p2");
                    }
                } else
                    nbt.setTag("p2",createPosTag(pos));
            }
        }else if(nbt.getInteger("mode")==2){ //Auto
            if(!nbt.hasKey("pt1")){
                nbt.removeTag("pt2");
                nbt.setTag("pt1",createPosTag(pos));
            }else{
                if(pos.equals(getPosFromTag(nbt.getCompoundTag("pt1")))) {
                    if(!nbt.hasKey("pt2"))
                        nbt.removeTag("pt1");
                    else {
                        nbt.setTag("pt1", nbt.getCompoundTag("pt2"));
                        nbt.removeTag("pt2");
                    }
                } else
                    nbt.setTag("pt2",createPosTag(pos));
            }
        }
    }

    public static NBTTagCompound createPosTag(Vector3f pos)
    {
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        nbttagcompound.setFloat("X", pos.x);
        nbttagcompound.setFloat("Y", pos.y);
        nbttagcompound.setFloat("Z", pos.z);
        return nbttagcompound;
    }

    public static Vector3f getPosFromTag(NBTTagCompound tag)
    {
        return Vector3fPool.get(tag.getFloat("X"), tag.getFloat("Y"), tag.getFloat("Z"));
    }

    @Override
    public boolean getShareTag() {
        return super.getShareTag();
    }

    @Override
    public NBTTagCompound getNBTShareTag(ItemStack stack) {
        return super.getNBTShareTag(stack);
    }

    @Override
    public void readNBTShareTag(ItemStack stack, @Nullable NBTTagCompound nbt) {
        super.readNBTShareTag(stack, nbt);
    }
}
