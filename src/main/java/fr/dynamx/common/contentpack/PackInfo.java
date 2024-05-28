package fr.dynamx.common.contentpack;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.aym.acslib.api.services.mps.ModProtectionContainer;
import fr.dynamx.api.contentpack.ContentPackType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.hermes.forge1122.dynamx.AddonLoader;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;
import net.minecraftforge.fml.common.versioning.InvalidVersionSpecificationException;
import net.minecraftforge.fml.common.versioning.VersionRange;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Holds basic information about a pack <br>
 * Version, dependencies and path
 */
@Getter
public class PackInfo extends SubInfoTypeOwner<PackInfo> {
    /**
     * The pack name, defined as the pack folder name or the pack zip file name (without extension)
     */
    protected final String originalPackName;
    /**
     * The path of the pack, relative to the DynamX directory (pack name with the file extension)
     */
    protected final String pathName;
    /**
     * The type of the pack
     */
    protected ContentPackType packType;

    /**
     * The PackName configured in the pack_info.dynx file
     */
    @Setter
    @PackFileProperty(configNames = "PackName")
    protected String fixedPackName;

    protected final List<RequiredAddonInfo> requiredAddons = new ArrayList<>();

    @PackFileProperty(configNames = "PackVersion", required = false, defaultValue = "1.0.0")
    protected String packVersion = "<missing>";
    @PackFileProperty(configNames = "CompatibleWithLoaderVersions", required = false, defaultValue = "[1.1.0,)")
    protected String compatibleLoaderVersions;
    @PackFileProperty(configNames = "DcFileVersion", defaultValue = DynamXConstants.DC_FILE_VERSION)
    protected String dcFileVersion = DynamXConstants.DC_FILE_VERSION;

    /**
     * Creates a new pack info
     *
     * @param packName The pack name (folder name or zip file name without extension)
     * @param pathName The path of the pack, relative to the DynamX directory (pack name with the file extension)
     * @param packType The type of the pack
     */
    protected PackInfo(String packName, String pathName, ContentPackType packType) {
        this.originalPackName = this.fixedPackName = packName;
        this.pathName = pathName;
        this.packType = packType;
    }

    /**
     * Creates a fake pack info for the given addon
     * @param modId The addon/mod id
     * @return A PackInfo for the given addon
     */
    public static PackInfo forAddon(String modId) {
        return new PackInfo(modId, modId, ContentPackType.BUILTIN);
    }

    public void validateVersions() {
        DynamXMain.log.debug("Validating from " + compatibleLoaderVersions + " for " + getFixedPackName());
        boolean hasLinkedErrors = DynamXErrorManager.getErrorManager().getAllErrors().containsKey(getFixedPackName()) && !DynamXErrorManager.getErrorManager().getAllErrors().get(getFixedPackName()).getErrors().isEmpty();
        if (!StringUtils.isNullOrEmpty(compatibleLoaderVersions)) {
            try { //Check format of the version specs
                VersionRange range = VersionRange.createFromVersionSpec(compatibleLoaderVersions);
                if (hasLinkedErrors && !range.containsVersion(DynamXConstants.PACK_LOADER_VERSION)) {
                    //DynamXMain.log.warn("Outdated content pack " + getFixedPackName() + " found, compatible with loader versions " + compatibleLoaderVersions);
                    DynamXErrorManager.addError(getFixedPackName(), DynamXErrorManager.PACKS_ERRORS, "pack_requirements", ErrorLevel.LOW, "pack_version", "This pack is made for versions " + compatibleLoaderVersions + " of the DynamX's pack loader (currently in version " + DynamXConstants.PACK_LOADER_VERSION.getVersionString() + ")", null, 600);
                }
            } catch (InvalidVersionSpecificationException e) {
                //DynamXMain.log.fatal("Invalid CompatibleWithLoaderVersions in " + getFullName(), e);
                DynamXErrorManager.addError(getFixedPackName(), DynamXErrorManager.PACKS_ERRORS, "pack_requirements", ErrorLevel.FATAL, "pack_version", "Bad CompatibleWithLoaderVersions property", e);
                compatibleLoaderVersions = "";
            }
        }
        if (!StringUtils.isNullOrEmpty(dcFileVersion) && packType.isCompressed()) {
            if (!dcFileVersion.equalsIgnoreCase(DynamXConstants.DC_FILE_VERSION)) {
                //DynamXMain.log.warn("Outdated content pack " + getFixedPackName() + " found. Compatible with dc files version " + dcFileVersion);
                DynamXErrorManager.addError(getFixedPackName(), DynamXErrorManager.PACKS_ERRORS, "pack_requirements", ErrorLevel.HIGH, "pack_dc_version", "The model files are compiled for version " + dcFileVersion + " of the DynamX's .dc file loader (currently in version " + DynamXConstants.DC_FILE_VERSION + "). The pack will take more time to load.", null, 600);
            }
        }
        for (RequiredAddonInfo addonInfo : requiredAddons) {
            if (hasLinkedErrors && !AddonLoader.isAddonLoaded(addonInfo.addonId)) {
                DynamXMain.log.error("Addon " + addonInfo.addonId + " is missing for content pack " + getFixedPackName());
                DynamXErrorManager.addError(getFixedPackName(), DynamXErrorManager.PACKS_ERRORS, "pack_requirements", ErrorLevel.FATAL, "pack_addon_dependencies", "This pack requires the addon " + addonInfo.addonId + " in order to be loaded", null, 700);
            }
            if (!StringUtils.isNullOrEmpty(addonInfo.versions) && AddonLoader.isAddonLoaded(addonInfo.addonId)) {
                try { //Check format of the version specs
                    VersionRange range = VersionRange.createFromVersionSpec(addonInfo.versions);
                    if (hasLinkedErrors && !range.containsVersion(new DefaultArtifactVersion(AddonLoader.getAddons().get(addonInfo.addonId).getVersion()))) {
                        //DynamXMain.log.warn("Outdated content pack " + getFixedPackName() + " found, compatible with versions of addon " + addonInfo.addonId + " : " + compatibleLoaderVersions);
                        DynamXErrorManager.addError(getFixedPackName(), DynamXErrorManager.PACKS_ERRORS, "pack_requirements", ErrorLevel.LOW, "pack_addon_dependencies", "This pack is made for versions " + addonInfo.versions + " of the addon " + addonInfo.addonId + " (currently in version " + AddonLoader.getAddons().get(addonInfo.addonId).getVersion() + ")", null, 600);
                    }
                } catch (InvalidVersionSpecificationException e) {
                    //DynamXMain.log.fatal("Invalid Versions in declaration of required addon " + addonInfo.getFullName(), e);
                    DynamXErrorManager.addError(getFixedPackName(), DynamXErrorManager.PACKS_ERRORS, "pack_requirements", ErrorLevel.FATAL, "pack_addon_dependencies", "Bad required addon " + addonInfo.getFullName() + " Versions syntax in pack_info", e);
                    addonInfo.versions = "";
                }
            }
        }
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
        return "pack_info in " + packType.name();
    }

    @Override
    public String getPackName() {
        return originalPackName;
    }

    public InputStream readFile(ResourceLocation file) throws IOException {
        InputStream result = null;
        ModProtectionContainer container = ContentPackLoader.getProtectedResources(getPathName());
        if (container.getSecureLoader() != null) {
            InputStream resourceStream = container.getSecureLoader().getResourceAsStream("assets/" + file.getNamespace() + "/" + file.getPath());
            if (resourceStream != null)
                return resourceStream;
        }
        switch (getPackType()) {
            case FOLDER:
                String fullPath = DynamXMain.resourcesDirectory + File.separator + getPathName() + File.separator + "assets" +
                        File.separator + file.getNamespace() + File.separator + file.getPath().replace("/", File.separator);
                File f = new File(fullPath);
                if(f.exists())
                    result = Files.newInputStream(f.toPath());
                break;
            case DNXPACK:
            case ZIP:
                ZipFile root = new ZipFile(DynamXMain.resourcesDirectory + File.separator + getPathName());
                ZipEntry e = root.getEntry("assets/" + file.getNamespace() + "/" + file.getPath());
                if(e != null)
                    result = root.getInputStream(e);
                else //if not found, close now, else we close 'result' later
                    root.close();
                break;
            case BUILTIN:
                String entry = "/assets/" + file.getNamespace() + "/" + file.getPath();
                result = ContentPackType.class.getResourceAsStream(entry);
                break;
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PackInfo packInfo = (PackInfo) o;
        return Objects.equals(fixedPackName, packInfo.fixedPackName) && Objects.equals(pathName, packInfo.pathName) && packType == packInfo.packType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fixedPackName, pathName, packType);
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
            owner.requiredAddons.add(this);
        }

        @Override
        public String getName() {
            return "Required addon " + name;
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
                ", packName='" + fixedPackName + '\'' +
                ", packType=" + packType +
                ", required=" + requiredAddons +
                ", pathName='" + pathName + '\'' +
                ", packVersion='" + packVersion + '\'' +
                ", compatibleLoaderVersions='" + compatibleLoaderVersions + '\'' +
                ", dcFileVersion='" + dcFileVersion + '\'' +
                '}';
    }
}
