package fr.dynamx.common.capability.itemdata;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public class DynamXItemDataStorage implements Capability.IStorage<DynamXItemData> {

    @Nullable
    @Override
    public NBTBase writeNBT(Capability<DynamXItemData> capability, DynamXItemData instance, EnumFacing side) {
        return new NBTTagCompound();
    }

    @Override
    public void readNBT(Capability<DynamXItemData> capability, DynamXItemData instance, EnumFacing side, NBTBase nbt) {

    }
}
