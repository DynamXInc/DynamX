package fr.dynamx.core.common.physics;

import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.common.DynamXMain;
import fr.dynamx.core.common.handlers.TaskScheduler;
import fr.dynamx.core.common.network.packets.MessageCollisionDebugDraw;
import fr.dynamx.core.utils.DynamXLoadingTasks;
import fr.dynamx.core.utils.debug.DynamXDebugOptions;
import fr.dynamx.core.utils.debug.Profiler;
import fr.dynamx.core.utils.optimization.QuaternionPool;
import fr.dynamx.core.utils.optimization.SubClassPool;
import fr.hermes.forge.JmeVector3fPool;
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
        if (event.phase == TickEvent.Phase.START) {
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
            QuaternionPool.openPool(SubClassPool.TICK_CLIENT);
            JmeVector3fPool.openPool(SubClassPool.TICK_CLIENT);
            DynamXLoadingTasks.tick();
        } else {
            Profiler.get().end(Profiler.Profiles.TICK);
            if (Minecraft.getMinecraft().world != null) {
                boolean profiling = DynamXDebugOptions.PROFILING.isActive();
                if (profiling) {
                    if (DynamXMain.getProxy().getTickTime() % 20 == 0) {
                        Profiler.get().printData("Client");
                    }
                }
                Profiler.setIsProfilingOn(profiling);
                Profiler.get().update();
            }

            if (!Minecraft.getMinecraft().isSingleplayer()) {//If not in solo
                TaskScheduler.tick();
            }
            JmeVector3fPool.closePool();
            QuaternionPool.closePool();
        }
    }

    @SideOnly(Side.CLIENT)
    private boolean canTickClient(Minecraft mc) {
        return mc.world != null && !mc.isGamePaused() && DynamXMain.getProxy().shouldUseBulletSimulation(mc.world) && DynamXContext.getPhysicsWorld(mc.world) != null;
    }

    @SubscribeEvent
    public void tickServer(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            QuaternionPool.openPool(SubClassPool.TICK_SERVER);
            JmeVector3fPool.openPool(SubClassPool.TICK_SERVER);
            try {
                Profiler.get().start(Profiler.Profiles.TICK);
            } catch (Exception e) {
                DynamXMain.log.throwing(e);
            }
        }
        for (WorldServer world : FMLCommonHandler.instance().getMinecraftServerInstance().worlds) {
            if (canTickServer(world)) {
                tickWorldPhysics(event.phase, world);
            }
        }

        if (event.phase == TickEvent.Phase.START) {
            if (FMLCommonHandler.instance().getSide().isServer()) {
                DynamXLoadingTasks.tick();
            }
        } else {
            Profiler.get().end(Profiler.Profiles.TICK);
            sendClientsDebug();
            Profiler.get().update();
            TaskScheduler.tick();
            JmeVector3fPool.closePool();
            QuaternionPool.closePool();
        }
    }

    private boolean canTickServer(World world) {
        return world != null && DynamXMain.getProxy().shouldUseBulletSimulation(world) && DynamXContext.getPhysicsWorld(world) != null;
    }

    private void tickWorldPhysics(TickEvent.Phase phase, World world) {
        IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(world);
        if (phase == TickEvent.Phase.END) {
            physicsWorld.tickEnd();
            return;
        }
        // START phase
        QuaternionPool.openPool(SubClassPool.TICK_PHYSICS_WORLD);
        JmeVector3fPool.openPool(SubClassPool.TICK_PHYSICS_WORLD);

        physicsWorld.tickStart();

        float deltaTimeSecond = getDeltaTimeMilliseconds() * 1.0E-3F;
        if (deltaTimeSecond > 0.5f) // game was paused ?
            deltaTimeSecond = 0.05f;

        Profiler.get().start(Profiler.Profiles.STEP_SIMULATION);
        physicsWorld.stepSimulation(deltaTimeSecond);
        Profiler.get().end(Profiler.Profiles.STEP_SIMULATION);

        if (physicsWorld.getDynamicsWorld() != null) {
            physicsWorld.getDynamicsWorld().getJointList().forEach(joint -> {
                if ((joint.getBodyA() != null && !physicsWorld.getDynamicsWorld().contains(joint.getBodyA()))
                        || (joint.getBodyB() != null && !physicsWorld.getDynamicsWorld().contains(joint.getBodyB()))) {
                    physicsWorld.removeJoint(joint);
                }
            });
        }

        JmeVector3fPool.closePool();
        QuaternionPool.closePool();
    }

    private void sendClientsDebug() {
        boolean profiling;
        if (DynamXMain.getProxy().getServerWorld().getMinecraftServer().isDedicatedServer()) { //If integrated server, the vars are already shared
            profiling = false;
            boolean networkDebug = false, wheelData = false;
            for (Map.Entry<EntityPlayer, Integer> e : requestedDebugInfo.entrySet()) {
                //Don't spam of debug packets
                if (DynamXMain.getProxy().getServerWorld().getMinecraftServer().getTickCounter() % 10 == 0 && (DynamXDebugOptions.BLOCK_BOXES.matchesNetMask(e.getValue()) || DynamXDebugOptions.SLOPE_BOXES.matchesNetMask(e.getValue()))) {
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
            if (DynamXMain.getProxy().getServerWorld().getMinecraftServer().getTickCounter() % 5 == 0) //requestedDebugInfo is sent all 5 ticks
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
            if (DynamXMain.getProxy().getTickTime() % 20 == 0) {
                Profiler.get().printData("Server");
            }
        }
        Profiler.setIsProfilingOn(profiling);
    }

    private float getDeltaTimeMilliseconds() {
        long cur = System.currentTimeMillis();
        long dt = cur - lastTickTimeMs;
        lastTickTimeMs = cur;
        return dt;
    }
}
