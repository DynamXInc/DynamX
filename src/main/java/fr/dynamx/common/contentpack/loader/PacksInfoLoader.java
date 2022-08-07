package fr.dynamx.common.contentpack.loader;

import fr.dynamx.common.contentpack.PackInfo;

import javax.annotation.Nullable;
import java.util.function.BiFunction;

public class PacksInfoLoader extends InfoLoader<PackInfo, PackInfo> {
    public PacksInfoLoader(String prefix, BiFunction<String, String, PackInfo> assetCreator, @Nullable SubInfoTypesRegistry<PackInfo> infoTypesRegistry) {
        super(prefix, assetCreator, infoTypesRegistry);
    }

    @Override
    public void clear(boolean hot) {
        infos.values().removeIf(packInfo -> !packInfo.isBuiltinPack());
    }

    @Override
    public void postLoad(boolean hot) {
        super.postLoad(hot);
        this.infos.values().forEach(PackInfo::validateVersions);
    }

    /**
     * Searches the pack info of the given pack
     *
     * @param packName The name of the pack (should be the name given in the pack info, not the pack file's name
     * @return The pack corresponding to the given name, or null
     */
    @Nullable
    public PackInfo findPackInfoByPackName(String packName) {
        for (PackInfo info : infos.values()) {
            if (info.getFixedPackName().equalsIgnoreCase(packName)) {
                return info;
            }
        }
        return null;
    }

    /**
     * Injects the given pack info <br>
     * Used to add default pack info to all packs
     */
    public void addInfo(String packName, PackInfo dummyInfo) {
        infos.put(packName, dummyInfo);
    }
}
