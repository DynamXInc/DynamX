package fr.dynamx.common.items;

import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.render.Enum3DRenderLocation;
import fr.dynamx.common.contentpack.parts.PartEntitySeat;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public abstract class ItemModularEntity extends DynamXItemSpawner<ModularVehicleInfo> implements IDynamXItem<ModularVehicleInfo> {
    private final int textureNum;

    public ItemModularEntity(ModularVehicleInfo modulableVehicleInfo) {
        super(modulableVehicleInfo);
        maxStackSize = 1;
        setCreativeTab(modulableVehicleInfo.getCreativeTab(DynamXItemRegistry.vehicleTab));

        textureNum = modulableVehicleInfo.getMaxVariantId();
        if (textureNum > 1)
            setHasSubtypes(true);
    }

    @Override
    public String getJsonName(int meta) {
        return super.getJsonName(meta) + "_" + getInfo().getVariantName((byte) meta);
    }

    @Override
    public boolean createJson() {
        return getDxModel().get3DItemRenderLocation() != Enum3DRenderLocation.ALL;
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (this.isInCreativeTab(tab)) {
            for (byte m = 0; m < textureNum; m++) {
                items.add(new ItemStack(this, 1, m));
            }
        }
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        if (stack.getMetadata() != 0 && textureNum > stack.getMetadata()) {
            return super.getTranslationKey(stack) + "_" + getInfo().getVariantName((byte) stack.getMetadata());
        }
        return super.getTranslationKey(stack);
    }

    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);

        if(flagIn.isAdvanced()) {
            getInfo().getPartsByType(PartWheel.class).forEach(vehicleWheelInfo -> tooltip.add("Wheel: " + vehicleWheelInfo.getDefaultWheelName()));

            int seats = getInfo().getPartsByType(PartEntitySeat.class).size();
            if (seats > 0) {
                tooltip.add(seats + " seats");
            }
        }
    }

    @Override
    public int getMaxMeta() {
        return textureNum;
    }
}
