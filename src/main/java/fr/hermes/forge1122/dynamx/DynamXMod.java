package fr.hermes.forge1122.dynamx;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.network.sync.SynchronizedEntityVariableRegistry;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.AddonInfo;
import fr.dynamx.common.items.tools.ItemRagdoll;
import fr.dynamx.common.items.tools.ItemShockWave;
import fr.dynamx.common.items.tools.ItemSlopes;
import fr.dynamx.server.command.DynamXCommands;
import fr.dynamx.utils.DynamXReflection;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.hermes.core.HermesMod;
import fr.hermes.core.HermesProgressManager;
import fr.hermes.core.HermesUtils;
import fr.hermes.dynamx.IAddonLoader;
import fr.hermes.forge1122.HmProgressManager;
import fr.hermes.forge1122.HmUtils;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;

import static fr.dynamx.utils.DynamXConstants.*;

@Mod(modid = ID, name = NAME, version = VERSION, updateJSON = "https://dynamx.fr/mps/updates.json", dependencies = "required-after:acslib@" + ACSLIBS_REQUIRED_VERSION)
public class DynamXMod implements HermesMod {
    @Mod.Instance(value = ID)
    public static DynamXMain instance;

    private final HermesProgressManager progressManager = new HmProgressManager();
    private final AddonLoader addonLoader = new AddonLoader();
    private final HermesUtils utils = new HmUtils();

    @Mod.EventHandler
    public void construction(FMLConstructionEvent event) {
        DynamXMain.log.info(NAME + " version " + VERSION + " (pack loader version " + PACK_LOADER_VERSION.getVersionString() + ") is running, by Yanis and Aym'");
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
        event.registerServerCommand(new DynamXCommands());
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
