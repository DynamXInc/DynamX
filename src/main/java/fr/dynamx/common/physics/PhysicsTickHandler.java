package fr.dynamx.common.physics;

import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.handlers.TaskScheduler;
import fr.dynamx.common.network.packets.MessageCollisionDebugDraw;
import fr.dynamx.server.command.CmdNetworkConfig;
import fr.dynamx.utils.DynamXLoadingTasks;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.TransformPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

public class PhysicsTickHandler {
    private static long lastTickTimeMs;
    public static final Map<EntityPlayer, Integer> requestedDebugInfo = new HashMap<>();

    @SideOnly(Side.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void tickClient(TickEvent.ClientTickEvent event) {
        if(event.phase == TickEvent.Phase.START) {
            try {
                Profiler.get().start(Profiler.Profiles.TICK);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (canTickClient(Minecraft.getMinecraft())) {
            tickWorldPhysics(event.phase, Minecraft.getMinecraft().world);
        }

        if (event.phase == TickEvent.Phase.START) {
            QuaternionPool.openPool();
            Vector3fPool.openPool();
            DynamXLoadingTasks.tick();
        } else {
            Profiler.get().end(Profiler.Profiles.TICK);
            if (Minecraft.getMinecraft().world != null) {
                boolean profiling = DynamXDebugOptions.PROFILING.isActive();
                if (profiling) {
                    if (DynamXMain.proxy.getTickTime() % 20 == 0) {
                        Profiler.get().printData("Client");
                    }
                }
                Profiler.setIsProfilingOn(profiling);
                Profiler.get().update();
            }

            if (!Minecraft.getMinecraft().isSingleplayer()) {//If not in solo
                TaskScheduler.tick();
            }
            Vector3fPool.closePool();
            QuaternionPool.closePool();
        }
    }

    @SideOnly(Side.CLIENT)
    private boolean canTickClient(Minecraft mc) {
        return mc.world != null && !mc.isGamePaused() && DynamXMain.proxy.shouldUseBulletSimulation(mc.world) && DynamXContext.getPhysicsWorld(mc.world) != null;
    }

    @SubscribeEvent
    public void tickServer(TickEvent.ServerTickEvent event) {
        if(event.phase == TickEvent.Phase.START) {
            try {
                Profiler.get().start(Profiler.Profiles.TICK);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (WorldServer world : FMLCommonHandler.instance().getMinecraftServerInstance().worlds) {
            if (canTickServer(world)) {
                tickWorldPhysics(event.phase, world);
            }
        }


        if (event.phase == TickEvent.Phase.START) {
            QuaternionPool.openPool();
            Vector3fPool.openPool();
            if (FMLCommonHandler.instance().getSide().isServer()) {
                DynamXLoadingTasks.tick();
            }
        } else {
            Profiler.get().end(Profiler.Profiles.TICK);
            sendClientsDebug();
            Profiler.get().update();
            TaskScheduler.tick();
            Vector3fPool.closePool();
            QuaternionPool.closePool();
        }
    }

    private boolean canTickServer(World world) {
        return world != null && DynamXMain.proxy.shouldUseBulletSimulation(world) && DynamXContext.getPhysicsWorld(world) != null;
    }

    private void tickWorldPhysics(TickEvent.Phase phase, World world) {
        IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(world);
        if (phase == TickEvent.Phase.START) {
            //Open Pool
            TransformPool.getPool().openSubPool();
            QuaternionPool.openPool();
            Vector3fPool.openPool();
            physicsWorld.tickStart();
            //System.out.println(">> Tick start");
        } else {

            float deltaTimeSecond = getDeltaTimeMilliseconds() * 1.0E-3F;
            if (deltaTimeSecond > 0.5f) // game was paused ?
                deltaTimeSecond = 0.05f;

            Profiler.get().start(Profiler.Profiles.STEP_SIMULATION);
            physicsWorld.stepSimulation(deltaTimeSecond * 0.01f);
            Profiler.get().end(Profiler.Profiles.STEP_SIMULATION);

            if(physicsWorld.getDynamicsWorld() != null) {
                physicsWorld.getDynamicsWorld().getJointList().forEach(joint -> {
                    if ((joint.getBodyA() != null && !physicsWorld.getDynamicsWorld().contains(joint.getBodyA()))
                            || (joint.getBodyB() != null && !physicsWorld.getDynamicsWorld().contains(joint.getBodyB()))) {
                        physicsWorld.removeJoint(joint);
                    }
                });
            }
            physicsWorld.tickEnd();

            //Close Pool
            Vector3fPool.closePool();
            QuaternionPool.closePool();
            TransformPool.getPool().closeSubPool();
           // System.out.println(">> Tick end");
        }
    }

    private void sendClientsDebug() {
        boolean profiling;
        if (DynamXMain.proxy.getServerWorld().getMinecraftServer().isDedicatedServer()) { //If integrated server, the vars are already shared
            profiling = false;
            boolean networkDebug = false, wheelData = false;
            for (Map.Entry<EntityPlayer, Integer> e : requestedDebugInfo.entrySet()) {
                //Don't spam of debug packets
                if (DynamXMain.proxy.getServerWorld().getMinecraftServer().getTickCounter() % 10 == 0 && (DynamXDebugOptions.BLOCK_BOXES.matchesNetMask(e.getValue()) || DynamXDebugOptions.SLOPE_BOXES.matchesNetMask(e.getValue()))) {
                    DynamXContext.getNetwork().sendToClient(new MessageCollisionDebugDraw(DynamXDebugOptions.BLOCK_BOXES.getDataIn(), DynamXDebugOptions.SLOPE_BOXES.getDataIn()), EnumPacketTarget.PLAYER, (EntityPlayerMP) e.getKey());
                }
                if (DynamXDebugOptions.PROFILING.matchesNetMask(e.getValue())) {
                    profiling = true;
                } else if (DynamXDebugOptions.FULL_NETWORK_DEBUG.matchesNetMask(e.getValue())) {
                    networkDebug = true;
                } else if (DynamXDebugOptions.WHEEL_ADVANCED_DATA.matchesNetMask(e.getValue())) {
                    wheelData = true;
                }
            }
            if (DynamXMain.proxy.getServerWorld().getMinecraftServer().getTickCounter() % 5 == 0) //requestedDebugInfo is sent all 5 ticks
                requestedDebugInfo.clear();
            if (networkDebug != DynamXDebugOptions.FULL_NETWORK_DEBUG.isActive()) {
                //System.out.println("Setting FULL_NETWORK_DEBUG active : " + networkDebug);
                if (networkDebug)
                    DynamXDebugOptions.FULL_NETWORK_DEBUG.enable();
                else
                    DynamXDebugOptions.FULL_NETWORK_DEBUG.disable();
            }
            if (wheelData != DynamXDebugOptions.WHEEL_ADVANCED_DATA.isActive()) {
                //System.out.println("Setting WHEEL_ADVANCED_DATA active : " + wheelData);
                if (wheelData)
                    DynamXDebugOptions.WHEEL_ADVANCED_DATA.enable();
                else
                    DynamXDebugOptions.WHEEL_ADVANCED_DATA.disable();
            }
        } else {
            profiling = DynamXDebugOptions.PROFILING.isActive();
        }
        if (profiling) {
            if (DynamXMain.proxy.getTickTime() % 20 == 0) {
                Profiler.get().printData("Server");
            }
        }
        Profiler.setIsProfilingOn(profiling);
    }

    private float getDeltaTimeMilliseconds() {
        long cur = System.currentTimeMillis();
        long dt = cur - lastTickTimeMs;
        if (false && CmdNetworkConfig.sync_buff)
            if (dt > 51)
                System.out.println("DT is " + dt);
        lastTickTimeMs = cur;
        return dt;
    }
}
