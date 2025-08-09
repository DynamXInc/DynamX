package fr.hermes.api.mod;

import fr.hermes.api.forge.HermesProgressManager;
import fr.dynamx.api.IAddonLoader;

public interface HermesMod
{
    HermesProgressManager getProgressManager();

    IAddonLoader getAddonLoader();

    HermesUtils getUtils();
}
