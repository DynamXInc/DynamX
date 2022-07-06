package fr.dynamx.common.capability;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public class DynamXChunkDataStorage implements Capability.IStorage<DynamXChunkData> {

    @Nullable
    @Override
    public NBTBase writeNBT(Capability<DynamXChunkData> capability, DynamXChunkData instance, EnumFacing side) {
        return new NBTTagCompound();
    }

    @Override
    public void readNBT(Capability<DynamXChunkData> capability, DynamXChunkData instance, EnumFacing side, NBTBase nbt) {

    }
}
