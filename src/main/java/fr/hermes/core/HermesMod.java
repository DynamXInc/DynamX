package fr.hermes.core;

import fr.hermes.dynamx.IAddonLoader;

public interface HermesMod
{
    HermesProgressManager getProgressManager();

    IAddonLoader getAddonLoader();

    HermesUtils getUtils();
}
