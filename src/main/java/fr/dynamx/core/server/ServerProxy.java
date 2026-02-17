package fr.dynamx.core.server;

import fr.aym.acslib.ACsLib;
import fr.aym.acslib.api.services.ThreadedLoadingService;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.core.common.network.sync.PhysicsEntitySynchronizer;
import fr.dynamx.core.common.CommonProxy;
import fr.dynamx.core.common.entities.PhysicsEntity;
import fr.dynamx.core.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.core.server.network.ServerPhysicsEntitySynchronizer;
import fr.dynamx.core.utils.DynamXLoadingTasks;
import fr.dynamx.core.utils.optimization.SubClassPool;
import fr.hermes.api.mc.world.HmServerWorld;
import fr.hermes.api.mc.world.HmWorld;
import fr.hermes.api.platform.HermesPlatform;
import fr.hermes.api.mod.HermesUtils;
import fr.dynamx.core.utils.optimization.JmeVector3fPool;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.SERVER)
public class ServerProxy extends CommonProxy {
    @Override
    public <T extends AbstractEntityPhysicsHandler<?, ?>> PhysicsEntitySynchronizer<? extends PhysicsEntity<T>> getNetHandlerForEntity(PhysicsEntity<T> tPhysicsEntity) {
        return new ServerPhysicsEntitySynchronizer<>(tPhysicsEntity);
    }

    @Override
    public boolean ownsSimulation(PhysicsEntity<?> entity) {
        return entity.getSynchronizer().getSimulationHolder() == SimulationHolder.SERVER;
    }

    @Override
    public void scheduleTask(HmWorld mcWorld, Runnable task) {
        ((HmServerWorld) mcWorld).hm$getServer().hm$addScheduledTask(task);
    }

    @Override
    public void schedulePacksInit(HermesUtils utils) {
        ThreadedLoadingService loadingService = ACsLib.getPlatform().provideService(ThreadedLoadingService.class);
        loadingService.addTask(ThreadedLoadingService.ModLoadingSteps.BLOCK_REGISTRY, "packsload", () -> {
            JmeVector3fPool.openPool(SubClassPool.PACK_MODEL_LOAD); //Open a pool for the loading of entities
            DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.MC_INIT, DynamXLoadingTasks.PACK);
            JmeVector3fPool.closePool();

            //Must follow addons init
            loadingService.addTask(ThreadedLoadingService.ModLoadingSteps.INIT, "proxy preinit", this::preInit);
        });
    }

    @Override
    public boolean isDedicatedServer() {
        return HermesPlatform.getInstance().getServer().hm$isDedicatedServer();
    }
}
