package fr.dynamx.common.capability.itemdata;

import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

public class DynamXItemDataProvider implements ICapabilitySerializable<NBTBase> {

    @CapabilityInject(DynamXItemData.class)
    public static final Capability<DynamXItemData> DYNAMX_ITEM_DATA_CAPABILITY = null;

    private final DynamXItemData itemData = DYNAMX_ITEM_DATA_CAPABILITY.getDefaultInstance();

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {

        return capability == DYNAMX_ITEM_DATA_CAPABILITY;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        return this.hasCapability(capability, facing) ? DYNAMX_ITEM_DATA_CAPABILITY.cast(this.itemData) : null;
    }

    @Override
    public NBTBase serializeNBT() {
        return DYNAMX_ITEM_DATA_CAPABILITY.getStorage().writeNBT(DYNAMX_ITEM_DATA_CAPABILITY, this.itemData, null);
    }

    @Override
    public void deserializeNBT(NBTBase nbt) {
        DYNAMX_ITEM_DATA_CAPABILITY.getStorage().readNBT(DYNAMX_ITEM_DATA_CAPABILITY, this.itemData, null, nbt);
    }
}
