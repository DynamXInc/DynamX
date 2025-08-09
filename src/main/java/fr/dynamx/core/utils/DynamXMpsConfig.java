package fr.dynamx.core.utils;

import fr.aym.acslib.api.services.mps.MpsUrlFactory;
import fr.aym.mps.core.BasicMpsConfig;

import static fr.dynamx.core.utils.DynamXConstants.*;

public class DynamXMpsConfig extends BasicMpsConfig {
    public DynamXMpsConfig() {
        super(VERSION, MPS_KEY, null, new MpsUrlFactory.DefaultUrlFactory(MPS_URL, MPS_AUX_URLS, true), new String[0], MPS_STARTER);
    }
}
