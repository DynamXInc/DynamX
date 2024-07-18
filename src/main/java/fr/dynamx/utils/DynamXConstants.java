package fr.dynamx.utils;

import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;

import java.util.function.Function;

public class DynamXConstants
{
    public static final String NAME = "DynamX";
    public static final String ID = "dynamxmod";
    public static final String VERSION = "4.1.0";
    public static final String VERSION_TYPE = "Beta";
    public static final String RES_DIR_NAME = "DynamX";

    public static final String ACS_GUIS_BASE_URL = "https://v3.mps.dynamx.fr/files/4.0.0/ACsGuis/ACsGuis-%s-all.jar";//"https://maven.dynamx.fr/artifactory/ACsGuisRepo/fr/aym/acsguis/ACsGuis/%1$s/ACsGuis-%1$s.jar";
    public static final String DEFAULT_ACSGUIS_VERSION = "1.2.13";
    public static final String ACSLIBS_REQUIRED_VERSION = "[1.2.12,)";

    public static final String LIBBULLET_BASE_URL = "https://v3.mps.dynamx.fr/files/4.0.0/LibBullet/";

    public static final String LIBBULLET_VERSION = "18.1.0";
    /** .dc file version, only change when an update of libbullet breaks the .dc files, to regenerate them */
    public static final String DC_FILE_VERSION = "12.5.0";
    /**
     * Version of the {@link fr.dynamx.common.contentpack.ContentPackLoader}
     */
    public static final ArtifactVersion PACK_LOADER_VERSION = new DefaultArtifactVersion("1.1.0");

    /**
     * FIXME NOTE <br>
     * For compatibility reasons, the main mps url is still mps.dynamx.fr, but mps dynamically rewrites it to v3.mps.dynamx.fr which has a valid DynamXRootCA certificate <br>
     * This rewrite will stay "hidden" and the new packs still need to be ciphered using the mps.dynamx.fr url <br>
     * The lets encrypt certificate is used for aux mps urls, and can be used for external mps servers. <br>
     * The aux mps is now mps.dynamx.fr, and the other aux url mps2.dynamx.fr currently uses an old certificate valid until September 24.
     */
    public static final String DYNAMX_CERT = "certs/DynamXRootCA.crt", DYNAMX_AUX_CERT = "certs/letsencrypt-isrgrootx1.der";

    public static final String MPS_URL = "https://mps.dynamx.fr/legacy/", OLD_MPS_URL = "https://dynamx.fr/mps/", MPS_KEY = "VGZQNFY5UEJ1YmVLTlpwWC1BQ1JFLTQ=", MPS_STARTER = null;
    public static final String[] MPS_AUX_URLS = new String[]{"https://mps.dynamx.fr/", "https://mps2.dynamx.fr/"};
    public static final Function<String, String> MPS_URL_PATCHER = url -> url.replace("https://mps.dynamx.fr/", "https://v3.mps.dynamx.fr/").replace(OLD_MPS_URL, "https://v3.mps.dynamx.fr/legacy/");

    public static final String STATS_URL = "https://dynamx.fr/statsbot/statsbotrcv.php", STATS_PRODUCT = "DNX_"+VERSION+"_BETA", STATS_TOKEN = "ZG54OnN0YWJkOTg=";

    public static final boolean REMAP = true;
}