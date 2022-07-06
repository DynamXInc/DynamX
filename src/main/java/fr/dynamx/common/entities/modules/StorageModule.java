package fr.dynamx.common.entities.modules;

import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.common.contentpack.parts.PartStorage;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.physics.entities.PackEntityPhysicsHandler;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

public class StorageModule implements IPhysicsModule<PackEntityPhysicsHandler<?, ?>>
{
    private final InventoryBasic inventory;

    public StorageModule(BaseVehicleEntity<?> entity, PartStorage partStorage) {
        this.inventory = new InventoryBasic(entity.getPackInfo().getName(), false, partStorage.getStorageSize());
    }

    public InventoryBasic getInventory() {
        return inventory;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            list.appendTag(inventory.getStackInSlot(i).writeToNBT(new NBTTagCompound()));
        }
        tag.setTag("StorageInv", list);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        NBTTagList list = tag.getTagList("StorageInv", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < Math.min(inventory.getSizeInventory(), list.tagCount()); i++) {
            inventory.setInventorySlotContents(i, new ItemStack(list.getCompoundTagAt(i)));
        }
    }
}
