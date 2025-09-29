package fr.hermes.api.mod;

import fr.hermes.api.mc.HmItem;
import fr.hermes.api.mc.HmItemStack;
import fr.hermes.api.mc.HmResourceLocation;

public interface McObjectBinder {
    McObjectBinder instance = new ForgeMcObjectBinder(); //TODO DYNAMIC

    HmItemStack newItemStack(HmItem item, int amount, int medata);

    HmResourceLocation newResourceLocation(String location);

    HmResourceLocation newResourceLocation(String namespace, String path);

    HmItemStack emptyItemStack();
}
