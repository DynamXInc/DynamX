package fr.dynamx.utils;

import fr.aym.acsguis.api.ACsGuiApi;
import fr.aym.acslib.ACsLib;
import fr.aym.acslib.api.services.ErrorTrackingService;
import fr.aym.acslib.api.services.ThreadedLoadingService;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.contentpack.sync.MessagePacksHashs;
import fr.dynamx.common.contentpack.sync.PackSyncHandler;
import fr.dynamx.common.network.packets.MessageSyncConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;

/**
 * Handles async loading, and reloading, of DynamX resources (packs, models, css) <br>
 * Also provides error types for the {@link ErrorTrackingService}
 */
public class DynamXLoadingTasks {
    /**
     * MAJS error type : notifies of DynamX updates
     */
    public static ErrorTrackingService.ErrorType MAJS = DynamXContext.getErrorTracker().createErrorType(new ResourceLocation(DynamXConstants.ID, "majs"), "Updates");
    /**
     * INIT error type : notifies of DynamX init errors
     */
    public static ErrorTrackingService.ErrorType INIT = DynamXContext.getErrorTracker().createErrorType(new ResourceLocation(DynamXConstants.ID, "init"), "Initialization");
    /**
     * PACK error type : notifies of pack loading errors <br>
     * Reload flag for packs
     */
    public static ErrorTrackingService.ErrorType PACK = DynamXContext.getErrorTracker().createErrorType(new ResourceLocation(DynamXConstants.ID, "pack"), "Packs");
    /**
     * MODEL error type : notifies of model loading errors <br>
     * Reload flag for models
     */
    public static ErrorTrackingService.ErrorType MODEL = DynamXContext.getErrorTracker().createErrorType(new ResourceLocation(DynamXConstants.ID, "model"), "Models");

    private static final BiFunction<ErrorTrackingService.ErrorType, TaskContext, Runnable> executors = (errorType, taskContext) -> {
        if (PACK.equals(errorType)) {
            return () ->
            {
                ContentPackLoader.reload(DynamXMain.resDir, taskContext != TaskContext.CLIENT || taskContext.isSinglePlayer());
                if (taskContext.isClient()) //Dedicated server
                {
                    DynamXContext.getObjModelRegistry().getItemRenderer().refreshItemInfos();
                    if (taskContext == TaskContext.CLIENT && !taskContext.isSinglePlayer() && DynamXConfig.syncPacks) {
                        DynamXMain.log.debug("Requesting pack sync...");
                        DynamXContext.getNetwork().sendToServer(new MessagePacksHashs(PackSyncHandler.getObjects()));
                    }
                } else if (taskContext == TaskContext.SERVER_RUNNING)
                    DynamXContext.getNetwork().sendToClient(new MessageSyncConfig(true, DynamXConfig.mountedVehiclesSyncTickRate, ContentPackLoader.getBlocksGrip(), ContentPackLoader.slopes, ContentPackLoader.SLOPES_LENGTH, ContentPackLoader.PLACE_SLOPES, DynamXContext.getPhysicsSimulationMode(Side.CLIENT)), EnumPacketTarget.ALL);
            };
        } else if (MODEL.equals(errorType)) {
            return () -> DynamXContext.getObjModelRegistry().reloadModels();
        } else if (ACsGuiApi.CSS_ERROR_TYPE.equals(errorType)) {
            return () -> {
                ACsGuiApi.reloadCssStyles(null);
                DynamXLoadingTasks.endTask(ACsGuiApi.CSS_ERROR_TYPE);
            };
        }
        throw new UnsupportedOperationException("Cannot reload " + errorType.getLabel());
    };

    private static final List<ErrorTrackingService.ErrorType> currentTasks = new ArrayList<>();
    private static final Queue<Runnable> tasks = new ArrayDeque<>();
    private static final List<Runnable> reloadCallback = new ArrayList<>();

    /**
     * Will reload the given resource types <br>
     * Synchronous during minecraft loading, or on the next tick in mc thread, if mc is running <br>
     * NOTE : the resource reload can be async itself, but it depends on the resource
     *
     * @param context The reload context
     * @param items   The resources to reload
     */
    public static void reload(TaskContext context, ErrorTrackingService.ErrorType... items) {
        reload(context, null, items);
    }

    /**
     * Will reload the given resource types <br>
     * Synchronous during minecraft loading, or on the next tick in mc thread, if mc is running <br>
     * NOTE : the resource reload can be async itself, but it depends on the resource
     *
     * @param context        The reload context
     * @param reloadCallback A callback to execute after all the given tasks have finished
     * @param items          The resources to reload
     */
    public static void reload(TaskContext context, Runnable reloadCallback, ErrorTrackingService.ErrorType... items) {
        if (reloadCallback != null)
            DynamXLoadingTasks.reloadCallback.add(reloadCallback);
        for (ErrorTrackingService.ErrorType t : items) {
            if (!currentTasks.contains(t)) {
                Runnable r = executors.apply(t, context);
                DynamXMain.log.info("Reloading " + t.getLabel());
                currentTasks.add(t);
                if (ACsLib.getPlatform().provideService(ThreadedLoadingService.class).mcLoadingFinished()) {
                    DynamXMain.log.info("Added to queue");
                    tasks.offer(r);
                } else {
                    DynamXMain.log.info("Running now");
                    r.run();
                }
            } else {
                DynamXMain.log.warn("Skipping reload of " + t.getLabel() + " : already reloading");
            }
        }
    }

    /**
     * Runs pending tasks
     */
    public static void tick() {
        if (!tasks.isEmpty()) {
            tasks.remove().run();
        }
    }

    /**
     * @return True if the given task is running
     */
    public static boolean isTaskRunning(ErrorTrackingService.ErrorType type) {
        return currentTasks.contains(type);
    }

    /**
     * Notifies that task has ended, useful for async tasks <br>
     * Should be fired for each resource type, on reload end
     */
    public static void endTask(ErrorTrackingService.ErrorType type) {
        if (!currentTasks.contains(type))
            throw new IllegalStateException("Task " + type.getLabel() + " not started !");
        DynamXMain.log.info("Finished " + type.getLabel() + " reloading");
        currentTasks.remove(type);
        if (currentTasks.isEmpty()) {
            while (!reloadCallback.isEmpty()) {
                reloadCallback.remove(0).run();
            }
        }
    }

    /**
     * Reload contexts
     */
    public enum TaskContext {
        MC_INIT, SERVER_RUNNING, CLIENT;

        public boolean isClient() {
            return this == CLIENT || FMLCommonHandler.instance().getSide().isClient();
        }

        @SideOnly(Side.CLIENT)
        public boolean isSinglePlayer() {
            return Minecraft.getMinecraft().isSingleplayer();
        }
    }
}
