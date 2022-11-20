package fr.dynamx.common.contentpack.loader;

import fr.dynamx.api.contentpack.ContentPackType;
import fr.dynamx.common.contentpack.PackInfo;

import javax.annotation.Nullable;
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

    /**
     * Injects the given pack info <br>
     * Used to add default pack info to all packs
     */
    public void addInfo(String packName, PackInfo dummyInfo) {
        infos.put(packName, dummyInfo);
    }
}
