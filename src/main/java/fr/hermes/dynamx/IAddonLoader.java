package fr.hermes.dynamx;

import fr.dynamx.common.contentpack.AddonInfo;

import java.util.Map;

public interface IAddonLoader {
    void discoverAddons();

    /**
     * Initializes all addons (discovered in init method)
     */
    void initAddons();

    /**
     * @param addon The addon id as specified in its annotation (may also be the modid of the addon)
     * @return True if the addon is loaded
     */
    boolean isAddonLoaded(String addon);

    /**
     * @return Loaded addons
     */
    Map<String, AddonInfo> getAddons();
}
