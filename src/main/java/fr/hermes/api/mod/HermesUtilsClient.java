package fr.hermes.api.mod;

import java.io.File;

public interface HermesUtilsClient {
    boolean addFileResources(File file);

    void reloadLanguageResources();

    // TODO this is not really a hermes thing
    void initializeDynamXPacks();

    void registerMinecraftRenderingHandlers();
}
