package fr.dynamx.common.entities.modules;

import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.common.contentpack.parts.PartStorage;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.physics.entities.PackEntityPhysicsHandler;
import lombok.Getter;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StorageModule implements IPhysicsModule<PackEntityPhysicsHandler<?, ?>> {
    @Getter
    private final Map<Byte, InventoryBasic> inventories = new HashMap<>();

    public StorageModule(BaseVehicleEntity<?> entity, PartStorage partStorage) {
        addInventory(entity, partStorage);
    }

    public void addInventory(BaseVehicleEntity<?> entity, PartStorage partStorage) {
        inventories.put(partStorage.getId(), new InventoryBasic(entity.getPackInfo().getName(), false, partStorage.getStorageSize()));
    }

    public IInventory getInventory(byte id) {
        return inventories.get(id);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        int j = 0;
        for(Map.Entry<Byte, InventoryBasic> inventoryBasic : inventories.entrySet()) {
            NBTTagList list = new NBTTagList();
            for (int i = 0; i < inventoryBasic.getValue().getSizeInventory(); i++) {
                list.appendTag(inventoryBasic.getValue().getStackInSlot(i).writeToNBT(new NBTTagCompound()));
            }
            tag.setTag("StorageInv"+j, list);
            j++;
        }
        tag.setInteger("StorageCount", j);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        if(tag.hasKey("StorageInv", Constants.NBT.TAG_LIST)) {
            NBTTagList list = tag.getTagList("StorageInv", Constants.NBT.TAG_COMPOUND);
            InventoryBasic inventory = inventories.get((byte) 0);
            for (int i = 0; i < Math.min(inventory.getSizeInventory(), list.tagCount()); i++) {
                inventory.setInventorySlotContents(i, new ItemStack(list.getCompoundTagAt(i)));
            }
        }
        for (int j = 0; j < Math.min(tag.getInteger("StorageCount"), inventories.size()); j++) {
            NBTTagList list = tag.getTagList("StorageInv"+j, Constants.NBT.TAG_COMPOUND);
            InventoryBasic inventory = inventories.get((byte) j);
            for (int i = 0; i < Math.min(inventory.getSizeInventory(), list.tagCount()); i++) {
                inventory.setInventorySlotContents(i, new ItemStack(list.getCompoundTagAt(i)));
            }
        }
    }
}
