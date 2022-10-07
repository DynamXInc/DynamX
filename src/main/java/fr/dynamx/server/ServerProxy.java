package fr.dynamx.server;

import fr.aym.acslib.ACsLib;
import fr.aym.acslib.api.services.ThreadedLoadingService;
import fr.dynamx.api.network.sync.PhysicsEntityNetHandler;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.common.CommonProxy;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.server.network.ServerPhysicsEntityNetHandler;
import fr.dynamx.utils.DynamXLoadingTasks;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.SERVER)
public class ServerProxy extends CommonProxy {
    @Override
    public <T extends AbstractEntityPhysicsHandler<?, ?>> PhysicsEntityNetHandler<? extends PhysicsEntity<T>> getNetHandlerForEntity(PhysicsEntity<T> tPhysicsEntity) {
        return new ServerPhysicsEntityNetHandler(tPhysicsEntity);
    }

    @Override
    public boolean ownsSimulation(PhysicsEntity<?> entity) {
        return entity.getSynchronizer().getSimulationHolder() == SimulationHolder.SERVER;
    }

    @Override
    public void scheduleTask(World mcWorld, Runnable task) {
        mcWorld.getMinecraftServer().addScheduledTask(task);
    }

    @Override
    public void schedulePacksInit() {
        ThreadedLoadingService loadingService = ACsLib.getPlatform().provideService(ThreadedLoadingService.class);
        loadingService.addTask(ThreadedLoadingService.ModLoadingSteps.BLOCK_REGISTRY, "packsload", () -> {
            Vector3fPool.openPool(); //Open a pool for the loading of entities
            DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.MC_INIT, DynamXLoadingTasks.PACK);

            //Must follow addons init
            loadingService.addTask(ThreadedLoadingService.ModLoadingSteps.INIT, "proxy preinit", this::preInit);
        });
    }
}
