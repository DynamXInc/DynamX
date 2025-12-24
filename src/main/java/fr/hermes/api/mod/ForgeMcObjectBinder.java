package fr.hermes.api.mod;

import fr.hermes.api.mc.items.HmItem;
import fr.hermes.api.mc.items.HmItemStack;
import fr.hermes.api.mc.utils.HmResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * Ensures safe conversion creation of Hermes Mc objects thanks to Mixin magic
 */
public class ForgeMcObjectBinder implements McObjectBinder {
    @Override
    public HmItemStack newItemStack(HmItem item, int amount, int medata) {
        return (HmItemStack) (Object) new ItemStack((Item) item, amount, medata);
    }

    @Override
    public HmResourceLocation newResourceLocation(String location) {
        return (HmResourceLocation) new ResourceLocation(location);
    }

    @Override
    public HmResourceLocation newResourceLocation(String namespace, String path) {
        return (HmResourceLocation) new ResourceLocation(namespace, path);
    }

    @Override
    public HmItemStack emptyItemStack() {
        return (HmItemStack) (Object) ItemStack.EMPTY;
    }
}
