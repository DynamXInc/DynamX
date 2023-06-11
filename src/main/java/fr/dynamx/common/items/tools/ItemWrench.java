package fr.dynamx.common.items.tools;

import fr.aym.acsguis.api.ACsGuiApi;
import fr.dynamx.client.gui.GuiWrenchSelection;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.items.DynamXItemRegistry;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.RegistryNameSetter;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.List;

public class ItemWrench extends Item {

    public ItemWrench() {
        RegistryNameSetter.setRegistryName(this, DynamXConstants.ID, "wrench");
        setTranslationKey("dynamxmod.wrench");
        setCreativeTab(DynamXItemRegistry.vehicleTab);
        this.maxStackSize = 1;
        DynamXItemRegistry.add(this);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
        if (!player.world.isRemote) {
            WrenchMode.getCurrentMode(stack).onWrenchLeftClickEntity(stack, player, entity);
        }
        return true;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        if (worldIn.isRemote) {
            if (playerIn.isSneaking()) {
                ACsGuiApi.asyncLoadThenShowGui("wrench_gui", GuiWrenchSelection::new);
            }
        }
        if (!worldIn.isRemote) {
            WrenchMode.getCurrentMode(playerIn.getHeldItem(handIn)).onWrenchRightClick(playerIn, handIn);
        } else
            WrenchMode.getCurrentMode(playerIn.getHeldItem(handIn)).onWrenchRightClickClient(playerIn, handIn);
        return super.onItemRightClick(worldIn, playerIn, handIn);
    }

    public static void writeEntity(ItemStack stack, PhysicsEntity<?> entity) {
        if (!stack.hasTagCompound())
            stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setInteger("Entity1", entity.getEntityId());
    }

    public static boolean hasEntity(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().hasKey("Entity1", Constants.NBT.TAG_INT);
    }

    public static void removeEntity(ItemStack stack) {
        if (stack.hasTagCompound()) {
            stack.getTagCompound().removeTag("Entity1");
        }
    }

    public static PhysicsEntity<?> getEntity(ItemStack stack, World world) {
        if (hasEntity(stack)) {
            Entity e = world.getEntityByID(stack.getTagCompound().getInteger("Entity1"));
            if (e instanceof PhysicsEntity) {
                return (PhysicsEntity<?>) e;
            }
            return null;
        }
        return null;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(I18n.format("wrench.mode.mode", I18n.format(WrenchMode.getCurrentMode(stack).getLabel())));
        if (hasEntity(stack)) {
            PhysicsEntity<?> e = getEntity(stack, worldIn);
            if (e instanceof PackPhysicsEntity)
                tooltip.add("Linked entity " + ((PackPhysicsEntity<?, ?>) e).getInfoName());
            else if (e != null)
                tooltip.add("Linked entity " + e.getName());
        }
    }

    public void interact(EntityPlayer context, PhysicsEntity<?> physicsEntity) {
        WrenchMode.getCurrentMode(context.getHeldItemMainhand()).onInteractWithEntity(context, physicsEntity, context.isSneaking());
    }
}
