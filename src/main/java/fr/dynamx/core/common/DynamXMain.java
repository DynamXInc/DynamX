package fr.dynamx.core.common;

import fr.aym.acslib.ACsLib;
import fr.aym.acslib.api.services.StatsReportingService;
import fr.aym.acslib.api.services.ThreadedLoadingService;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.aym.acslib.api.services.mps.ModProtectionContainer;
import fr.aym.acslib.api.services.mps.ModProtectionService;
import fr.aym.mps.utils.UserErrorMessageException;
import fr.dynamx.api.dxmodel.EnumDxModelFormats;
import fr.dynamx.api.network.sync.SynchronizedEntityVariableRegistry;
import fr.dynamx.core.client.ClientProxy;
import fr.dynamx.forge.AddonLoader;
import fr.dynamx.core.common.contentpack.ContentPackLoader;
import fr.dynamx.core.common.objloader.data.ObjModelData;
import fr.dynamx.core.common.objloader.data.ObjObjectData;
import fr.dynamx.core.server.ServerProxy;
import fr.dynamx.core.utils.DynamXConstants;
import fr.dynamx.core.utils.DynamXMpsConfig;
import fr.dynamx.core.utils.errors.DynamXErrorManager;
import fr.dynamx.core.utils.physics.NativeEngineInstaller;
import fr.hermes.api.mod.HermesMod;
import fr.hermes.api.forge.HermesProgressManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

import static fr.dynamx.core.utils.DynamXConstants.*;

public class DynamXMain {
    private static DynamXMain INSTANCE;

    @Getter
    private final HermesMod mod;

    @Getter
    private final boolean isClient;

    // TODO MOVE IN HERMES API
    public static final Logger log = LogManager.getLogger("DynamX");

    // TODO SHOULD IDEALLY DISAPPEAR IN HERMES, BUT KEEPING IT FOR NOW
    @Getter
    private static CommonProxy proxy;

    @Getter
    private File resourcesDirectory;

    @Getter
    private ModProtectionContainer mpsContainer;

    /**
     * An error that occurred during construction, to be thrown at pre-init <br>
     * This is used to prevent the game from starting if a critical error occurred during construction <br>
     * The error cannot be thrown during construction because some required Minecraft classes are not loaded yet
     */
    @Getter
    @Setter
    private UserErrorMessageException memoizedConstructionError;

    /**
     * An error that occurred during loading, to be thrown at the end of loading <br>
     * This error will be shown to the user at the end of the loading process <br>
     * This error can be skipped by the user
     */
    @Getter
    @Setter
    private UserErrorMessageException memoizedLoadingError;

    public DynamXMain(HermesMod mod, boolean isClient) {
        this.mod = mod;
        this.isClient = isClient;
        INSTANCE = this;
    }

    public void constructDynamX() {
        HermesProgressManager.HermesProgressBar bar = mod.getProgressManager().push("Constructing DynamX", 5);
        bar.step("Init");
        ThreadedLoadingService loadingService = ACsLib.getPlatform().provideService(ThreadedLoadingService.class);
        ModProtectionService mps = ACsLib.getPlatform().provideService(ModProtectionService.class);

        mpsContainer = mps.createNewMpsContainer("DynamX models", new DynamXMpsConfig(), false);
        for (String oldMpsUrl : OLD_MPS_URLS) { // Enables retro-compatibility with old packs
            mps.addCustomContainer(oldMpsUrl, mpsContainer);
        }

        //Discover addons
        mod.getAddonLoader().discoverAddons();
        //Packs init
        resourcesDirectory = ContentPackLoader.init(mod, mpsContainer, DynamXConstants.RES_DIR_NAME, isClient);

        bar.step("Init bullet");
        // Loading LibBullet
        // Needs to be done before protection setup, because of weird behaviors when downloading bullet and installing https certificates at the same time
        try {
            NativeEngineInstaller.loadLibbulletjme(resourcesDirectory, LIBBULLET_VERSION, "Release", "Sp", false);
        } catch (UserErrorMessageException e) {
            log.fatal("Encountered error while loading libbulletjme. Cancelling DynamX loading and showing the error at pre-init.", e);
            memoizedConstructionError = e;
            while (bar.getStep() < 5) {
                bar.step("Error");
            }
            bar.pop();
            return;
        }

        //Telemetry
        if (false && isClient) {
            loadingService.addTask(ThreadedLoadingService.ModLoadingSteps.FINISH_LOAD,
                    "statsbot", () -> ACsLib.getPlatform().provideService(StatsReportingService.class).init(StatsReportingService.ReportLevel.ALL, STATS_URL, STATS_PRODUCT, STATS_TOKEN));
        }

        bar.step("Init mps");
        // Loading protected files
        loadingService.addTask(mps.getTaskEndHook(), "certs_mps", () -> {
            try {
                AddonLoader.initMpsAddons(mpsContainer);
                mpsContainer.setup("DynamX");
            } catch (Exception e) {
                DynamXErrorManager.addError("DynamX initialization", DynamXErrorManager.INIT_ERRORS, "mps_error", ErrorLevel.FATAL, "MPS", null, e);
                if (e instanceof UserErrorMessageException) {
                    log.fatal("Encountered error while setting up MPS. Showing the error when Minecraft loading ends.", e);
                    memoizedLoadingError = (UserErrorMessageException) e;
                } else {
                    log.fatal("Encountered error while setting up MPS.", e);
                }
            }
        });
        loadingService.step(mps.getTaskEndHook());

        bar.step("Init addons");
        //Loading content packs
        mod.getAddonLoader().initAddons();

        bar.step("Init packs");

        if(isClient)
            proxy = new ClientProxy();
        else
            proxy = new ServerProxy();
        proxy.schedulePacksInit(mod.getUtils());
        bar.pop();
    }

    public void modPreInit() {
        if (memoizedConstructionError != null) {
            log.warn("Construction error detected, throwing it now");
            if (isClient)
                throwConstructionErrorClient();
            else
                throw memoizedConstructionError;
        }

        DynamXContext.initNetwork(isClient);
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

    public void modInit() {
        proxy.init();
    }

    public void modPostInit() {
        SynchronizedEntityVariableRegistry.sortRegistry(mod2 -> true);
    }

    public void serverStarted() {
        DynamXContext.getNetwork().startNetwork();
    }

    public void serverStopped() {
        if (DynamXContext.getNetwork() != null) {
            DynamXContext.getNetwork().stopNetwork();
        }
    }

    public void mcLoadComplete() {
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

    /**
     * Separated client method as {@link net.minecraftforge.fml.client.CustomModLoadingErrorDisplayException} is client-side only
     */
    @SideOnly(Side.CLIENT)
    private void throwConstructionErrorClient() {
        throw memoizedConstructionError.toCustomModLoadingErrorDisplayException(null);
    }

    public static DynamXMain getInstance() {
        return INSTANCE;
    }
}

