package fr.dynamx.core.common.items.tools;

import fr.aym.acsguis.api.ACsGuiApi;
import fr.dynamx.core.client.gui.GuiWrenchSelection;
import fr.dynamx.core.common.entities.PackPhysicsEntity;
import fr.dynamx.core.common.entities.PhysicsEntity;
import fr.dynamx.core.common.items.DynamXItemRegistry;
import fr.dynamx.core.utils.DynamXConstants;
import fr.dynamx.core.utils.RegistryNameSetter;
import fr.hermes.api.mc.entities.HmPlayerEntity;
import fr.hermes.api.mc.items.HmItemStack;
import fr.hermes.api.mc.world.HmWorld;
import fr.hermes.api.utils.HmEntityLogicMatcher;
import fr.hermes.api.mc.entities.HmEntity;
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
        } else {
            WrenchMode.getCurrentMode(playerIn.getHeldItem(handIn)).onWrenchRightClickClient(playerIn, handIn);
        }
        return super.onItemRightClick(worldIn, playerIn, handIn);
    }

    public static void writeEntity(HmItemStack stack, PhysicsEntity<?> entity) {
        stack.hm$getOrCreateTagCompound().setInteger("Entity1", entity.getEntityId());
    }

    public static boolean hasEntity(HmItemStack stack) {
        return stack.hm$hasTagCompound() && stack.hm$getTagCompound().hasKey("Entity1", Constants.NBT.TAG_INT);
    }

    public static void removeEntity(HmItemStack stack) {
        if (stack.hm$hasTagCompound()) {
            stack.hm$getTagCompound().removeTag("Entity1");
        }
    }

    public static PhysicsEntity<?> getEntity(HmItemStack stack, HmWorld world) {
        if (hasEntity(stack)) {
            HmEntity e = world.hm$getEntityByID(stack.hm$getTagCompound().getInteger("Entity1"));
            return HmEntityLogicMatcher.cast(e, PhysicsEntity.class);
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

    public void interact(HmPlayerEntity context, PhysicsEntity<?> physicsEntity) {
        WrenchMode.getCurrentMode(context.hm$getHeldItemMainhand()).onInteractWithEntity(context, physicsEntity, context.isSneaking());
    }
}
