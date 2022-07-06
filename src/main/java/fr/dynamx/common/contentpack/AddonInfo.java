package fr.dynamx.common.contentpack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AddonInfo {
    private final String modId, addonName, version;
    private final Method addonInit;
    private final boolean requiredOnClient;

    public AddonInfo(String modId, String addonName, String version, Method addonInit, boolean requiredOnClient) {
        this.modId = modId;
        this.addonName = addonName;
        this.version = version;
        this.addonInit = addonInit;
        this.requiredOnClient = requiredOnClient;
    }

    public void initAddon() throws InvocationTargetException, IllegalAccessException {
        addonInit.invoke(null);
    }

    /**
     * @return The name of the addon (used for logging)
     */
    public String getAddonName() {
        return addonName;
    }

    /**
     * @return The id of the mod owning this addon
     */
    public String getModId() {
        return modId;
    }

    /**
     * @return The version of the addon (used for addon compatibility)
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return A boolean indicating if a client without this addon should be disconnected when trying to connect to a server having this addon loaded
     */
    public boolean isRequiredOnClient() {
        return requiredOnClient;
    }

    @Override
    public String toString() {
        return modId + ":" + addonName + ":" + version;
    }
}
