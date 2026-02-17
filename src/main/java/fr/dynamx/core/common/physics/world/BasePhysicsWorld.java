package fr.dynamx.core.common.physics.world;

import com.jme3.bullet.PhysicsSoftSpace;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.api.physics.entities.EntityPhysicsState;
import fr.dynamx.core.client.handlers.ClientDebugSystem;
import fr.dynamx.core.client.network.ClientPhysicsSyncManager;
import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.common.DynamXMain;
import fr.dynamx.core.common.entities.PhysicsEntity;
import fr.dynamx.core.common.physics.CollisionsHandler;
import fr.dynamx.core.common.physics.terrain.PhysicsWorldTerrain;
import fr.dynamx.core.common.physics.utils.PhysicsWorldOperation;
import fr.dynamx.core.server.network.ServerPhysicsSyncManager;
import fr.dynamx.core.utils.PhysicsEntityException;
import fr.dynamx.core.utils.debug.Profiler;
import fr.dynamx.core.utils.optimization.QuaternionPool;
import fr.dynamx.core.utils.optimization.SubClassPool;
import fr.hermes.api.mc.world.HmWorld;
import fr.dynamx.core.utils.optimization.JmeVector3fPool;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for the two DynamX physics worlds
 */
public abstract class BasePhysicsWorld implements IPhysicsWorld {
    protected PhysicsSoftSpace dynamicsWorld;
    protected final PhysicsWorldTerrain manager;
    protected final HmWorld mcWorld;

    protected final Set<PhysicsJoint> joints = new HashSet<>();
    protected final HashSet<PhysicsEntity<?>> entities = new HashSet<>();

    protected final ConcurrentLinkedQueue<Runnable> scheduledTasks = new ConcurrentLinkedQueue<>();
    protected final ConcurrentLinkedQueue<PhysicsWorldOperation<?>> operations = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean scheduledTasksLock = new AtomicBoolean();

    public BasePhysicsWorld(HmWorld world, boolean isRemoteWorld) {
        this.mcWorld = world;
        this.manager = new PhysicsWorldTerrain(this, world, isRemoteWorld);
    }

    /**
     * Initializes the bullet physics world <br>
     * Should be fired by the physics thread
     */
    protected void initPhysicsWorld() {
        Vector3f min = new Vector3f(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
        Vector3f max = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);

        PhysicsSpace.BroadphaseType bPhase = PhysicsSpace.BroadphaseType.DBVT;
        IPhysicsWorld physicsWorld = this;
        dynamicsWorld = new PhysicsSoftSpace(min, max, bPhase) {
            @Override
            public void onContactStarted(long manifoldId) {
                // memory leak fix : don't call super method : bullets stores all collision events in a queue
            }

            @Override
            public void onContactProcessed(PhysicsCollisionObject pcoA, PhysicsCollisionObject pcoB, long contactPointId) {
                // memory leak fix : don't call super method : bullets stores all collision events in a queue
                CollisionsHandler.handleCollision(physicsWorld, new PhysicsCollisionEvent(pcoA, pcoB, contactPointId), (BulletShapeType<?>) pcoA.getUserObject(), (BulletShapeType<?>) pcoB.getUserObject());
            }

            @Override
            public void onContactEnded(long manifoldId) {
            }
        };
        dynamicsWorld.setForceUpdateAllAabbs(false); // only tick the aabbs from the CollisionObjects when it is active
    }

    /**
     * Processes pending operations (adding/removing objects to the physics world)
     *
     * @param profiler The current profiler
     */
    protected void flushOperations(Profiler profiler) {
        profiler.start(Profiler.Profiles.ADD_REMOVE_BODIES);
        {
            try {
                while (!scheduledTasks.isEmpty()) {
                    scheduledTasks.remove().run();
                }
            } catch (Exception e) {
                scheduledTasksLock.set(true);
                DynamXMain.log.error("[SCHED_ERR] There is " + scheduledTasks.size() + " real scheduled tasks remaining");
                DynamXMain.log.error("[SCHED_ERR] Error executing task !", e);
                try {
                    DynamXMain.log.error("[SCHED_ERR] Scheduled tasks : " + scheduledTasks);
                } catch (Exception exception) {
                    DynamXMain.log.fatal("[SCHED_ERR] Additional error while printing current tasks", e);
                }
                throw e;
            }
            while (!operations.isEmpty()) {
                operations.remove().execute(this, dynamicsWorld, joints, entities);
            }
        }
        profiler.end(Profiler.Profiles.ADD_REMOVE_BODIES);
    }

    /**
     * Ticks the physics world
     *
     * @param profiler        The current profiler
     * @param syncThreadsLock Semaphore to acquire when getting the positions from the physics engine to the minecraft thread. Can be null if the physics are in the minecraft thread
     */
    protected void stepSimulationImpl(Profiler profiler, Semaphore syncThreadsLock) {
        JmeVector3fPool.openPool(SubClassPool.PHYSICS_WORLD_STEP);
        //Process pending operations
        flushOperations(profiler);
        //Check entity statuses and load terrain around them
        profiler.start(Profiler.Profiles.LOAD_SHAPES);
        entities.forEach(e -> {
            if (e.physicsHandler != null) {
                EntityPhysicsState physicalState = e.physicsHandler.getPhysicsState();
                switch (physicalState) {
                    case UNFREEZE:
                        e.physicsHandler.setFreezePhysics(false);
                    case ENABLE:
                        e.getTerrainCache().update(manager, profiler);

                        //FIXME TEST TO PREVENT DESPAWNING ON DEDICATED SERVER, BUT NOT IN SOLO BECAUSE THREADED
                        if (this instanceof BuiltinPhysicsWorld) {
                            e.physicsHandler.setPhysicsState(EntityPhysicsState.FREEZE);
                        }
                        break;
                    case FREEZE:
                        e.physicsHandler.setPhysicsState(EntityPhysicsState.WILL_FREEZE);
                        break;
                    case WILL_FREEZE:
                        e.physicsHandler.setFreezePhysics(true);
                        e.physicsHandler.setPhysicsState(EntityPhysicsState.FROZEN);
                        break;
                }
            }
        });
        profiler.end(Profiler.Profiles.LOAD_SHAPES);
        JmeVector3fPool.closePool();

        //Update collisions handler
        CollisionsHandler.tick();

        //Pre-tick each entity before the physics engine tick
        //Read the input data and send it to the physics
        profiler.start(Profiler.Profiles.PHYSICS_TICK_ENTITIES_PRE);
        entities.forEach(e -> {
            QuaternionPool.openPool(SubClassPool.TICK_ENTITY_PHY_PRE);
            JmeVector3fPool.openPool(SubClassPool.TICK_ENTITY_PHY_PRE);
            try {
                e.getSynchronizer().onPrePhysicsTick(profiler);
            } catch (Exception ex) {
                throw new PhysicsEntityException(e, "prePhysicsTick", ex);
            }
            QuaternionPool.closePool();
            JmeVector3fPool.closePool();
        });
        profiler.end(Profiler.Profiles.PHYSICS_TICK_ENTITIES_PRE);

        //Update sync system
        if (mcWorld.hm$isClient()) {
            ClientPhysicsSyncManager.tick();
        } else {
            ServerPhysicsSyncManager.tick(profiler);
        }

        //Tick the physics engine
        profiler.start(Profiler.Profiles.BULLET_STEP_SIM);
        DynamXContext.getPhysicsSimulationMode(MixinEnvironment.Side.SERVER).updatePhysicsWorld(dynamicsWorld);

        //Post-tick each entity after the physics engine tick
        //Retrieves the simulated data
        profiler.start(Profiler.Profiles.PHYSICS_TICK_ENTITIES_POST);
        if (syncThreadsLock != null) {
            try {
                syncThreadsLock.acquire();
            } catch (InterruptedException ignored) {
                syncThreadsLock = null; //fix: don't release if interrupted
            }
        }
        entities.forEach(e -> {
            QuaternionPool.openPool(SubClassPool.TICK_ENTITY_PHY_POST);
            JmeVector3fPool.openPool(SubClassPool.TICK_ENTITY_PHY_POST);
            try {
                e.getSynchronizer().onPostPhysicsTick(profiler);
            } catch (Exception ex) {
                throw new PhysicsEntityException(e, "postPhysicsTick", ex);
            }
            QuaternionPool.closePool();
            JmeVector3fPool.closePool();
        });
        if (syncThreadsLock != null) {
            syncThreadsLock.release();
        }
        profiler.end(Profiler.Profiles.PHYSICS_TICK_ENTITIES_POST);

        // TODO EVENT MinecraftForge.EVENT_BUS.post(new PhysicsEvent.StepSimulation(this, DynamXContext.getPhysicsSimulationMode(MixinEnvironment.Side.SERVER).getTimeStep()));
        profiler.end(Profiler.Profiles.BULLET_STEP_SIM);
    }

    @Override
    public void addOperation(PhysicsWorldOperation<?> operation) {
        operations.add(operation);
    }

    @Override
    public void schedule(Runnable r) {
        if (!scheduledTasksLock.get())
            scheduledTasks.add(r);
        else
            DynamXMain.log.error("Adding physics tacks is locked due to a previous execution exception");
    }

    @Override
    public PhysicsWorldTerrain getTerrainManager() {
        return manager;
    }

    @Override
    public HmWorld getWorld() {
        return mcWorld;
    }

    @Override
    public void tickStart() {
    }

    @Override
    public void tickEnd() {
    }

    @Override
    public int getLoadedEntityCount() {
        return entities.size();
    }

    @Override
    public PhysicsSoftSpace getDynamicsWorld() {
        return this.dynamicsWorld;
    }

    @Override
    public void clearAll() {
        for (PhysicsCollisionObject rb : this.dynamicsWorld.getRigidBodyList()) {
            this.dynamicsWorld.removeCollisionObject(rb);
        }
        for (PhysicsJoint jt : this.joints) {
            this.dynamicsWorld.removeJoint(jt);
        }
        entities.clear();
        joints.clear();
        getTerrainManager().onWorldUnload();
        DynamXContext.getPhysicsWorldPerDimensionMap().remove(mcWorld.hm$getDimension());
        if(mcWorld.hm$isClient()) {
            ClientDebugSystem.trackedRigidBodies.clear();
        }
    }
}