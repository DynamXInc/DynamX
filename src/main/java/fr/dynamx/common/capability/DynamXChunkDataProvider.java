package fr.dynamx.common.capability;

import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

public class DynamXChunkDataProvider implements ICapabilitySerializable<NBTBase> {

    @CapabilityInject(DynamXChunkData.class)
    public static final Capability<DynamXChunkData> DYNAMX_CHUNK_DATA_CAPABILITY = null;

    private final DynamXChunkData chunkAABB = DYNAMX_CHUNK_DATA_CAPABILITY.getDefaultInstance();

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == DYNAMX_CHUNK_DATA_CAPABILITY;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        return this.hasCapability(capability, facing) ? DYNAMX_CHUNK_DATA_CAPABILITY.cast(this.chunkAABB) : null;
    }

    @Override
    public NBTBase serializeNBT() {
        return DYNAMX_CHUNK_DATA_CAPABILITY.getStorage().writeNBT(DYNAMX_CHUNK_DATA_CAPABILITY, this.chunkAABB, null);
    }

    @Override
    public void deserializeNBT(NBTBase nbt) {
        DYNAMX_CHUNK_DATA_CAPABILITY.getStorage().readNBT(DYNAMX_CHUNK_DATA_CAPABILITY, this.chunkAABB, null, nbt);
    }
}
