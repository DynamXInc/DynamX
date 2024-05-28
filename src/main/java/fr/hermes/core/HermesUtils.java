package fr.hermes.core;

import java.io.File;
import java.net.URL;

public interface HermesUtils
{
    void addPathToClasspath(URL path);

    boolean addFileResources(File file);
}
