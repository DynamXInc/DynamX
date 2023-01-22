package fr.dynamx.common;

import fr.aym.acslib.ACsLib;
import fr.aym.acslib.api.services.StatsReportingService;
import fr.aym.acslib.api.services.ThreadedLoadingService;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.aym.acslib.api.services.mps.ModProtectionContainer;
import fr.aym.acslib.api.services.mps.ModProtectionService;
import fr.dynamx.api.network.sync.SynchronizedEntityVariableRegistry;
import fr.dynamx.common.capability.DynamXChunkData;
import fr.dynamx.common.capability.DynamXChunkDataStorage;
import fr.dynamx.common.contentpack.AddonInfo;
import fr.dynamx.common.contentpack.AddonLoader;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.entities.PropsEntity;
import fr.dynamx.common.entities.RagdollEntity;
import fr.dynamx.common.entities.vehicles.*;
import fr.dynamx.common.handlers.DynamXGuiHandler;
import fr.dynamx.common.items.tools.ItemRagdoll;
import fr.dynamx.common.items.tools.ItemShockWave;
import fr.dynamx.common.items.tools.ItemSlopes;
import fr.dynamx.server.command.DynamXCommands;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXMpsConfig;
import fr.dynamx.utils.DynamXReflection;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.physics.NativeEngineInstaller;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Map;

import static fr.dynamx.utils.DynamXConstants.*;
import static net.minecraftforge.fml.common.Mod.EventHandler;
import static net.minecraftforge.fml.common.Mod.Instance;

@Mod(modid = ID, name = NAME, version = VERSION, updateJSON = "https://dynamx.fr/mps/updates.json", dependencies = "required-after:acslib@" + ACSLIBS_REQUIRED_VERSION)
public class DynamXMain {
    @Instance(value = ID)
    public static DynamXMain instance;
    @SidedProxy(clientSide = "fr.dynamx.client.ClientProxy", serverSide = "fr.dynamx.server.ServerProxy")
    public static CommonProxy proxy;

    public static File resDir;

    public static final Logger log = LogManager.getLogger("DynamX");

    public static ModProtectionContainer container;

    @EventHandler
    public void construction(FMLConstructionEvent event) {
        log.info(NAME + " version " + VERSION + " (pack loader version " + PACK_LOADER_VERSION.getVersionString() + ") is running, by Yanis and Aym'");

        ProgressManager.ProgressBar bar = ProgressManager.push("Constructing DynamX", 5);
        bar.step("Init");
        ThreadedLoadingService loadingService = ACsLib.getPlatform().provideService(ThreadedLoadingService.class);
        ModProtectionService mps = ACsLib.getPlatform().provideService(ModProtectionService.class);

        container = mps.createNewMpsContainer("DynamX models", new DynamXMpsConfig(), false);

        //Packs init
        resDir = ContentPackLoader.init(event, container, DynamXConstants.RES_DIR_NAME, event.getSide());

        bar.step("Init bullet");
        // Loading LibBullet
        // Needs to be done before protection setup, because of weird behaviors when downloading bullet and installing https certificates at the same time
        if (!NativeEngineInstaller.loadLibbulletjme(resDir, LIBBULLET_VERSION, "Release", "Sp", false))
            throw new RuntimeException("Native physics engine cannot be found or installed !");

        //Telemetry
        if (event.getSide().isClient()) {
            loadingService.addTask(ThreadedLoadingService.ModLoadingSteps.FINISH_LOAD,
                    "statsbot", () -> ACsLib.getPlatform().provideService(StatsReportingService.class).init(StatsReportingService.ReportLevel.ALL, STATS_URL, STATS_PRODUCT, STATS_TOKEN));
        }

        bar.step("Init mps");
        // Loading protected files
        loadingService.addTask(mps.getTaskEndHook(), "certs_mps", () -> {
            try {
                container.setup("DynamXEA");
            } catch (Exception e) {
                DynamXErrorManager.addError("DynamX initialization", DynamXErrorManager.INIT_ERRORS, "mps_error", ErrorLevel.FATAL, "MPS", null, e);
                e.printStackTrace();
            }
        });

        bar.step("Init addons");
        //Loading content packs
        AddonLoader.initAddons();

        bar.step("Init packs");

        loadingService.step(mps.getTaskEndHook());
        proxy.schedulePacksInit();
        ProgressManager.pop(bar);
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if(!Minecraft.getMinecraft().getFramebuffer().isStencilEnabled()){
            Minecraft.getMinecraft().getFramebuffer().enableStencil();
        }

        /* Loading configuration file */
        DynamXConfig.load(event.getSuggestedConfigurationFile());

        new ItemShockWave();
        new ItemSlopes();
        new ItemRagdoll();
        /* Registering entities*/
        EntityRegistry.registerModEntity(new ResourceLocation(DynamXConstants.ID, "entity_car"), CarEntity.class, "entity_car", 102, this, 200, 4, false);
        EntityRegistry.registerModEntity(new ResourceLocation(DynamXConstants.ID, "entity_caterpillar"), CaterpillarEntity.class, "entity_caterpillar", 104, this, 200, 4, false);
        EntityRegistry.registerModEntity(new ResourceLocation(DynamXConstants.ID, "entity_trailer"), TrailerEntity.class, "entity_trailer", 105, this, 200, 4, false);
        EntityRegistry.registerModEntity(new ResourceLocation(DynamXConstants.ID, "entity_prop"), PropsEntity.class, "entity_prop", 106, this, 200, 40, false);
        //EntityRegistry.registerModEntity(new ResourceLocation(DynamXMain.ID, "entity_boat"), BoatEntity.class, "entity_boat", 107, this, 200, 4, false);
        EntityRegistry.registerModEntity(new ResourceLocation(DynamXConstants.ID, "entity_ragdoll"), RagdollEntity.class, "entity_ragdoll", 108, this, 200, 4, false);
        EntityRegistry.registerModEntity(new ResourceLocation(DynamXConstants.ID, "entity_boat"), BoatEntity.class, "entity_boat", 107, this, 200, 4, false);
        EntityRegistry.registerModEntity(new ResourceLocation(DynamXConstants.ID, "entity_door"), DoorEntity.class, "entity_door", 108, this, 200, 4, false);
        /* Registering gui handler */
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new DynamXGuiHandler());

        CapabilityManager.INSTANCE.register(DynamXChunkData.class, new DynamXChunkDataStorage(), DynamXChunkData::new);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        DynamXReflection.initReflection();
        SynchronizedEntityVariableRegistry.sortRegistry(mod -> true);
    }

    @EventHandler
    public void completeLoad(FMLLoadCompleteEvent event) {
        ForgeVersion.CheckResult result = ForgeVersion.getResult(Loader.instance().activeModContainer());
        if (result.status == ForgeVersion.Status.OUTDATED) {
            //DynamXMain.log.warn("Outdated version found, you should update to " + result.target);
            DynamXErrorManager.addError("DynamX updates", DynamXErrorManager.UPDATES, "updates", ErrorLevel.ADVICE, "DynamX", "Version " + result.target + " disponible");
        } else if (result.status == ForgeVersion.Status.FAILED) {
            DynamXMain.log.warn("Forge failed to check majs for DynamX !");
        }
        DynamXErrorManager.printErrors(event.getSide(), event.getSide().isServer() ? ErrorLevel.ADVICE : ErrorLevel.HIGH);
    }

    @EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new DynamXCommands());
    }

    @EventHandler
    public void onServerStarted(FMLServerStartedEvent event) {
        DynamXContext.getNetwork().startNetwork();
    }

    @Mod.EventHandler
    public void stopServer(FMLServerStoppedEvent event) {
        if (DynamXContext.getNetwork() != null) {
            DynamXContext.getNetwork().stopNetwork();
        }
    }

    @NetworkCheckHandler
    public boolean checkRemote(Map mods, Side side) {
        DynamXMain.log.info("Connecting to " + mods + " on " + side);
        if (side.isClient()) {
            for (AddonInfo info : AddonLoader.getAddons().values()) {
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
}

