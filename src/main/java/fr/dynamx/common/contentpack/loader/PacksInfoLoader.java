package fr.dynamx.common.contentpack.loader;

import fr.dynamx.api.contentpack.ContentPackType;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.contentpack.PackInfo;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class PacksInfoLoader extends InfoLoader<PackInfo> {
    /**
     * @param prefix                      The prefix used to detect associated .dnx files
     * @param assetCreator                A function matching the packName and pack file name with its object
     * @param defaultSubInfoTypesRegistry The default SubInfoTypesRegistry for this object (can be overridden by ISubInfoTypeOwners)
     */
    public PacksInfoLoader(String prefix, BiFunction<String, String, PackInfo> assetCreator, @Nullable SubInfoTypesRegistry<PackInfo> defaultSubInfoTypesRegistry) {
        super(prefix, assetCreator, defaultSubInfoTypesRegistry);
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
     * @param packName The name of the pack (should be the name given in the pack info config file, not the actual pack file's name
     * @return The packs corresponding to the given name, or null
     */
    public List<PackInfo> findPackLocations(String packName) {
        return infos.values().stream().filter(packInfo -> packInfo.getFixedPackName().equalsIgnoreCase(packName)).collect(Collectors.toList());
    }

    /**
     * Loads the given pack info file
     *
     * @param loadingPack The loading pack name
     * @param file        The file to load
     * @param hot         If it's a hot reload
     * @param pathName    The path name of the pack (pack name with file extension)
     * @param packType    The pack type
     * @return The loaded pack info
     * @throws IOException If an error occurs while reading the file
     */
    public PackInfo load(String loadingPack, ContentPackLoader.PackFile file, boolean hot, String pathName, ContentPackType packType) throws IOException {
        BufferedReader inputStream = null;
        try {
            inputStream = new BufferedReader(new InputStreamReader(file.getInputStream()));
            PackInfo info = assetCreator.create(loadingPack, pathName, null);
            info.setPackType(packType);
            if (infos.containsKey(info.getFullName()))
                throw new IllegalArgumentException("Found a duplicated pack_info file " + info.getFullName() + " in pack " + loadingPack + " !");
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
