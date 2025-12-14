package fr.hermes.api.mod;

import fr.hermes.api.mc.items.HmItem;
import fr.hermes.api.mc.items.HmItemStack;
import fr.hermes.api.mc.utils.HmResourceLocation;

public interface McObjectBinder {
    McObjectBinder instance = new ForgeMcObjectBinder(); //TODO DYNAMIC

    HmItemStack newItemStack(HmItem item, int amount, int medata);

    HmResourceLocation newResourceLocation(String location);

    HmResourceLocation newResourceLocation(String namespace, String path);

    HmItemStack emptyItemStack();
}
