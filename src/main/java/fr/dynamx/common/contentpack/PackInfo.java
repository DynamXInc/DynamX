package fr.dynamx.common.contentpack;

import fr.aym.acslib.api.services.ErrorTrackingService;
import fr.dynamx.api.contentpack.ContentPackType;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXLoadingTasks;
import lombok.Getter;
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;
import net.minecraftforge.fml.common.versioning.InvalidVersionSpecificationException;
import net.minecraftforge.fml.common.versioning.VersionRange;

import java.util.ArrayList;
import java.util.List;

public class PackInfo extends SubInfoTypeOwner<PackInfo> implements INamedObject {
    private final String originalPackName;
    @PackFileProperty(configNames = "PackName")
    private final String packName;
    @Getter
    private ContentPackType packType;

    private final List<RequiredAddonInfo> required = new ArrayList<>();
    @Getter
    private String pathName;

    @Getter
    @PackFileProperty(configNames = "PackVersion", required = false)
    private String packVersion = "nc";
    @PackFileProperty(configNames = "CompatibleWithLoaderVersions", required = false)
    @Getter
    private String compatibleLoaderVersions;
    @Getter
    @PackFileProperty(configNames = "DcFileVersion", defaultValue = DynamXConstants.DC_FILE_VERSION)
    private String dcFileVersion = DynamXConstants.DC_FILE_VERSION;

    public PackInfo(String packName, ContentPackType packType) {
        this.originalPackName = this.packName = packName;
        this.pathName = packName;
        this.packType = packType;
    }

    public void validateVersions() {
        DynamXMain.log.debug("Validating from " + compatibleLoaderVersions + " for " + getFixedPackName());
        if (DynamXContext.getErrorTracker().getAllErrors().containsKey(getFixedPackName()) && !DynamXContext.getErrorTracker().getAllErrors().get(getFixedPackName()).getErrors().isEmpty()) {
            if (!StringUtils.isNullOrEmpty(compatibleLoaderVersions)) {
                try {
                    VersionRange range = VersionRange.createFromVersionSpec(compatibleLoaderVersions);
                    if (!range.containsVersion(DynamXConstants.PACK_LOADER_VERSION)) {
                        DynamXMain.log.warn("Outdated content pack " + getFixedPackName() + " found, compatible with loader versions " + compatibleLoaderVersions);
                        DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.PACK, getFixedPackName(), "Outdated content pack", "This pack is made for versions " + compatibleLoaderVersions + " of the DynamX's pack loader (currently in version " + DynamXConstants.PACK_LOADER_VERSION.getVersionString() + ")", ErrorTrackingService.TrackedErrorLevel.LOW);
                    }
                } catch (InvalidVersionSpecificationException ignored) {
                } //Already caught in onComplete
            }
            if (!StringUtils.isNullOrEmpty(dcFileVersion) && packType.isCompressed()) {
                if (!dcFileVersion.equalsIgnoreCase(DynamXConstants.DC_FILE_VERSION)) {
                    DynamXMain.log.warn("Outdated content pack " + getFixedPackName() + " found. Compatible with dc files version " + dcFileVersion);
                    DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.PACK, getFixedPackName(), "Outdated content pack", "The model files are compiled for version " + dcFileVersion + " of the DynamX's .dc file loader (currently in version " + DynamXConstants.DC_FILE_VERSION + "). The pack will take more time to load.", ErrorTrackingService.TrackedErrorLevel.HIGH);
                }
            }
            for (RequiredAddonInfo addonInfo : required) {
                if (!StringUtils.isNullOrEmpty(addonInfo.versions) && AddonLoader.isAddonLoaded(addonInfo.addonId)) {
                    try {
                        VersionRange range = VersionRange.createFromVersionSpec(addonInfo.versions);
                        if (!range.containsVersion(new DefaultArtifactVersion(AddonLoader.getAddons().get(addonInfo.addonId).getVersion()))) {
                            DynamXMain.log.warn("Outdated content pack " + getFixedPackName() + " found, compatible with versions of addon " + addonInfo.addonId + " : " + compatibleLoaderVersions);
                            DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.PACK, getFixedPackName(), "Unsupported addon version", "This pack is made for versions " + addonInfo.versions + " of the addon " + addonInfo.addonId + " (currently in version " + AddonLoader.getAddons().get(addonInfo.addonId).getVersion() + ")", ErrorTrackingService.TrackedErrorLevel.LOW);
                        }
                    } catch (InvalidVersionSpecificationException ignored) {
                    } //Already caught in onComplete
                }
            }
        }
    }

    @Override
    public void onComplete(boolean hotReload) {
        DynamXMain.log.info("Pack " + packName + " loading in version " + packVersion);
        if (!StringUtils.isNullOrEmpty(compatibleLoaderVersions)) {
            try { //Check format of the version specs
                VersionRange.createFromVersionSpec(compatibleLoaderVersions);
            } catch (InvalidVersionSpecificationException e) {
                DynamXMain.log.fatal("Invalid CompatibleWithLoaderVersions in " + getFullName(), e);
                DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.PACK, getFixedPackName(), "Bad CompatibleWithLoaderVersions property in pack_info", e, ErrorTrackingService.TrackedErrorLevel.FATAL);
                compatibleLoaderVersions = "";
            }
        }
        for (RequiredAddonInfo addonInfo : required) {
            if (!AddonLoader.isAddonLoaded(addonInfo.addonId)) {
                DynamXMain.log.error("Addon " + addonInfo.addonId + " is missing for content pack " + getFixedPackName());
                DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.PACK, getFixedPackName(), "Missing addon", "This pack requires the addon " + addonInfo.addonId + " in order to be loaded", ErrorTrackingService.TrackedErrorLevel.FATAL);
            }
            if (!StringUtils.isNullOrEmpty(addonInfo.versions) && AddonLoader.isAddonLoaded(addonInfo.addonId)) {
                try { //Check format of the version specs
                    VersionRange.createFromVersionSpec(addonInfo.versions);
                } catch (InvalidVersionSpecificationException e) {
                    DynamXMain.log.fatal("Invalid Versions in declaration of required addon " + addonInfo.getFullName(), e);
                    DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.PACK, getFixedPackName(), "Bad required addon " + addonInfo.getFullName() + " Versions in pack_info", e, ErrorTrackingService.TrackedErrorLevel.FATAL);
                    addonInfo.versions = "";
                }
            }
        }
    }

    public PackInfo setPathName(String pathName) {
        this.pathName = pathName;
        return this;
    }

    public PackInfo setPackVersion(String packVersion) {
        this.packVersion = packVersion;
        return this;
    }

    public PackInfo setPackType(ContentPackType packType) {
        this.packType = packType;
        return this;
    }

    @Override
    public String getName() {
        return "pack_info";
    }

    @Override
    public String getPackName() {
        return originalPackName;
    }

    /**
     * @return The PackName configured in the pack_info.dynx file
     */
    public String getFixedPackName() {
        return packName;
    }

    @RegisteredSubInfoType(name = "RequiredAddon", registries = SubInfoTypeRegistries.PACKS, strictName = false)
    public static class RequiredAddonInfo extends SubInfoType<PackInfo> {
        private final String name;

        @PackFileProperty(configNames = "Id")
        private String addonId;
        @PackFileProperty(configNames = "Versions", required = false)
        private String versions;

        public RequiredAddonInfo(ISubInfoTypeOwner<PackInfo> owner, String name) {
            super(owner);
            this.name = name;
        }

        @Override
        public void appendTo(PackInfo owner) {
            owner.required.add(this);
        }

        @Override
        public String getName() {
            return "Required addon " + name + " in " + getOwner().getName();
        }

        @Override
        public String toString() {
            return "RequiredAddonInfo{" +
                    "name='" + name + '\'' +
                    ", addonId='" + addonId + '\'' +
                    ", versions='" + versions + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "PackInfo{" +
                "originalPackName='" + originalPackName + '\'' +
                ", packName='" + packName + '\'' +
                ", packType=" + packType +
                ", required=" + required +
                ", pathName='" + pathName + '\'' +
                ", packVersion='" + packVersion + '\'' +
                ", compatibleLoaderVersions='" + compatibleLoaderVersions + '\'' +
                ", dcFileVersion='" + dcFileVersion + '\'' +
                '}';
    }
}
