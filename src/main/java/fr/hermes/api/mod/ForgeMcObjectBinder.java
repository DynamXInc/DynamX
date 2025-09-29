package fr.hermes.api.mod;

import fr.hermes.api.mc.HmItem;
import fr.hermes.api.mc.HmItemStack;
import fr.hermes.api.mc.HmResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class ForgeMcObjectBinder implements McObjectBinder {
    @Override
    public HmItemStack newItemStack(HmItem item, int amount, int medata) {
        return new ItemStack((Item) item, amount, medata);
    }

    @Override
    public HmResourceLocation newResourceLocation(String location) {
        return new ResourceLocation(location);
    }

    @Override
    public HmResourceLocation newResourceLocation(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }
}
