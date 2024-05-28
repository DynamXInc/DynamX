package fr.hermes.forge1122;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.hermes.core.HermesUtils;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLModContainer;
import net.minecraftforge.fml.common.MetadataCollection;
import net.minecraftforge.fml.common.discovery.ContainerType;
import net.minecraftforge.fml.common.discovery.ModCandidate;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

public class HmUtils implements HermesUtils {
    @Override
    public void addPathToClasspath(URL path) {
        ((LaunchClassLoader) Thread.currentThread().getContextClassLoader()).addURL(path);
    }

    @Override
    public boolean addFileResources(File file) {
        try {
            HashMap<String, Object> map = new HashMap<>();
            map.put("modid", DynamXConstants.ID);
            map.put("name", "DynamX assets : " + file.getName());
            map.put("version", "1.0");
            FMLModContainer container = new FMLModContainer("fr.dynamx.common.DynamXMain", new ModCandidate(file, file, file.isDirectory() ? ContainerType.DIR : ContainerType.JAR), map);
            container.bindMetadata(MetadataCollection.from(null, ""));
            FMLClientHandler.instance().addModAsResource(container);
            return true;
        } catch (Throwable e) {
            DynamXMain.log.error("Failed to load textures and models of DynamX pack : " + file.getName());
            DynamXMain.log.throwing(e);
            if (!(e instanceof Exception)) //todo clean
                e = new RuntimeException("encapsulated error", e);
            DynamXErrorManager.addError(file.getName(), DynamXErrorManager.INIT_ERRORS, "res_pack_load_fail", ErrorLevel.FATAL, "assets", "Failed to register as resource pack", (Exception) e, 700);
            return false;
        }
    }
}
