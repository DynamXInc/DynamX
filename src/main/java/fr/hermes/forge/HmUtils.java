package fr.hermes.forge;

import fr.hermes.api.mod.HermesUtils;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.net.URL;

public class HmUtils implements HermesUtils {
    @Override
    public void addPathToClasspath(URL path) {
        ((LaunchClassLoader) Thread.currentThread().getContextClassLoader()).addURL(path);
    }
}
