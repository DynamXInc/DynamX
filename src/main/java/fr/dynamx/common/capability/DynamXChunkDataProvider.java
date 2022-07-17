package fr.dynamx.common.capability;

import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

public class DynamXChunkDataProvider implements ICapabilitySerializable<NBTBase> {

    @CapabilityInject(DynamXChunkData.class)
    public static final Capability<DynamXChunkData> DYNAM_X_CHUNK_DATA_CAPABILITY = null;

    private final DynamXChunkData chunkAABB = DYNAM_X_CHUNK_DATA_CAPABILITY.getDefaultInstance();

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {

        return capability == DYNAM_X_CHUNK_DATA_CAPABILITY;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        return this.hasCapability(capability, facing) ? DYNAM_X_CHUNK_DATA_CAPABILITY.cast(this.chunkAABB) : null;
    }

    @Override
    public NBTBase serializeNBT() {
        return DYNAM_X_CHUNK_DATA_CAPABILITY.getStorage().writeNBT(DYNAM_X_CHUNK_DATA_CAPABILITY, this.chunkAABB, null);
    }

    @Override
    public void deserializeNBT(NBTBase nbt) {
        DYNAM_X_CHUNK_DATA_CAPABILITY.getStorage().readNBT(DYNAM_X_CHUNK_DATA_CAPABILITY, this.chunkAABB, null, nbt);
    }
}
