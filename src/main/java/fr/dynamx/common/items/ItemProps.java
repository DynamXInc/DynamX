package fr.dynamx.common.items;

import com.jme3.math.Vector3f;
import fr.dynamx.common.contentpack.type.objects.PropObject;
import fr.dynamx.common.entities.PropsEntity;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class ItemProps<T extends PropObject<T>> extends DynamXItemSpawner<T> {

    protected final int textureNum;

    public ItemProps(T itemInfo) {
        super(itemInfo);
        setCreativeTab(itemInfo.getCreativeTab(DynamXItemRegistry.objectTab));

        textureNum = itemInfo.getMaxTextureMetadata();
        if (textureNum > 1) {
            setHasSubtypes(true);
            setMaxDamage(0);
        }
    }

    @Override
    public boolean spawnEntity(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn, Vec3d blockPos) {
        Vector3f pos;
        if (!worldIn.isRemote) {
            if (playerIn.isSneaking()) {
                for (float i = 0; i < 5; i += 1) {
                    for (float j = 0; j < 5; j += 1) {
                        for (float k = 0; k < 5; k += 1) {
                            pos = new Vector3f((float) blockPos.x + i, (float) blockPos.y + 4f + (j), (float) blockPos.z + k);
                            worldIn.spawnEntity(getSpawnEntity(worldIn, playerIn, pos, playerIn.rotationYaw % 360.0F, itemStackIn.getMetadata()));
                        }
                    }
                }
            } else {
                pos = new Vector3f((float) blockPos.x + getInfo().getSpawnOffset().x, (float) blockPos.y + getInfo().getSpawnOffset().y, (float) blockPos.z + getInfo().getSpawnOffset().z);
                worldIn.spawnEntity(getSpawnEntity(worldIn, playerIn, pos, playerIn.rotationYaw % 360.0F, itemStackIn.getMetadata()));
            }
        }
        return true;
    }

    @Override
    public PropsEntity<?> getSpawnEntity(World worldIn, EntityPlayer playerIn, Vector3f pos, float spawnRotation, int metadata) {
        return new PropsEntity<>(getInfo().getFullName(), worldIn, pos, spawnRotation, metadata);
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
        return (stack.getMetadata() != 0 && textureNum > stack.getMetadata()) ? super.getTranslationKey(stack) + "_" + getInfo().getTextureVariantsFor(null).get((byte) stack.getMetadata()).getName().toLowerCase()
                : super.getTranslationKey(stack);
    }

    @Override
    public int getMaxMeta() {
        return textureNum;
    }

    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("Description: " + getInfo().getDescription());
        tooltip.add("Pack: " + getInfo().getPackName());
        if (stack.getMetadata() != 0) {
            if (textureNum > stack.getMetadata())
                tooltip.add("Texture: " + getInfo().getTextureVariantsFor(null).get((byte) stack.getMetadata()).getName());
            else
                tooltip.add(TextFormatting.RED + "Texture not found, check your pack errors");
        }
    }
}
