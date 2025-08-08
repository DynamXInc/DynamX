package fr.dynamx.common.entities.modules;

import fr.dynamx.api.blocks.IBlockEntityModule;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.contentpack.parts.PartStorage;
import fr.dynamx.common.entities.IDynamXObject;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.physics.entities.PackEntityPhysicsHandler;
import lombok.Getter;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.Constants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Getter
public class StorageModule implements IPhysicsModule<PackEntityPhysicsHandler<?, ?>>, IBlockEntityModule, IPackInfoReloadListener {
    private final IDynamXObject owner;
    private final Map<Byte, InventoryBasic> inventories = new HashMap<>();

    public StorageModule(PackPhysicsEntity<?, ?> entity, PartStorage<?> partStorage) {
        this.owner = entity;
        addInventory(entity, partStorage);
    }

    public StorageModule(TEDynamXBlock block, BlockPos pos, PartStorage<?> partStorage) {
        this.owner = block;
        addInventory(block, pos, partStorage);
    }

    public void addInventory(PackPhysicsEntity<?, ?> entity, PartStorage<?> partStorage) {
        addInventory(player -> !entity.isDead && entity.getDistanceSq(player) <= 256, partStorage);
    }

    public void addInventory(TEDynamXBlock block, BlockPos pos, PartStorage<?> partStorage) {
        addInventory(player -> {
            return player.world.getTileEntity(pos) == block &&
                    player.getDistanceSq((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D) <= 64.0D;
        }, partStorage);
    }

    public void addInventory(Predicate<EntityPlayer> usagePredicate, PartStorage<?> partStorage) {
        inventories.put(partStorage.getId(), new InventoryBasic("part.storage"+partStorage.getOwner().getFullName(), false, partStorage.getStorageSize()) {
            @Override
            public boolean isUsableByPlayer(EntityPlayer player) {
                return usagePredicate.test(player);
            }
        });
    }

    public IInventory getInventory(byte id) {
        return inventories.get(id);
    }

    @Override
    public void getBlockDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        for (InventoryBasic inventory : inventories.values()) {
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    drops.add(stack);
                }
            }
        }
    }

    @Override
    public void onSetDead() {
        if(!(owner instanceof Entity) || ((Entity) owner).world.isRemote)
            return;
        for (InventoryBasic inventory : inventories.values()) {
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    ((Entity) owner).entityDropItem(stack, 0.5F);
                }
            }
        }
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

    @Override
    public void onPackInfosReloaded() {
        List<PartStorage> storageParts = getOwner().getPackInfo().getPartsByType(PartStorage.class);
        if(storageParts.size() != inventories.size()) {
            Map<Byte, InventoryBasic> newInventories = new HashMap<>();
            for (PartStorage<?> partStorage : storageParts) {
                if(!inventories.containsKey(partStorage.getId())) {
                    addInventory(getOwner() instanceof PackPhysicsEntity<?, ?> ? (PackPhysicsEntity<?, ?>) getOwner() : null, partStorage);
                }
                newInventories.put(partStorage.getId(), inventories.get(partStorage.getId()));
            }
            inventories.clear();
            inventories.putAll(newInventories);
        }
    }
}
