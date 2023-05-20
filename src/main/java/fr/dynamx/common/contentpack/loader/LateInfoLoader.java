package fr.dynamx.common.contentpack.loader;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.utils.errors.DynamXErrorManager;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * A late info loader is a loader that loads the files only when they are requested
 * @param <T> The type of the info
 */
public class LateInfoLoader<T extends ISubInfoTypeOwner<?>> extends InfoLoader<T> {
    private final Map<String, ContentPackLoader.PackFile> cachedFiles = new HashMap<>();

    public LateInfoLoader(String prefix, AssetCreator<T> assetCreator, @Nullable SubInfoTypesRegistry<T> infoTypesRegistry) {
        super(prefix, assetCreator, infoTypesRegistry);
    }

    /**
     * Loads a file, only if the prefix matches with this object
     *
     * @param loadingPack The pack owning the object
     * @param configName  The object's name
     * @param file        The object file
     * @param hot         If it's an hot reload
     * @return True if this InfoLoader has loaded this object
     * @throws IOException If an error occurs while reading the stream
     */
    public boolean load(String loadingPack, String configName, ContentPackLoader.PackFile file, boolean hot) throws IOException {
        if (configName.startsWith(prefix)) {
            cachedFiles.put(loadingPack + "." + configName, file);
            return true;
        }
        return false;
    }

    @Nullable
    public T findOrLoadInfo(String infoFullName, Class<? extends T> clazz) {
        T loaded = super.findInfo(infoFullName);
        if (loaded != null || !cachedFiles.containsKey(infoFullName)) {
            return loaded;
        }
        ContentPackLoader.PackFile file = cachedFiles.remove(infoFullName);
        String loadingPack = infoFullName.substring(0, infoFullName.indexOf('.'));
        String configName = infoFullName.substring(infoFullName.indexOf('.') + 1);
        BufferedReader inputStream = null;
        try {
            inputStream = new BufferedReader(new InputStreamReader(file.getInputStream()));
            T info = assetCreator.create(loadingPack, configName, clazz.getName());
            if (infos.containsKey(info.getFullName()))
                throw new IllegalArgumentException("Found a duplicated pack file " + configName + " in pack " + loadingPack + " !");
            readInfo(inputStream, info);
            loadItems(info, ContentPackLoader.isHotReloading);
            return info;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (Throwable e) {
            if (!(e instanceof Exception)) //todo clean
                e = new RuntimeException("encapsulated error", e);
            DynamXErrorManager.addError(loadingPack, DynamXErrorManager.PACKS_ERRORS, "pack_file_load_error", ErrorLevel.FATAL, file.getName().replace(prefix, ""), null, (Exception) e, 100);
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void clear(boolean hot) {
        super.clear(hot);
        cachedFiles.clear();
    }
}
