package fr.dynamx.common.physics.world;

import fr.dynamx.api.events.PhysicsEvent;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.BoundingBoxPool;
import fr.dynamx.utils.optimization.TransformPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

/**
 * Where all the physics happen <br>
 * Not multithreaded but thread-safe
 */
public class BuiltinPhysicsWorld extends BasePhysicsWorld {
    protected final Thread physicsThread;
    private short serverAfkTime = 0;

    public BuiltinPhysicsWorld(World world, boolean isRemoteWorld) {
        super(world, isRemoteWorld);
        initPhysicsWorld();
        this.physicsThread = Thread.currentThread();

        DynamXMain.log.info("Loading the physics world for the dimension " + world.provider.getDimension());
        MinecraftForge.EVENT_BUS.post(new PhysicsEvent.PhysicsWorldLoad(this));
    }

    @Override
    public void stepSimulation(float deltaTime) {
        Vector3fPool.openPool();
        TransformPool.getPool().openSubPool();
        BoundingBoxPool.getPool().openSubPool();
        {
            //Disable physics simulation
            //Note that minecraft does the same, but with a delay of 300, so it avoids physics while entities are paused
            if (mcWorld.playerEntities.isEmpty()) {
                if (serverAfkTime < 200) {
                    serverAfkTime++;
                }
            } else {
                serverAfkTime = 0;
            }

            if (serverAfkTime < 200) {
                stepSimulationImpl(Profiler.get(), null);
            } else {
                flushOperations(Profiler.get());
            }
        }
        TransformPool.getPool().closeSubPool();
        Vector3fPool.closePool();
        BoundingBoxPool.getPool().closeSubPool();

        Profiler.get().start(Profiler.Profiles.TICK_TERRAIN);
        manager.tickTerrain();
        Profiler.get().end(Profiler.Profiles.TICK_TERRAIN);
    }

    @Override
    public void clearAll() {
        DynamXMain.log.info("Unloading the physics world of the dimension " + mcWorld.provider.getDimension());
        super.clearAll();
    }

    @Override
    public Thread getPhysicsThread() {
        return physicsThread;
    }
}