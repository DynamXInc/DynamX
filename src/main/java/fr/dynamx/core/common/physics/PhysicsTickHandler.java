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
import fr.hermes.api.events.HmEventPhase;
import fr.hermes.api.mc.client.HmMinecraftClient;
import fr.hermes.api.mc.entities.HmPlayerEntity;
import fr.hermes.api.mc.entities.HmServerPlayerEntity;
import fr.hermes.api.mc.events.HmMcClientEvents;
import fr.hermes.api.mc.events.HmMcServerEvents;
import fr.hermes.api.mc.world.HmServerWorld;
import fr.hermes.api.mc.world.HmWorld;
import fr.hermes.api.mod.HermesPlatform;
import fr.dynamx.core.utils.optimization.JmeVector3fPool;

import java.util.HashMap;
import java.util.Map;

public class PhysicsTickHandler {
    private static long lastTickTimeMs;
    public static final Map<HmPlayerEntity, Integer> requestedDebugInfo = new HashMap<>();

    public static void register()
    {
        HmMcClientEvents.TICK.register(PhysicsTickHandler::tickClient);
        HmMcServerEvents.TICK.register(PhysicsTickHandler::tickServer);
    }

    // note: why was it LOWEST priority @SubscribeEvent(priority = EventPriority.LOWEST)
    private static void tickClient(HmEventPhase phase) {
        if (phase == HmEventPhase.PRE) {
            try {
                Profiler.get().start(Profiler.Profiles.TICK);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (canTickClient(HermesPlatform.getInstance().getClient())) {
            tickWorldPhysics(phase, HermesPlatform.getInstance().getClient().hm$getWorld());
        }

        if (phase == HmEventPhase.PRE) {
            QuaternionPool.openPool(SubClassPool.TICK_CLIENT);
            JmeVector3fPool.openPool(SubClassPool.TICK_CLIENT);
            DynamXLoadingTasks.tick();
        } else {
            Profiler.get().end(Profiler.Profiles.TICK);
            if (HermesPlatform.getInstance().getClient().hm$getWorld() != null) {
                boolean profiling = DynamXDebugOptions.PROFILING.isActive();
                if (profiling) {
                    if (DynamXMain.getProxy().getTickTime() % 20 == 0) {
                        Profiler.get().printData("Client");
                    }
                }
                Profiler.setIsProfilingOn(profiling);
                Profiler.get().update();
            }

            if (!HermesPlatform.getInstance().getClient().hm$isSingleplayer()) {//If not in solo
                TaskScheduler.tick();
            }
            JmeVector3fPool.closePool();
            QuaternionPool.closePool();
        }
    }

    //@SideOnly(Side.CLIENT)
    private static boolean canTickClient(HmMinecraftClient mc) {
        return !mc.hm$isGamePaused() && canTickWorld(mc.hm$getWorld());
    }

    private static void tickServer(HmEventPhase phase) {
        if (phase == HmEventPhase.PRE) {
            QuaternionPool.openPool(SubClassPool.TICK_SERVER);
            JmeVector3fPool.openPool(SubClassPool.TICK_SERVER);
            try {
                Profiler.get().start(Profiler.Profiles.TICK);
            } catch (Exception e) {
                DynamXMain.log.throwing(e);
            }
        }
        for (HmServerWorld world : HermesPlatform.getInstance().getServer().hm$getWorlds()) {
            if (canTickWorld(world)) {
                tickWorldPhysics(phase, world);
            }
        }

        if (phase == HmEventPhase.PRE) {
            if (HermesPlatform.getInstance().getServer().hm$isDedicatedServer()) {
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

    private static boolean canTickWorld(HmWorld world) {
        return world != null && DynamXMain.getProxy().shouldUseBulletSimulation(world) && DynamXContext.getPhysicsWorld(world) != null;
    }

    private static void tickWorldPhysics(HmEventPhase phase, HmWorld world) {
        IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(world);
        if (phase == HmEventPhase.POST) {
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

    private static void sendClientsDebug() {
        boolean profiling;
        if (DynamXMain.getProxy().isDedicatedServer()) { //If integrated server, the vars are already shared
            profiling = false;
            boolean networkDebug = false, wheelData = false;
            for (Map.Entry<HmPlayerEntity, Integer> e : requestedDebugInfo.entrySet()) {
                //Don't spam of debug packets
                if (DynamXMain.getProxy().getTickTime() % 10 == 0 && (DynamXDebugOptions.BLOCK_BOXES.matchesNetMask(e.getValue()) || DynamXDebugOptions.SLOPE_BOXES.matchesNetMask(e.getValue()))) {
                    DynamXContext.getNetwork().sendToClient(new MessageCollisionDebugDraw(DynamXDebugOptions.BLOCK_BOXES.getDataIn(), DynamXDebugOptions.SLOPE_BOXES.getDataIn()),
                            EnumPacketTarget.PLAYER, (HmServerPlayerEntity) e.getKey());
                }
                if (DynamXDebugOptions.PROFILING.matchesNetMask(e.getValue())) {
                    profiling = true;
                } else if (DynamXDebugOptions.FULL_NETWORK_DEBUG.matchesNetMask(e.getValue())) {
                    networkDebug = true;
                } else if (DynamXDebugOptions.WHEEL_ADVANCED_DATA.matchesNetMask(e.getValue())) {
                    wheelData = true;
                }
            }
            if (DynamXMain.getProxy().getTickTime() % 5 == 0) {//requestedDebugInfo is sent all 5 ticks
                requestedDebugInfo.clear();
            }
            if (networkDebug != DynamXDebugOptions.FULL_NETWORK_DEBUG.isActive()) {
                //System.out.println("Setting FULL_NETWORK_DEBUG active : " + networkDebug);
                if (networkDebug) {
                    DynamXDebugOptions.FULL_NETWORK_DEBUG.enable();
                } else {
                    DynamXDebugOptions.FULL_NETWORK_DEBUG.disable();
                }
            }
            if (wheelData != DynamXDebugOptions.WHEEL_ADVANCED_DATA.isActive()) {
                //System.out.println("Setting WHEEL_ADVANCED_DATA active : " + wheelData);
                if (wheelData) {
                    DynamXDebugOptions.WHEEL_ADVANCED_DATA.enable();
                } else {
                    DynamXDebugOptions.WHEEL_ADVANCED_DATA.disable();
                }
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

    private static float getDeltaTimeMilliseconds() {
        long cur = System.currentTimeMillis();
        long dt = cur - lastTickTimeMs;
        lastTickTimeMs = cur;
        return dt;
    }
}
