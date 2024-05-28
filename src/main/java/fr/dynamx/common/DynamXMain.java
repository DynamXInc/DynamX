package fr.dynamx.common;

import fr.aym.acslib.ACsLib;
import fr.aym.acslib.api.services.StatsReportingService;
import fr.aym.acslib.api.services.ThreadedLoadingService;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.aym.acslib.api.services.mps.ModProtectionContainer;
import fr.aym.acslib.api.services.mps.ModProtectionService;
import fr.dynamx.api.dxmodel.EnumDxModelFormats;
import fr.dynamx.api.network.sync.SynchronizedEntityVariableRegistry;
import fr.dynamx.client.ClientProxy;
import fr.hermes.forge1122.dynamx.AddonLoader;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.objloader.data.ObjModelData;
import fr.dynamx.common.objloader.data.ObjObjectData;
import fr.dynamx.server.ServerProxy;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXMpsConfig;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.physics.NativeEngineInstaller;
import fr.hermes.core.HermesMod;
import fr.hermes.core.HermesProgressManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

import static fr.dynamx.utils.DynamXConstants.*;

public class DynamXMain {
    private static CommonProxy proxy;

    public static File resourcesDirectory;

    public static ModProtectionContainer mpsContainer;

    public static final Logger log = LogManager.getLogger("DynamX");

    public static void constructDynamX(HermesMod mod, boolean isClient) {
        HermesProgressManager.HermesProgressBar bar = mod.getProgressManager().push("Constructing DynamX", 5);
        bar.step("Init");
        ThreadedLoadingService loadingService = ACsLib.getPlatform().provideService(ThreadedLoadingService.class);
        ModProtectionService mps = ACsLib.getPlatform().provideService(ModProtectionService.class);

        mpsContainer = mps.createNewMpsContainer("DynamX models", new DynamXMpsConfig(), false);
        mps.addCustomContainer(OLD_MPS_URL, mpsContainer); // Enables retro-compatibility with old packs

        //Discover addons
        mod.getAddonLoader().discoverAddons();
        //Packs init
        resourcesDirectory = ContentPackLoader.init(mpsContainer, DynamXConstants.RES_DIR_NAME);

        bar.step("Init bullet");
        // Loading LibBullet
        // Needs to be done before protection setup, because of weird behaviors when downloading bullet and installing https certificates at the same time
        if (!NativeEngineInstaller.loadLibbulletjme(resourcesDirectory, LIBBULLET_VERSION, "Release", "Sp", false))
            throw new RuntimeException("Native physics engine cannot be found or installed !");

        //Telemetry
        if (false && isClient) {
            loadingService.addTask(ThreadedLoadingService.ModLoadingSteps.FINISH_LOAD,
                    "statsbot", () -> ACsLib.getPlatform().provideService(StatsReportingService.class).init(StatsReportingService.ReportLevel.ALL, STATS_URL, STATS_PRODUCT, STATS_TOKEN));
        }

        bar.step("Init mps");
        // Loading protected files
        loadingService.addTask(mps.getTaskEndHook(), "certs_mps", () -> {
            try {
                mpsContainer.setup("DynamX");
            } catch (Exception e) {
                DynamXErrorManager.addError("DynamX initialization", DynamXErrorManager.INIT_ERRORS, "mps_error", ErrorLevel.FATAL, "MPS", null, e);
                e.printStackTrace();
            }
        });

        bar.step("Init addons");
        //Loading content packs
        mod.getAddonLoader().initAddons();

        bar.step("Init packs");

        loadingService.step(mps.getTaskEndHook());
        if(isClient)
            proxy = new ClientProxy();
        else
            proxy = new ServerProxy();
        proxy.schedulePacksInit();
        bar.pop();
    }

    public static void modPreInit(HermesMod mod) {
        DynamXContext.initNetwork();
        /* Registering entities*/
        //TODO ENTITY REGISTRY
        /*EntityRegistry.registerModEntity(new ResourceLocation(DynamXConstants.ID, "entity_car"), CarEntity.class, "entity_car", 102, this, 200, 4, false);
        EntityRegistry.registerModEntity(new ResourceLocation(DynamXConstants.ID, "entity_trailer"), TrailerEntity.class, "entity_trailer", 105, this, 200, 4, false);
        EntityRegistry.registerModEntity(new ResourceLocation(DynamXConstants.ID, "entity_prop"), PropsEntity.class, "entity_prop", 106, this, 200, 40, false);
        EntityRegistry.registerModEntity(new ResourceLocation(DynamXConstants.ID, "entity_boat"), BoatEntity.class, "entity_boat", 107, this, 200, 4, false);
        EntityRegistry.registerModEntity(new ResourceLocation(DynamXConstants.ID, "entity_ragdoll"), RagdollEntity.class, "entity_ragdoll", 108, this, 200, 4, false);
        EntityRegistry.registerModEntity(new ResourceLocation(DynamXConstants.ID, "entity_door"), DoorEntity.class, "entity_door", 109, this, 200, 4, false);
        EntityRegistry.registerModEntity(new ResourceLocation(DynamXConstants.ID, "entity_helico"), HelicopterEntity.class, "entity_helico", 110, this, 200, 4, false);
        //TODO TEST UPDATE FREQUENCY
        EntityRegistry.registerModEntity(new ResourceLocation(DynamXConstants.ID, "entity_seat"), SeatEntity.class, "entity_seat", 111, this, 164, 80, false);*/

        /* Registering gui handler */
        //TODO NetworkRegistry.INSTANCE.registerGuiHandler(instance, new DynamXGuiHandler());

        //TODO CapabilityManager.INSTANCE.register(DynamXChunkData.class, new DynamXChunkDataStorage(), DynamXChunkData::new);
    }

    public static void modInit(HermesMod mod) {
        proxy.init();
    }

    public static void modPostInit(HermesMod mod) {
        SynchronizedEntityVariableRegistry.sortRegistry(mod2 -> true);
    }

    public static void serverStarted(HermesMod mod) {
        DynamXContext.getNetwork().startNetwork();
    }

    public static void serverStopped(HermesMod mod) {
        if (DynamXContext.getNetwork() != null) {
            DynamXContext.getNetwork().stopNetwork();
        }
    }

    public static void mcLoadComplete(HermesMod mod, boolean isClient) {
        //TODO proxy.completeInit();
        DynamXErrorManager.printErrors(isClient, !isClient ? ErrorLevel.ADVICE : ErrorLevel.HIGH);
        if (!isClient) {
            log.info("Clearing obj model data cache...");
            DynamXContext.getDxModelDataCache().values()
                    .stream()
                    .filter(dxModelData -> dxModelData.getFormat().equals(EnumDxModelFormats.OBJ))
                    .map(dxModelData -> (ObjModelData) dxModelData)
                    .forEach(model -> model.getObjObjects().forEach(ObjObjectData::clearData));
        }
    }
}

