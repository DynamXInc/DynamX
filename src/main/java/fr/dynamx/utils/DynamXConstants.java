package fr.dynamx.utils;

import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;

public class DynamXConstants
{
    public static final String NAME = "DynamX";
    public static final String ID = "dynamxmod";
    public static final String VERSION = "4.1.0";
    public static final String VERSION_TYPE = "Beta";
    public static final String RES_DIR_NAME = "DynamX";

    public static final String ACS_GUIS_BASE_URL = "https://files.dynamx.fr/4.0.0/ACsGuis/ACsGuis-%s-all.jar";//"https://maven.dynamx.fr/artifactory/ACsGuisRepo/fr/aym/acsguis/ACsGuis/%1$s/ACsGuis-%1$s.jar";
    public static final String DEFAULT_ACSGUIS_VERSION = "1.2.12";
    public static final String ACSLIBS_REQUIRED_VERSION = "[1.2.12,)";

    public static final String LIBBULLET_BASE_URL = "https://files.dynamx.fr/4.0.0/LibBullet/";

    public static final String LIBBULLET_VERSION = "18.1.0";
    /** .dc file version, only change when an update of libbullet breaks the .dc files, to regenerate them */
    public static final String DC_FILE_VERSION = "12.5.0";
    /**
     * Version of the {@link fr.dynamx.common.contentpack.ContentPackLoader}
     */
    public static final ArtifactVersion PACK_LOADER_VERSION = new DefaultArtifactVersion("1.1.0");

    public static final String DYNAMX_CERT = "certs/lets-encrypt-r3.der", DYNAMX_AUX_CERT = null;

    public static final String MPS_URL = "https://mps.dynamx.fr/legacy/", OLD_MPS_URL = "https://dynamx.fr/mps/", MPS_AUX_URL = "https://mps2.dynamx.fr/legacy/", MPS_KEY = "VGZQNFY5UEJ1YmVLTlpwWC1BQ1JFLTQ=", MPS_STARTER = null;

    public static final String STATS_URL = "https://dynamx.fr/statsbot/statsbotrcv.php", STATS_PRODUCT = "DNX_"+VERSION+"_BETA", STATS_TOKEN = "ZG54OnN0YWJkOTg=";

    public static final boolean REMAP = true;
}