package fr.hermes.client.api;

import java.io.File;

public interface HermesUtilsClient {
    void addFileResources(File file);

    void reloadLanguageResources();

    // TODO this is not really a hermes thing
    void initializeDynamXPacks();
}
