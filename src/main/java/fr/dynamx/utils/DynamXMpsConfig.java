package fr.dynamx.utils;

import fr.aym.acslib.api.services.mps.ModProtectionConfig;
import fr.aym.acslib.api.services.mps.ModProtectionContainer;
import fr.aym.mps.core.BasicMpsConfig;
import fr.aym.mps.utils.MpsUrlFactory;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;

import static fr.dynamx.utils.DynamXConstants.*;

public class DynamXMpsConfig extends BasicMpsConfig {
    public DynamXMpsConfig() {
        super(VERSION, MPS_KEY, null, new DynamXMpsUrlFactory(MPS_URL, MPS_AUX_URLS), new String[0], MPS_STARTER);
    }

    public static class DynamXMpsUrlFactory extends MpsUrlFactory.DefaultUrlFactory {
        public DynamXMpsUrlFactory(String mainUrl, String[] auxUrls) {
            super(mainUrl, auxUrls);
        }

        public String getPatchedMainUrl() {
            return MPS_URL_PATCHER.apply(this.getMainUrl());
        }

        @Override
        public URL getHomeUrl(ModProtectionConfig config, int attempt) throws MalformedURLException {
            String mpsURL = attempt > -1 ? getAuxUrls()[attempt] : getPatchedMainUrl();
            return new URL(mpsURL + config.getMpsVersion() + "/home.php?access_key=" + config.getMpsAccessKey());
        }

        @Override
        public URL getResourceUrl(ModProtectionConfig config, int attempt, String encodedModVersion, String resource, @Nullable ModProtectionContainer.CustomRepoParams params) throws MalformedURLException {
            if (params != null) {
                if (attempt == -1)
                    return new URL(MPS_URL_PATCHER.apply(params.getDomain()) + resource);
                return new URL(params.getDomain().replace(getMainUrl(), getAuxUrls()[attempt]) + resource); //apply aux url
            }
            String mpsURL = (attempt > -1 ? getAuxUrls()[attempt] : getPatchedMainUrl());
            return new URL(mpsURL + config.getMpsVersion() + "/router.php?mod_version=" + encodedModVersion + "&target=" + resource);
        }
    }
}
