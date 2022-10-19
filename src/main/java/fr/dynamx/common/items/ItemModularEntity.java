package fr.dynamx.common.items;

import fr.dynamx.api.contentpack.object.IInfoOwner;
import fr.dynamx.api.contentpack.object.render.Enum3DRenderLocation;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.contentpack.parts.PartWheel;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public abstract class ItemModularEntity<T extends ModularVehicleInfo<?>> extends DynamXItemSpawner<T> implements IInfoOwner<T> {
    private final int textureNum;

    public ItemModularEntity(T modulableVehicleInfo) {
        super(modulableVehicleInfo);
        this.maxStackSize = 1;
        setCreativeTab(modulableVehicleInfo.getCreativeTab(DynamXItemRegistry.vehicleTab));

        textureNum = modulableVehicleInfo.getMaxTextureMetadata();
        if (textureNum > 1)
            setHasSubtypes(true);
    }

    @Override
    public String getJsonName(int meta) {
        return super.getJsonName(meta) + "_" + getInfo().getVariantName((byte) meta);
    }

    @Override
    public boolean createJson() {
        return getObjModel().get3DItemRenderLocation() != Enum3DRenderLocation.ALL;
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
        tooltip.add("Description: " + getInfo().getDescription());
        tooltip.add("Pack: " + getInfo().getPackName());
        if (stack.getMetadata() != 0) {
            if (textureNum > stack.getMetadata())
                tooltip.add("Texture: " + getInfo().getVariantName((byte) stack.getMetadata()));
            else
                tooltip.add(TextFormatting.RED + "Texture not found, check your pack errors");
        }
        getInfo().getPartsByType(PartWheel.class).forEach(vehicleWheelInfo -> tooltip.add("Wheel: " + vehicleWheelInfo.getDefaultWheelName()));
        //vehicleInfo.getPartsByType(PartSeat.class).forEach(seatInfo -> tooltip.add("Seat: " + seatInfo.getPartName()));
        //vehicleInfo.partShapes.forEach(vehicleCollisionInfo -> tooltip.add("Collisions: " + vehicleCollisionInfo.shapeName));
    }

    @Override
    public int getMaxMeta() {
        return textureNum;
    }
}
