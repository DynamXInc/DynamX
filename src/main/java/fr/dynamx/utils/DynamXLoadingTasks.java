package fr.dynamx.utils;

import fr.aym.acsguis.api.ACsGuiApi;
import fr.aym.acslib.ACsLib;
import fr.aym.acslib.api.services.ThreadedLoadingService;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.contentpack.sync.MessagePacksHashs;
import fr.dynamx.common.contentpack.sync.PackSyncHandler;
import fr.dynamx.common.network.packets.MessageSyncConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Handles async loading, and reloading, of DynamX resources (packs, models, css)
 */
public class DynamXLoadingTasks {
    public static Consumer<TaskContext> PACK = new Consumer<TaskContext>() {
        @Override
        public void accept(TaskContext taskContext) {
            ContentPackLoader.reload(DynamXMain.resourcesDirectory, taskContext != TaskContext.CLIENT || taskContext.isSinglePlayer());
            if (taskContext.isClient()) //Dedicated server
            {
                DynamXContext.getDxModelRegistry().getItemRenderer().refreshItemInfos();
                if (taskContext == TaskContext.CLIENT && !taskContext.isSinglePlayer() && DynamXConfig.syncPacks) {
                    DynamXMain.log.debug("Requesting pack sync...");
                    DynamXContext.getNetwork().sendToServer(new MessagePacksHashs(PackSyncHandler.getObjects()));
                } else if(taskContext != TaskContext.MC_INIT) {
                    DynamXUtils.hotswapWorldPackInfos(DynamXMain.proxy.getClientWorld());
                    if (taskContext.isSinglePlayer())
                        DynamXUtils.hotswapWorldPackInfos(DynamXMain.proxy.getServerWorld());
                }
            } else if (taskContext == TaskContext.SERVER_RUNNING) {
                DynamXContext.getNetwork().sendToClient(new MessageSyncConfig(true, DynamXConfig.mountedVehiclesSyncTickRate, ContentPackLoader.getBlocksGrip(), ContentPackLoader.slopes, ContentPackLoader.SLOPES_LENGTH, ContentPackLoader.PLACE_SLOPES, DynamXContext.getPhysicsSimulationMode(Side.CLIENT)), EnumPacketTarget.ALL);
                for(World w : FMLCommonHandler.instance().getMinecraftServerInstance().worlds) {
                    DynamXUtils.hotswapWorldPackInfos(w);
                }
            }
        }

        @Override
        public String toString() {
            return "packs";
        }
    };
    public static Consumer<TaskContext> MODEL = new Consumer<TaskContext>() {
        @Override
        public void accept(TaskContext taskContext) {
            DynamXContext.getDxModelRegistry().reloadModels();
        }

        @Override
        public String toString() {
            return "models";
        }
    };
    ;
    public static Consumer<TaskContext> CSS = new Consumer<TaskContext>() {
        @Override
        public void accept(TaskContext taskContext) {
            ACsGuiApi.reloadCssStyles(null);
            DynamXLoadingTasks.endTask(CSS);
        }

        @Override
        public String toString() {
            return "css";
        }
    };
    ;

    private static final Queue<Runnable> tasks = new ArrayDeque<>();
    private static final Map<Consumer<TaskContext>, CompletableFuture<Void>> reloadCallbacks = new HashMap<>();


    /**
     * Will reload the given resource types <br>
     * Synchronous during minecraft loading, or on the next tick in mc thread, if mc is running <br>
     * Async load permits to update the gui saying "reloading has started" <br>
     * NOTE : the resource reload can be async itself, but it depends on the resource
     *
     * @param context The reload context
     * @param items   The resources to reload
     * @return A future that will be completed when specified resources have been reloaded
     */
    public static CompletableFuture<Void> reload(TaskContext context, Consumer<TaskContext>... items) {
        CompletableFuture<Void>[] futures = new CompletableFuture[items.length];
        for (int i = 0; i < items.length; i++) {
            Consumer<TaskContext> task = items[i];
            if (!reloadCallbacks.containsKey(task)) {
                DynamXMain.log.info("Reloading " + task);
                futures[i] = new CompletableFuture<>();
                reloadCallbacks.put(task, futures[i]);
                if (ACsLib.getPlatform().provideService(ThreadedLoadingService.class).mcLoadingFinished())
                    tasks.offer(() -> task.accept(context));
                else
                    task.accept(context);
            } else {
                futures[i] = reloadCallbacks.get(task);
                DynamXMain.log.warn("Skipping reload of " + task + " : already reloading");
            }
        }
        return CompletableFuture.allOf(futures);
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
     * Notifies that task has ended, useful for async tasks <br>
     * Should be fired for each resource type, on reload end
     */
    public static void endTask(Consumer<TaskContext> type) {
        if (!reloadCallbacks.containsKey(type))
            throw new IllegalStateException("Reloading of " + type + " isn't running");
        DynamXMain.log.info("Done reloading " + type);
        reloadCallbacks.remove(type).complete(null);
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
