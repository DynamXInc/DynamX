package fr.hermes.api.mc.items;

import net.minecraft.nbt.NBTTagCompound;

public interface HmItemStack {
    HmItem hm$getItem();

    boolean hm$hasTagCompound();

    NBTTagCompound hm$getTagCompound();

    int hm$getMetadata();
}
