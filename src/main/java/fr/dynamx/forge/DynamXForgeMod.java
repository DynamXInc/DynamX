package fr.dynamx.forge;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.network.sync.SynchronizedEntityVariableRegistry;
import fr.dynamx.core.common.DynamXMain;
import fr.dynamx.core.common.contentpack.AddonInfo;
import fr.dynamx.core.common.items.tools.ItemRagdoll;
import fr.dynamx.core.common.items.tools.ItemShockWave;
import fr.dynamx.core.common.items.tools.ItemSlopes;
import fr.dynamx.core.server.command.DynamXServerCommands;
import fr.dynamx.core.utils.DynamXReflection;
import fr.dynamx.core.utils.errors.DynamXErrorManager;
import fr.hermes.api.mod.HermesMod;
import fr.hermes.api.forge.HermesProgressManager;
import fr.hermes.api.mod.HermesUtils;
import fr.dynamx.api.IAddonLoader;
import fr.hermes.forge.ForgeProgressManager;
import fr.hermes.forge.HmUtils;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;

import static fr.dynamx.core.utils.DynamXConstants.*;

@Mod(modid = ID, name = NAME, version = VERSION, updateJSON = "https://dynamx.fr/mps/updates.json", dependencies = "required-after:acslib@" + ACSLIBS_REQUIRED_VERSION)
public class DynamXForgeMod implements HermesMod {
    @Mod.Instance(value = ID)
    public static DynamXMain instance;

    private final HermesProgressManager progressManager = new ForgeProgressManager();
    private final AddonLoader addonLoader = new AddonLoader();
    private final HermesUtils utils = new HmUtils();

    @Mod.EventHandler
    public void construction(FMLConstructionEvent event) {
        DynamXMain.log.info(NAME + " version " + VERSION + "-" + VERSION_TYPE + " (pack loader version {}) is running, by Yanis and Aym'", PACK_LOADER_VERSION.getVersionString());
        addonLoader.setForgeData(event);
        DynamXMain.constructDynamX(this, event.getSide().isClient());
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        /* Loading configuration file */
        DynamXConfig.load(event.getSuggestedConfigurationFile());
        DynamXMain.modPreInit(this);

        new ItemShockWave();
        new ItemSlopes();
        new ItemRagdoll();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        DynamXMain.modInit(this);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        DynamXReflection.initReflection();
        DynamXMain.modPostInit(this);
    }

    @Mod.EventHandler
    public void completeLoad(FMLLoadCompleteEvent event) {
        ForgeVersion.CheckResult result = ForgeVersion.getResult(Loader.instance().activeModContainer());
        if (result.status == ForgeVersion.Status.OUTDATED) {
            //DynamXMain.log.warn("Outdated version found, you should update to " + result.target);
            DynamXErrorManager.addError("DynamX updates", DynamXErrorManager.UPDATES, "updates", ErrorLevel.ADVICE, "DynamX", "Version " + result.target + " disponible");
        } else if (result.status == ForgeVersion.Status.FAILED) {
            DynamXMain.log.warn("Forge failed to check majs for DynamX !");
        }
        DynamXMain.mcLoadComplete(this, event.getSide().isClient());
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new DynamXServerCommands());
    }

    @Mod.EventHandler
    public void onServerStarted(FMLServerStartedEvent event) {
        DynamXMain.serverStarted(this);
    }

    @Mod.EventHandler
    public void stopServer(FMLServerStoppedEvent event) {
        DynamXMain.serverStopped(this);
    }

    @NetworkCheckHandler
    public boolean checkRemote(Map mods, Side side) {
        DynamXMain.log.info("Connecting to " + mods + " on " + side);
        if (side.isClient()) {
            for (AddonInfo info : addonLoader.getAddons().values()) {
                if (info.isRequiredOnClient() && !mods.containsKey(info.getModId())) {
                    DynamXMain.log.fatal("Rejecting connection: Addon not loaded on client : " + info);
                    return false;
                }
            }
        } else {
            SynchronizedEntityVariableRegistry.sortRegistry(mods::containsKey);
        }
        return true;
    }

    @Override
    public HermesProgressManager getProgressManager() {
        return progressManager;
    }

    @Override
    public IAddonLoader getAddonLoader() {
        return addonLoader;
    }

    @Override
    public HermesUtils getUtils() {
        return utils;
    }
}
