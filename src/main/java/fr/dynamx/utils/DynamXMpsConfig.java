package fr.dynamx.utils;

import fr.aym.acslib.api.services.mps.ModProtectionConfig;
import fr.aym.acslib.api.services.mps.ModProtectionContainer;
import fr.aym.acslib.api.services.mps.MpsUrlFactory;
import fr.aym.mps.core.BasicMpsConfig;
import fr.aym.mps.utils.SSLHelper;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;

import static fr.dynamx.utils.DynamXConstants.*;

public class DynamXMpsConfig extends BasicMpsConfig {
    public DynamXMpsConfig() {
        super(VERSION, MPS_KEY, null, new MpsUrlFactory.DefaultUrlFactory(MPS_URL, MPS_AUX_URLS, true), new String[0], MPS_STARTER);
    }
}
