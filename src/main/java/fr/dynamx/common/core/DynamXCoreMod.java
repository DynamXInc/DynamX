package fr.dynamx.common.core;

import fr.aym.acslib.services.impl.stats.core.StatsBotCorePlugin;
import fr.aym.loadingscreen.client.SplashScreenTransformer;
import fr.aym.mps.utils.SSLHelper;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.LibraryInstaller;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
public class DynamXCoreMod implements IFMLLoadingPlugin {
    public static final Logger LOG = LogManager.getLogger("DynamXCoreMod");

    public DynamXCoreMod() {
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.dynamxmod.json");
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{SplashScreenTransformer.class.getName(), StatsBotCorePlugin.class.getName()};
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        StatsBotCorePlugin.runtimeDeobfuscationEnabled = (Boolean) data.get("runtimeDeobfuscationEnabled");
        if (StatsBotCorePlugin.runtimeDeobfuscationEnabled) { //Production
            SSLContext sslContext;
            if (DynamXConstants.DYNAMX_CERT != null || DynamXConstants.DYNAMX_AUX_CERT != null) { // && SSLHelper.shouldInstallCert())
                sslContext = SSLHelper.createCustomSSLContext(DynamXConstants.DYNAMX_CERT, DynamXConstants.DYNAMX_AUX_CERT);
            } else {
                try {
                    sslContext = SSLContext.getDefault();
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException("SSLContext error", e);
                }
            }
            LOG.info("Checking ACsGuis installation...");
            if (!LibraryInstaller.loadACsGuis(LOG, sslContext, new File("mods"), DynamXConstants.DEFAULT_ACSGUIS_VERSION)) {
                LOG.fatal("ACsGuis library cannot be found or installed !");
            }
        } else {
            LOG.info("DevEnv detected, skipping ACsGuis installation check");
        }
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
