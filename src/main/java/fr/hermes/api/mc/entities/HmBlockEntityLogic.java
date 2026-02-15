package fr.hermes.api.mc.entities;

import net.minecraft.nbt.NBTTagCompound;

public interface HmBlockEntityLogic {
    void update();

    void readFromNbt(NBTTagCompound tag);

    void writeToNbt(NBTTagCompound tag);

    float getMaxRenderDistanceSquared();

    void onLoad();
}
