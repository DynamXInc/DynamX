package fr.dynamx.common.contentpack.loader;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.ContentPackType;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.PackInfo;
import fr.dynamx.utils.errors.DynamXErrorManager;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class PacksInfoLoader extends InfoLoader<PackInfo> {
    public PacksInfoLoader(String prefix, BiFunction<String, String, PackInfo> assetCreator, @Nullable SubInfoTypesRegistry<PackInfo> infoTypesRegistry) {
        super(prefix, assetCreator, infoTypesRegistry);
    }

    @Override
    public void clear(boolean hot) {
        infos.values().removeIf(packInfo -> packInfo.getPackType() != ContentPackType.BUILTIN);
    }

    @Override
    public void postLoad(boolean hot) {
        super.postLoad(hot);
        this.infos.values().forEach(PackInfo::validateVersions);
    }

    /**
     * Searches the packs infos of the given pack <br>
     * Note that there may be several pack files pointing to the same pack (different folders but same PackName in their pack_info)
     *
     * @param packName The name of the pack (should be the name given in the pack info, not the pack file's name
     * @return The packs corresponding to the given name, or null
     */
    public List<PackInfo> findPackLocations(String packName) {
        return infos.values().stream().filter(packInfo -> packInfo.getFixedPackName().equalsIgnoreCase(packName)).collect(Collectors.toList());
    }

    public PackInfo load(String loadingPack, String configName, ContentPackLoader.PackFile file, boolean hot, String pathName, ContentPackType packType) throws IOException {
        BufferedReader inputStream = null;
        try {
            inputStream = new BufferedReader(new InputStreamReader(file.getInputStream()));
            PackInfo info = assetCreator.create(loadingPack, configName, null);
            info.setPathName(pathName);
            info.setPackType(packType);
            if (infos.containsKey(info.getFullName()))
                throw new IllegalArgumentException("Found a duplicated pack file " + configName + " in pack " + loadingPack + " !");
            readInfo(getDefaultSubInfoTypesRegistry(), inputStream, info);
            loadItems(info, hot);
            return info;
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
    public boolean load(String loadingPack, String configName, ContentPackLoader.PackFile file, boolean hot) throws IOException {
        return false; //Should be manually loaded using the above method
    }
}
