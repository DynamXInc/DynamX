package fr.dynamx.common.contentpack.loader;

import fr.dynamx.api.contentpack.ContentPackType;
import fr.dynamx.common.contentpack.PackInfo;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
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

    public PackInfo load(String loadingPack, String configName, BufferedReader inputStream, boolean hot, String pathName, ContentPackType packType) throws IOException {
        PackInfo info = assetCreator.apply(loadingPack, configName);
        info.setPathName(pathName);
        info.setPackType(packType);
        if (infos.containsKey(info.getFullName()))
            throw new IllegalArgumentException("Found a duplicated pack file " + configName + " in pack " + loadingPack + " !");
        readInfo(inputStream, info);
        loadItems(info, hot);
        return info;
    }

    @Override
    public boolean load(String loadingPack, String configName, BufferedReader inputStream, boolean hot) throws IOException {
        return false; //Should be manually loaded using the above method
    }
}
