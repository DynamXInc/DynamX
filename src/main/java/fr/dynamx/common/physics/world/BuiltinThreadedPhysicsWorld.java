package fr.dynamx.common.physics.world;

import fr.dynamx.api.events.PhysicsEvent;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.BoundingBoxPool;
import fr.dynamx.utils.optimization.TransformPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Where all physics happen <br>
 * Multithreaded and thread-safe
 */
//Important note : this does not include server inactivity check (using player list), so it's only compatible with clients
public class BuiltinThreadedPhysicsWorld extends BasePhysicsWorld implements Runnable {
    private final Thread myThread;

    private final AtomicInteger ticksLate = new AtomicInteger(0);

    private final Semaphore simLock = new Semaphore(1);

    private static int myId;
    private boolean alive;
    private static int crashCount;

    public BuiltinThreadedPhysicsWorld(World world, boolean isRemoteWorld) {
        super(world, isRemoteWorld);
        myThread = new Thread(this);
        myThread.setName("DynamXWorld#" + myId);
        myThread.setPriority(Thread.MAX_PRIORITY);
        myId++;

        myThread.setUncaughtExceptionHandler((t, e) -> {
            crashCount++;
            DynamXMain.log.fatal("DynamX physics thread has crashed, telling to restart !");
            DynamXMain.log.error("Exception : " + e.toString(), e);
            //Refresh pools
            Vector3fPool.closePool();
            TransformPool.getPool().closeSubPool();
            BoundingBoxPool.getPool().closeSubPool();
            //DynamXMain.physicsWorld = new BuiltinThreadedPhysicsWorld(mcWorld); //Create a new simulator

            if (world.getMinecraftServer() != null)
                world.getMinecraftServer().getPlayerList().sendMessage(new TextComponentString("[DynamX] Physics thread has crashed, please restart the server !"));
            else if (world.isRemote)
                sendRestartMsg();
        });
        alive = true;
        DynamXMain.log.info("Loading the threaded physics world for the dimension " + world.provider.getDimension());
        MinecraftForge.EVENT_BUS.post(new PhysicsEvent.PhysicsWorldLoad(this));
        myThread.start();
    }

    @SideOnly(Side.CLIENT)
    private static void sendRestartMsg() {
        ITextComponent msg = new TextComponentString("[DynamX] Physics thread has crashed, please disconnect and reconnect to the server !");
        msg.getStyle().setColor(TextFormatting.DARK_RED);
        Minecraft.getMinecraft().player.sendMessage(msg);
    }

    @Override
    public void run() {
        initPhysicsWorld();
        Profiler profiler = Profiler.get();
        while (alive) {
            if (ticksLate.get() > 0) {
                if (ticksLate.get() > 1) {
                    if (profiler.isActive()) {
                        profiler.printData("Physics thread");
                        profiler.reset();
                    }
                    DynamXMain.log.warn("Server too slow, physics will skip " + (ticksLate.get() - 1) + " simulation ticks !");
                    ticksLate.set(1);
                } else {
                    if (profiler.isActive() && DynamXMain.proxy.getTickTime() % 20 == 0) {
                        profiler.printData("Physics thread");
                        profiler.reset();
                    }
                }
                profiler.start(Profiler.Profiles.STEP_SIMULATION);
                if (ticksLate.get() > 0) {
                    stepSimulationImpl(profiler, simLock);

                    profiler.start(Profiler.Profiles.TICK_TERRAIN);
                    manager.tickTerrain();
                    profiler.end(Profiler.Profiles.TICK_TERRAIN);

                    ticksLate.getAndDecrement();
                }
                profiler.end(Profiler.Profiles.STEP_SIMULATION);
                profiler.update();

            ClientDebugSystem.tickEvent(null);
            }
            if (ticksLate.get() == 0 && alive) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                    //System.out.println("Inter 2 !");
                }
            }
        }
        DynamXMain.log.info("Unloading the threaded physics world of the dimension " + mcWorld.provider.getDimension());
        super.clearAll();
    }

    @Override
    public void stepSimulation(float deltaTime) {
        alive = true;
        ticksLate.getAndIncrement();
        if (myThread.getState() == Thread.State.TIMED_WAITING) { //Thread is in sleep(50), waiting for the next tick
            myThread.interrupt();
        }

        if (crashCount >= 2) {
            throw new RuntimeException("DynamX physics thread has crashed too many times, more info can be found in the log");
        }
    }

    @Override
    public void tickStart() {
        try {
            simLock.acquire();
        } catch (InterruptedException e) {
            DynamXMain.log.error("Client thread was interrupted !", e);
        }
    }

    @Override
    public void tickEnd() {
        simLock.release();
    }

    @Override
    public void clearAll() {
        DynamXMain.log.info("Terminating the physics world");
        alive = false;
        ticksLate.set(0);
        myThread.interrupt();
    }

    @Override
    public Thread getPhysicsThread() {
        return myThread;
    }
}
