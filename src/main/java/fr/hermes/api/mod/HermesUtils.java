package fr.hermes.api.mod;

import java.io.File;
import java.net.URL;

public interface HermesUtils
{
    void addPathToClasspath(URL path);

    //TODO SHOULD BE MOVED TO SOME CLIENT UTILS METHOD
    boolean addFileResources(File file);

    //TODO SHOULD BE MOVED TO SOME CLIENT UTILS METHOD
    void reloadLanguageResources();
}
